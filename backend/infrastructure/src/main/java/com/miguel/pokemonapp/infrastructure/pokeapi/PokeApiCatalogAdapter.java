package com.miguel.pokemonapp.infrastructure.pokeapi;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Ticker;
import com.miguel.pokemonapp.application.CatalogUnavailableException;
import com.miguel.pokemonapp.application.port.PokemonCatalogPort;
import com.miguel.pokemonapp.application.port.SpeciesPage;
import com.miguel.pokemonapp.domain.pokemon.CatalogAbility;
import com.miguel.pokemonapp.domain.pokemon.CatalogEntry;
import com.miguel.pokemonapp.domain.pokemon.CatalogPokemon;
import com.miguel.pokemonapp.domain.pokemon.CatalogSpecies;
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
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.JsonNode;

public final class PokeApiCatalogAdapter implements PokemonCatalogPort, AutoCloseable {

  private static final Pattern SPECIES_ID = Pattern.compile(".*/pokemon-species/(\\d+)/?$");
  private static final Duration DEFAULT_CACHE_TTL = Duration.ofHours(24);
  private static final int DEFAULT_ID_CACHE_MAX_SIZE = 2048;
  private static final int DEFAULT_LIST_CACHE_MAX_SIZE = 256;

  private final RestClient restClient;
  private final ExecutorService executor;
  private final Cache<String, SpeciesPage> speciesPageCache;
  private final Cache<Integer, CatalogPokemon> pokemonCache;
  private final Cache<Integer, CatalogSpecies> speciesCache;

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
  }

  @Override
  public SpeciesPage speciesPage(int offset, int limit) {
    String key = offset + ":" + limit;
    return speciesPageCache.get(key, ignored -> loadSpeciesPage(offset, limit));
  }

  @Override
  public List<CatalogEntry> fetchEntries(List<Integer> nationalIds) {
    if (nationalIds == null) {
      throw new IllegalArgumentException("nationalIds must not be null");
    }
    List<CompletableFuture<CatalogEntry>> futures =
        nationalIds.stream()
            .map(id -> CompletableFuture.supplyAsync(() -> fetchEntry(id), executor))
            .toList();
    try {
      List<CatalogEntry> entries = new ArrayList<>(futures.size());
      for (CompletableFuture<CatalogEntry> future : futures) {
        entries.add(future.join());
      }
      return List.copyOf(entries);
    } catch (CompletionException e) {
      futures.forEach(future -> future.cancel(true));
      throw toCatalogUnavailable(e, "catalog entries unavailable");
    }
  }

  private CatalogEntry fetchEntry(int nationalId) {
    CompletableFuture<CatalogPokemon> pokemon =
        CompletableFuture.supplyAsync(() -> cachedPokemon(nationalId), executor);
    CompletableFuture<CatalogSpecies> species =
        CompletableFuture.supplyAsync(() -> cachedSpecies(nationalId), executor);
    try {
      return new CatalogEntry(pokemon.join(), species.join());
    } catch (CompletionException e) {
      pokemon.cancel(true);
      species.cancel(true);
      throw toCatalogUnavailable(e, "catalog entry unavailable for id " + nationalId);
    }
  }

  private CatalogPokemon cachedPokemon(int nationalId) {
    return pokemonCache.get(nationalId, this::loadPokemon);
  }

  private CatalogSpecies cachedSpecies(int nationalId) {
    return speciesCache.get(nationalId, this::loadSpecies);
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

  private CatalogPokemon loadPokemon(int nationalId) {
    JsonNode body = getJson("/pokemon/{id}", nationalId);
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
    return new CatalogPokemon(
        id,
        name,
        textOrNull(body.path("sprites").path("front_default")),
        body.get("weight").asInt(),
        abilities);
  }

  private CatalogSpecies loadSpecies(int nationalId) {
    JsonNode body = getJson("/pokemon-species/{id}", nationalId);
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
    return new CatalogSpecies(names, genera);
  }

  private JsonNode getJson(String uriTemplate, Object... uriVariables) {
    try {
      JsonNode body =
          restClient
              .get()
              .uri(uriTemplate, uriVariables)
              .retrieve()
              .onStatus(
                  status -> status.isError(),
                  (request, response) -> {
                    throw new CatalogUnavailableException(
                        "catalog returned " + response.getStatusCode() + " for " + uriTemplate);
                  })
              .body(JsonNode.class);
      if (body == null) {
        throw new CatalogUnavailableException("empty catalog response for " + uriTemplate);
      }
      return body;
    } catch (CatalogUnavailableException e) {
      throw e;
    } catch (RestClientException e) {
      throw new CatalogUnavailableException("catalog unavailable for " + uriTemplate, e);
    }
  }

  private static void requireField(JsonNode body, String field, String message) {
    JsonNode node = body.get(field);
    if (node == null || node.isNull() || node.isMissingNode()) {
      throw new CatalogUnavailableException(message);
    }
  }

  private static CatalogUnavailableException toCatalogUnavailable(
      CompletionException e, String message) {
    Throwable cause = e.getCause() != null ? e.getCause() : e;
    if (cause instanceof CatalogUnavailableException unavailable) {
      return unavailable;
    }
    return new CatalogUnavailableException(message, cause);
  }

  private static int parseSpeciesId(String url) {
    Matcher matcher = SPECIES_ID.matcher(url);
    if (!matcher.matches()) {
      throw new CatalogUnavailableException("unrecognized species url: " + url);
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
