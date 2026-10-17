package com.miguel.pokemonapp.infrastructure.pokeapi;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Ticker;
import com.miguel.pokemonapp.application.CatalogUnavailableException;
import com.miguel.pokemonapp.application.PokemonNotFoundException;
import com.miguel.pokemonapp.application.port.PokemonCatalogPort;
import com.miguel.pokemonapp.application.port.SpeciesPage;
import com.miguel.pokemonapp.domain.pokemon.CatalogAbility;
import com.miguel.pokemonapp.domain.pokemon.CatalogDetailEntry;
import com.miguel.pokemonapp.domain.pokemon.CatalogEntry;
import com.miguel.pokemonapp.domain.pokemon.CatalogEvolutionChain;
import com.miguel.pokemonapp.domain.pokemon.CatalogEvolutionDetail;
import com.miguel.pokemonapp.domain.pokemon.CatalogEvolutionNode;
import com.miguel.pokemonapp.domain.pokemon.CatalogPokemonDetail;
import com.miguel.pokemonapp.domain.pokemon.CatalogSpeciesDetail;
import com.miguel.pokemonapp.domain.pokemon.CatalogStat;
import com.miguel.pokemonapp.domain.pokemon.CatalogType;
import com.miguel.pokemonapp.domain.pokemon.FlavorTextEntry;
import com.miguel.pokemonapp.domain.pokemon.LocalizedText;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.JsonNode;

public final class PokeApiCatalogAdapter implements PokemonCatalogPort, AutoCloseable {

  private static final Pattern SPECIES_ID = Pattern.compile(".*/pokemon-species/(\\d+)/?$");
  private static final Pattern EVOLUTION_CHAIN_ID = Pattern.compile(".*/evolution-chain/(\\d+)/?$");
  private static final Duration DEFAULT_CACHE_TTL = Duration.ofHours(24);
  private static final int DEFAULT_ID_CACHE_MAX_SIZE = 2048;
  private static final int DEFAULT_LIST_CACHE_MAX_SIZE = 256;

  private final RestClient restClient;
  private final ExecutorService executor;
  private final Cache<String, SpeciesPage> speciesPageCache;
  private final Cache<Integer, CatalogPokemonDetail> pokemonCache;
  private final Cache<Integer, CatalogSpeciesDetail> speciesCache;
  private final Cache<Integer, CatalogEvolutionChain> evolutionChainCache;

  public PokeApiCatalogAdapter(String baseUrl, Duration connectTimeout, Duration readTimeout) {
    this(
        baseUrl,
        connectTimeout,
        readTimeout,
        DEFAULT_CACHE_TTL,
        DEFAULT_ID_CACHE_MAX_SIZE,
        DEFAULT_LIST_CACHE_MAX_SIZE,
        Ticker.systemTicker());
  }

  public PokeApiCatalogAdapter(
      String baseUrl,
      Duration connectTimeout,
      Duration readTimeout,
      Duration cacheTtl,
      int idCacheMaxSize,
      int listCacheMaxSize,
      Ticker ticker) {
    HttpClient httpClient = HttpClient.newBuilder().connectTimeout(connectTimeout).build();
    JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
    requestFactory.setReadTimeout(readTimeout);
    this.restClient = RestClient.builder().baseUrl(baseUrl).requestFactory(requestFactory).build();
    this.executor = Executors.newVirtualThreadPerTaskExecutor();
    this.speciesPageCache =
        Caffeine.newBuilder()
            .expireAfterWrite(cacheTtl)
            .maximumSize(listCacheMaxSize)
            .ticker(ticker)
            .build();
    this.pokemonCache =
        Caffeine.newBuilder()
            .expireAfterWrite(cacheTtl)
            .maximumSize(idCacheMaxSize)
            .ticker(ticker)
            .build();
    this.speciesCache =
        Caffeine.newBuilder()
            .expireAfterWrite(cacheTtl)
            .maximumSize(idCacheMaxSize)
            .ticker(ticker)
            .build();
    this.evolutionChainCache =
        Caffeine.newBuilder()
            .expireAfterWrite(cacheTtl)
            .maximumSize(idCacheMaxSize)
            .ticker(ticker)
            .build();
  }

  @Override
  public SpeciesPage speciesPage(int offset, int limit) {
    String key = offset + ":" + limit;
    SpeciesPage cached = speciesPageCache.getIfPresent(key);
    if (cached != null) {
      return cached;
    }
    SpeciesPage loaded = loadSpeciesPage(offset, limit);
    speciesPageCache.put(key, loaded);
    return loaded;
  }

  @Override
  public List<CatalogEntry> fetchEntries(List<Integer> nationalIds) {
    if (nationalIds == null) {
      throw new IllegalArgumentException("nationalIds must not be null");
    }
    List<CompletableFuture<CatalogEntry>> futures =
        nationalIds.stream().map(this::fetchEntry).toList();
    try {
      List<CatalogEntry> entries = new ArrayList<>(futures.size());
      for (CompletableFuture<CatalogEntry> future : futures) {
        entries.add(future.join());
      }
      return List.copyOf(entries);
    } catch (CompletionException e) {
      futures.forEach(future -> future.cancel(true));
      throw toListPortException(e, "catalog entries unavailable");
    }
  }

  @Override
  public CatalogDetailEntry fetchDetail(int nationalId) {
    CompletableFuture<CatalogPokemonDetail> pokemon =
        CompletableFuture.supplyAsync(() -> cachedPokemon(nationalId), executor);
    CompletableFuture<CatalogSpeciesDetail> species =
        CompletableFuture.supplyAsync(() -> cachedSpecies(nationalId), executor);
    try {
      return pokemon.thenCombine(species, CatalogDetailEntry::new).join();
    } catch (CompletionException e) {
      pokemon.cancel(true);
      species.cancel(true);
      throw toPortException(e, "catalog detail unavailable");
    }
  }

  @Override
  public CatalogEvolutionChain fetchEvolutionChain(int chainId) {
    CatalogEvolutionChain cached = evolutionChainCache.getIfPresent(chainId);
    if (cached != null) {
      return cached;
    }
    CatalogEvolutionChain loaded = loadEvolutionChain(chainId);
    evolutionChainCache.put(chainId, loaded);
    return loaded;
  }

  private CompletableFuture<CatalogEntry> fetchEntry(int nationalId) {
    CompletableFuture<CatalogPokemonDetail> pokemon =
        CompletableFuture.supplyAsync(() -> cachedPokemon(nationalId), executor);
    CompletableFuture<CatalogSpeciesDetail> species =
        CompletableFuture.supplyAsync(() -> cachedSpecies(nationalId), executor);
    return pokemon.thenCombine(species, (p, s) -> new CatalogEntry(p.toSummary(), s.toSummary()));
  }

  private CatalogPokemonDetail cachedPokemon(int nationalId) {
    CatalogPokemonDetail cached = pokemonCache.getIfPresent(nationalId);
    if (cached != null) {
      return cached;
    }
    CatalogPokemonDetail loaded = loadPokemon(nationalId);
    pokemonCache.put(nationalId, loaded);
    return loaded;
  }

  private CatalogSpeciesDetail cachedSpecies(int nationalId) {
    CatalogSpeciesDetail cached = speciesCache.getIfPresent(nationalId);
    if (cached != null) {
      return cached;
    }
    CatalogSpeciesDetail loaded = loadSpecies(nationalId);
    speciesCache.put(nationalId, loaded);
    return loaded;
  }

  private SpeciesPage loadSpeciesPage(int offset, int limit) {
    JsonNode body = getJson("/pokemon-species?offset={offset}&limit={limit}", offset, limit);
    requireField(body, "count", "malformed species page: missing count");
    long total = body.get("count").asLong();
    List<Integer> ids = new ArrayList<>();
    for (JsonNode result : body.path("results")) {
      ids.add(parseSpeciesId(result.path("url").asString()));
    }
    return new SpeciesPage(ids, total);
  }

  private CatalogPokemonDetail loadPokemon(int nationalId) {
    JsonNode body = getIdentityJson(nationalId, "/pokemon/{id}", nationalId);
    requireField(body, "id", "malformed pokemon payload for id " + nationalId);
    requireField(body, "name", "malformed pokemon payload for id " + nationalId);
    requireField(body, "weight", "malformed pokemon payload for id " + nationalId);

    int id = body.get("id").asInt();
    if (id != nationalId) {
      throw new CatalogUnavailableException(
          "malformed pokemon id: expected " + nationalId + " but was " + id);
    }
    String name = body.get("name").asString();
    if (name.isBlank()) {
      throw new CatalogUnavailableException("malformed pokemon payload for id " + nationalId);
    }

    List<CatalogAbility> abilities = new ArrayList<>();
    for (JsonNode abilityNode : body.path("abilities")) {
      abilities.add(
          new CatalogAbility(
              abilityNode.path("slot").asInt(),
              abilityNode.path("ability").path("name").asString(),
              abilityNode.path("is_hidden").asBoolean()));
    }

    List<CatalogType> types = new ArrayList<>();
    for (JsonNode typeNode : body.path("types")) {
      types.add(
          new CatalogType(
              typeNode.path("slot").asInt(), typeNode.path("type").path("name").asString()));
    }

    List<CatalogStat> stats = new ArrayList<>();
    for (JsonNode statNode : body.path("stats")) {
      stats.add(
          new CatalogStat(
              statNode.path("stat").path("name").asString(), statNode.path("base_stat").asInt()));
    }

    int heightDecimetres = body.path("height").asInt(0);

    try {
      return new CatalogPokemonDetail(
          id,
          name,
          textOrNull(body.path("sprites").path("front_default")),
          textOrNull(
              body.path("sprites").path("other").path("official-artwork").path("front_default")),
          heightDecimetres,
          body.get("weight").asInt(),
          abilities,
          types,
          stats);
    } catch (IllegalArgumentException e) {
      throw new CatalogUnavailableException("malformed pokemon payload for id " + nationalId, e);
    }
  }

  private CatalogEvolutionChain loadEvolutionChain(int chainId) {
    JsonNode body = getJson("/evolution-chain/{id}", chainId);
    requireField(body, "chain", "malformed evolution-chain payload for id " + chainId);
    try {
      return new CatalogEvolutionChain(parseEvolutionNode(body.get("chain")));
    } catch (IllegalArgumentException e) {
      throw new CatalogUnavailableException(
          "malformed evolution-chain payload for id " + chainId, e);
    }
  }

  private CatalogEvolutionNode parseEvolutionNode(JsonNode node) {
    String speciesUrl = textOrNull(node.path("species").path("url"));
    if (speciesUrl == null) {
      throw new CatalogUnavailableException("malformed evolution node: missing species url");
    }
    int speciesId = parseSpeciesId(speciesUrl);
    List<CatalogEvolutionDetail> details = new ArrayList<>();
    for (JsonNode detailNode : node.path("evolution_details")) {
      details.add(
          new CatalogEvolutionDetail(
              textOrNull(detailNode.path("trigger").path("name")),
              integerOrNull(detailNode.path("min_level")),
              textOrNull(detailNode.path("item").path("name"))));
    }
    List<CatalogEvolutionNode> children = new ArrayList<>();
    for (JsonNode child : node.path("evolves_to")) {
      children.add(parseEvolutionNode(child));
    }
    return new CatalogEvolutionNode(speciesId, details, children);
  }

  private CatalogSpeciesDetail loadSpecies(int nationalId) {
    JsonNode body = getIdentityJson(nationalId, "/pokemon-species/{id}", nationalId);
    List<LocalizedText> names = new ArrayList<>();
    for (JsonNode nameNode : body.path("names")) {
      names.add(
          new LocalizedText(
              nameNode.path("language").path("name").asString(), nameNode.path("name").asString()));
    }
    List<LocalizedText> genera = new ArrayList<>();
    for (JsonNode genusNode : body.path("genera")) {
      genera.add(
          new LocalizedText(
              genusNode.path("language").path("name").asString(),
              genusNode.path("genus").asString()));
    }
    List<FlavorTextEntry> flavorEntries = new ArrayList<>();
    for (JsonNode flavorNode : body.path("flavor_text_entries")) {
      flavorEntries.add(
          new FlavorTextEntry(
              flavorNode.path("language").path("name").asString(),
              flavorNode.path("flavor_text").asString(),
              flavorNode.path("version").path("name").asString()));
    }

    String generationSlug = textOrNull(body.path("generation").path("name"));
    if (generationSlug == null) {
      throw new CatalogUnavailableException(
          "malformed species payload for id " + nationalId + ": missing generation");
    }
    String chainUrl = textOrNull(body.path("evolution_chain").path("url"));
    if (chainUrl == null) {
      throw new CatalogUnavailableException(
          "malformed species payload for id " + nationalId + ": missing evolution_chain");
    }
    int evolutionChainId = parseEvolutionChainId(chainUrl);

    try {
      return new CatalogSpeciesDetail(
          names, genera, flavorEntries, generationSlug, evolutionChainId);
    } catch (IllegalArgumentException e) {
      throw new CatalogUnavailableException("malformed species payload for id " + nationalId, e);
    }
  }

  private JsonNode getIdentityJson(int nationalId, String uriTemplate, Object... uriVariables) {
    return exchangeWithRetry(uriTemplate, nationalId, uriVariables);
  }

  private JsonNode getJson(String uriTemplate, Object... uriVariables) {
    return exchangeWithRetry(uriTemplate, null, uriVariables);
  }

  private JsonNode exchangeWithRetry(
      String uriTemplate, Integer notFoundNationalId, Object... uriVariables) {
    try {
      return exchangeJson(uriTemplate, notFoundNationalId, uriVariables);
    } catch (ResourceAccessException first) {
      if (isTimeout(first)) {
        throw new CatalogUnavailableException("catalog unavailable for " + uriTemplate, first);
      }
      try {
        return exchangeJson(uriTemplate, notFoundNationalId, uriVariables);
      } catch (CatalogUnavailableException | PokemonNotFoundException e) {
        throw e;
      } catch (RestClientException retry) {
        throw new CatalogUnavailableException("catalog unavailable for " + uriTemplate, retry);
      }
    } catch (CatalogUnavailableException | PokemonNotFoundException e) {
      throw e;
    } catch (RestClientException e) {
      throw new CatalogUnavailableException("catalog unavailable for " + uriTemplate, e);
    }
  }

  private static boolean isTimeout(ResourceAccessException exception) {
    Throwable cause = exception;
    while (cause != null) {
      if (cause instanceof java.net.http.HttpTimeoutException) {
        return true;
      }
      cause = cause.getCause();
    }
    return false;
  }

  private JsonNode exchangeJson(
      String uriTemplate, Integer notFoundNationalId, Object... uriVariables) {
    JsonNode body =
        restClient
            .get()
            .uri(uriTemplate, uriVariables)
            .retrieve()
            .onStatus(
                status -> status.isError(),
                (request, response) -> {
                  if (notFoundNationalId != null && response.getStatusCode().value() == 404) {
                    throw new PokemonNotFoundException(
                        notFoundNationalId, "catalog returned 404 for " + uriTemplate);
                  }
                  throw new CatalogUnavailableException(
                      "catalog returned " + response.getStatusCode() + " for " + uriTemplate);
                })
            .body(JsonNode.class);
    if (body == null) {
      throw new CatalogUnavailableException("empty catalog response for " + uriTemplate);
    }
    return body;
  }

  private static void requireField(JsonNode body, String field, String message) {
    JsonNode node = body.get(field);
    if (node == null || node.isNull() || node.isMissingNode()) {
      throw new CatalogUnavailableException(message);
    }
  }

  private static RuntimeException toPortException(CompletionException e, String message) {
    Throwable cause = e.getCause() != null ? e.getCause() : e;
    if (cause instanceof CatalogUnavailableException unavailable) {
      return unavailable;
    }
    if (cause instanceof PokemonNotFoundException notFound) {
      return notFound;
    }
    return new CatalogUnavailableException(message, cause);
  }

  private static RuntimeException toListPortException(CompletionException e, String message) {
    Throwable cause = e.getCause() != null ? e.getCause() : e;
    if (cause instanceof CatalogUnavailableException unavailable) {
      return unavailable;
    }
    if (cause instanceof PokemonNotFoundException notFound) {
      return new CatalogUnavailableException(message, notFound);
    }
    return new CatalogUnavailableException(message, cause);
  }

  private static Integer integerOrNull(JsonNode node) {
    if (node == null || node.isNull() || node.isMissingNode()) {
      return null;
    }
    return node.asInt();
  }

  private static int parseSpeciesId(String url) {
    Matcher matcher = SPECIES_ID.matcher(url);
    if (!matcher.matches()) {
      throw new CatalogUnavailableException("unrecognized species url: " + url);
    }
    return Integer.parseInt(matcher.group(1));
  }

  private static int parseEvolutionChainId(String url) {
    Matcher matcher = EVOLUTION_CHAIN_ID.matcher(url);
    if (!matcher.matches()) {
      throw new CatalogUnavailableException("unrecognized evolution-chain url: " + url);
    }
    return Integer.parseInt(matcher.group(1));
  }

  private static String textOrNull(JsonNode node) {
    if (node == null || node.isNull() || node.isMissingNode()) {
      return null;
    }
    String text = node.asString();
    return text.isBlank() ? null : text;
  }

  @Override
  public void close() {
    executor.close();
  }
}
