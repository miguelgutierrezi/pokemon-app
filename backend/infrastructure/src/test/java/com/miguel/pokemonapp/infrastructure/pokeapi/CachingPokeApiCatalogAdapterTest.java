package com.miguel.pokemonapp.infrastructure.pokeapi;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.benmanes.caffeine.cache.Ticker;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.miguel.pokemonapp.application.CatalogUnavailableException;
import com.miguel.pokemonapp.application.port.SpeciesPage;
import com.miguel.pokemonapp.domain.pokemon.CatalogEntry;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CachingPokeApiCatalogAdapterTest {

  private WireMockServer wireMock;
  private PokeApiCatalogAdapter adapter;
  private ManualTicker ticker;

  @BeforeEach
  void setUp() {
    wireMock = new WireMockServer(wireMockConfig().dynamicPort());
    wireMock.start();
    ticker = new ManualTicker();
    adapter =
        new PokeApiCatalogAdapter(
            wireMock.baseUrl(),
            Duration.ofMillis(200),
            Duration.ofMillis(500),
            Duration.ofHours(24),
            2048,
            256,
            ticker);
  }

  @AfterEach
  void tearDown() {
    if (adapter != null) {
      adapter.close();
    }
    if (wireMock != null) {
      wireMock.stop();
    }
  }

  @Test
  void identicalSpeciesPageCallsHitUpstreamOnce() {
    stubSpeciesPage();

    SpeciesPage first = adapter.speciesPage(0, 2);
    SpeciesPage second = adapter.speciesPage(0, 2);

    assertThat(first.nationalIds()).isEqualTo(second.nationalIds());
    wireMock.verify(
        exactly(1),
        getRequestedFor(urlPathEqualTo("/pokemon-species"))
            .withQueryParam("offset", equalTo("0"))
            .withQueryParam("limit", equalTo("2")));
  }

  @Test
  void identicalFetchEntriesCallsHitEachResourceOnce() {
    stubPokemon(1, pokemonBody(1, "bulbasaur"));
    stubSpecies(1, speciesBody("Bulbasaur"));
    stubPokemon(2, pokemonBody(2, "ivysaur"));
    stubSpecies(2, speciesBody("Ivysaur"));

    List<CatalogEntry> first = adapter.fetchEntries(List.of(1, 2));
    List<CatalogEntry> second = adapter.fetchEntries(List.of(1, 2));

    assertThat(first).hasSize(2);
    assertThat(second).hasSize(2);
    wireMock.verify(exactly(1), getRequestedFor(urlEqualTo("/pokemon/1")));
    wireMock.verify(exactly(1), getRequestedFor(urlEqualTo("/pokemon/2")));
    wireMock.verify(exactly(1), getRequestedFor(urlEqualTo("/pokemon-species/1")));
    wireMock.verify(exactly(1), getRequestedFor(urlEqualTo("/pokemon-species/2")));
  }

  @Test
  void failedCallIsNotCachedAndRetryHitsUpstream() {
    wireMock.stubFor(
        get(urlEqualTo("/pokemon/1")).willReturn(aResponse().withStatus(500).withBody("error")));
    stubSpecies(1, speciesBody("Bulbasaur"));

    assertThatThrownBy(() -> adapter.fetchEntries(List.of(1)))
        .isInstanceOf(CatalogUnavailableException.class);

    stubPokemon(1, pokemonBody(1, "bulbasaur"));

    List<CatalogEntry> recovered = adapter.fetchEntries(List.of(1));
    assertThat(recovered).hasSize(1);
    wireMock.verify(exactly(2), getRequestedFor(urlEqualTo("/pokemon/1")));
  }

  @Test
  void expiredCacheEntryIsRefetched() {
    stubSpeciesPage();

    adapter.speciesPage(0, 2);
    ticker.advance(Duration.ofHours(25));
    adapter.speciesPage(0, 2);

    wireMock.verify(
        exactly(2),
        getRequestedFor(urlPathEqualTo("/pokemon-species"))
            .withQueryParam("offset", equalTo("0"))
            .withQueryParam("limit", equalTo("2")));
  }

  private void stubSpeciesPage() {
    wireMock.stubFor(
        get(urlPathEqualTo("/pokemon-species"))
            .withQueryParam("offset", equalTo("0"))
            .withQueryParam("limit", equalTo("2"))
            .willReturn(
                aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withBody(
                        """
                        {
                          "count": 1025,
                          "results": [
                            {
                              "name": "bulbasaur",
                              "url": "https://pokeapi.co/api/v2/pokemon-species/1/"
                            },
                            {
                              "name": "ivysaur",
                              "url": "https://pokeapi.co/api/v2/pokemon-species/2/"
                            }
                          ]
                        }
                        """)));
  }

  private void stubPokemon(int id, String body) {
    wireMock.stubFor(
        get(urlEqualTo("/pokemon/" + id))
            .willReturn(aResponse().withHeader("Content-Type", "application/json").withBody(body)));
  }

  private void stubSpecies(int id, String body) {
    wireMock.stubFor(
        get(urlEqualTo("/pokemon-species/" + id))
            .willReturn(aResponse().withHeader("Content-Type", "application/json").withBody(body)));
  }

  private static String pokemonBody(int id, String name) {
    return """
        {
          "id": %d,
          "name": "%s",
          "weight": 69,
          "sprites": { "front_default": "https://example.com/%d.png" },
          "abilities": [
            { "ability": { "name": "overgrow" }, "is_hidden": false, "slot": 1 }
          ]
        }
        """
        .formatted(id, name, id);
  }

  private static String speciesBody(String englishName) {
    return """
        {
          "names": [ { "name": "%s", "language": { "name": "en" } } ],
          "genera": [ { "genus": "Seed Pokémon", "language": { "name": "en" } } ],
          "generation": { "name": "generation-i" },
          "evolution_chain": { "url": "https://pokeapi.co/api/v2/evolution-chain/1/" }
        }
        """
        .formatted(englishName);
  }

  private static final class ManualTicker implements Ticker {
    private final AtomicLong nanos = new AtomicLong();

    @Override
    public long read() {
      return nanos.get();
    }

    void advance(Duration duration) {
      nanos.addAndGet(duration.toNanos());
    }
  }
}
