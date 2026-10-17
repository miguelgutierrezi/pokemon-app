package com.miguel.pokemonapp.infrastructure.pokeapi;

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
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.JsonNode;

public final class PokeApiCatalogAdapter implements PokemonCatalogPort, AutoCloseable {

  private static final Pattern SPECIES_ID = Pattern.compile(".*/pokemon-species/(\\d+)/?$");

  private final RestClient restClient;
  private final ExecutorService executor;

  public PokeApiCatalogAdapter(String baseUrl, Duration connectTimeout, Duration readTimeout) {
    HttpClient httpClient = HttpClient.newBuilder().connectTimeout(connectTimeout).build();
    JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
    requestFactory.setReadTimeout(readTimeout);
    this.restClient =
        RestClient.builder()
            .baseUrl(baseUrl)
            .requestFactory(requestFactory)
            .messageConverters(List.of(new JacksonJsonHttpMessageConverter()))
            .build();
    this.executor = Executors.newVirtualThreadPerTaskExecutor();
  }

  @Override
  public SpeciesPage speciesPage(int offset, int limit) {
    try {
      JsonNode body =
          restClient
              .get()
              .uri(
                  uriBuilder ->
                      uriBuilder
                          .path("/pokemon-species")
                          .queryParam("offset", offset)
                          .queryParam("limit", limit)
                          .build())
              .retrieve()
              .onStatus(
                  status -> status.isError(),
                  (request, response) -> {
                    throw new CatalogUnavailableException(
                        "species page returned " + response.getStatusCode());
                  })
              .body(JsonNode.class);
      if (body == null) {
        throw new CatalogUnavailableException("empty species page response");
      }
      long total = body.path("count").asLong();
      List<Integer> ids = new ArrayList<>();
      for (JsonNode result : body.path("results")) {
        ids.add(parseSpeciesId(result.path("url").asText()));
      }
      return new SpeciesPage(ids, total);
    } catch (CatalogUnavailableException e) {
      throw e;
    } catch (RestClientException e) {
      throw new CatalogUnavailableException("species page unavailable", e);
    }
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
      return futures.stream().map(CompletableFuture::join).toList();
    } catch (CompletionException e) {
      throw toCatalogUnavailable(e, "catalog entries unavailable");
    }
  }

  private CatalogEntry fetchEntry(int nationalId) {
    CompletableFuture<CatalogPokemon> pokemon =
        CompletableFuture.supplyAsync(() -> fetchPokemon(nationalId), executor);
    CompletableFuture<CatalogSpecies> species =
        CompletableFuture.supplyAsync(() -> fetchSpecies(nationalId), executor);
    try {
      return new CatalogEntry(pokemon.join(), species.join());
    } catch (CompletionException e) {
      throw toCatalogUnavailable(e, "catalog entry unavailable for id " + nationalId);
    }
  }

  private CatalogPokemon fetchPokemon(int nationalId) {
    try {
      JsonNode body =
          restClient
              .get()
              .uri("/pokemon/{id}", nationalId)
              .retrieve()
              .onStatus(
                  status -> status.isError(),
                  (request, response) -> {
                    throw new CatalogUnavailableException(
                        "pokemon " + nationalId + " returned " + response.getStatusCode());
                  })
              .body(JsonNode.class);
      if (body == null) {
        throw new CatalogUnavailableException("empty pokemon response for id " + nationalId);
      }
      List<CatalogAbility> abilities = new ArrayList<>();
      for (JsonNode abilityNode : body.path("abilities")) {
        abilities.add(
            new CatalogAbility(
                abilityNode.path("slot").asInt(),
                abilityNode.path("ability").path("name").asText(),
                abilityNode.path("is_hidden").asBoolean()));
      }
      return new CatalogPokemon(
          body.path("id").asInt(),
          body.path("name").asText(),
          textOrNull(body.path("sprites").path("front_default")),
          body.path("weight").asInt(),
          abilities);
    } catch (CatalogUnavailableException e) {
      throw e;
    } catch (RestClientException e) {
      throw new CatalogUnavailableException("pokemon " + nationalId + " unavailable", e);
    }
  }

  private CatalogSpecies fetchSpecies(int nationalId) {
    try {
      JsonNode body =
          restClient
              .get()
              .uri("/pokemon-species/{id}", nationalId)
              .retrieve()
              .onStatus(
                  status -> status.isError(),
                  (request, response) -> {
                    throw new CatalogUnavailableException(
                        "species " + nationalId + " returned " + response.getStatusCode());
                  })
              .body(JsonNode.class);
      if (body == null) {
        throw new CatalogUnavailableException("empty species response for id " + nationalId);
      }
      List<LocalizedText> names = new ArrayList<>();
      for (JsonNode nameNode : body.path("names")) {
        names.add(
            new LocalizedText(
                nameNode.path("language").path("name").asText(), nameNode.path("name").asText()));
      }
      List<LocalizedText> genera = new ArrayList<>();
      for (JsonNode genusNode : body.path("genera")) {
        genera.add(
            new LocalizedText(
                genusNode.path("language").path("name").asText(),
                genusNode.path("genus").asText()));
      }
      return new CatalogSpecies(names, genera);
    } catch (CatalogUnavailableException e) {
      throw e;
    } catch (RestClientException e) {
      throw new CatalogUnavailableException("species " + nationalId + " unavailable", e);
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
    String text = node.asText();
    return text.isBlank() ? null : text;
  }

  @Override
  public void close() {
    executor.close();
  }
}
