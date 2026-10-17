package com.miguel.pokemonapp.infrastructure.pokeapi;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.WireMockServer;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Runs in a Surefire fork with {@code -Djdk.virtualThreadScheduler.parallelism=2}. Sync Caffeine
 * {@code Cache} + blocking loads pin those carriers and serialize (~N/2 × delay); AsyncCache keeps
 * wall time near one delay.
 */
class VirtualThreadCarrierPinningIT {

  private static final int ENTRY_COUNT = 16;
  private static final int DELAY_MS = 200;

  private WireMockServer wireMock;
  private PokeApiCatalogAdapter adapter;

  @BeforeEach
  void setUp() {
    wireMock = new WireMockServer(wireMockConfig().dynamicPort());
    wireMock.start();
    adapter =
        new PokeApiCatalogAdapter(
            wireMock.baseUrl(), Duration.ofMillis(200), Duration.ofMillis(2_000));
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
  void manyParallelCacheMissesFinishNearOneDelayWithTwoCarriers() {
    List<Integer> ids = IntStream.rangeClosed(1, ENTRY_COUNT).boxed().toList();
    for (int id : ids) {
      stubPokemon(id);
      stubSpecies(id);
    }

    Instant start = Instant.now();
    assertThat(adapter.fetchEntries(ids)).hasSize(ENTRY_COUNT);
    long elapsedMs = Duration.between(start, Instant.now()).toMillis();

    // Pinned sync Cache with 2 carriers: 32 HTTP calls → ≥ 16 × DELAY_MS.
    // AsyncCache: all in flight → a little over one DELAY_MS.
    assertThat(elapsedMs)
        .as(
            "elapsed=%sms; sync Cache would be ~%sms with 2 carriers",
            elapsedMs, ENTRY_COUNT * DELAY_MS)
        .isLessThan(ENTRY_COUNT * DELAY_MS / 2L);
  }

  private void stubPokemon(int id) {
    wireMock.stubFor(
        get(urlEqualTo("/pokemon/" + id))
            .willReturn(
                aResponse()
                    .withFixedDelay(DELAY_MS)
                    .withHeader("Content-Type", "application/json")
                    .withBody(
                        """
                        {
                          "id": %d,
                          "name": "p%d",
                          "weight": 69,
                          "sprites": { "front_default": null },
                          "abilities": []
                        }
                        """
                            .formatted(id, id))));
  }

  private void stubSpecies(int id) {
    wireMock.stubFor(
        get(urlEqualTo("/pokemon-species/" + id))
            .willReturn(
                aResponse()
                    .withFixedDelay(DELAY_MS)
                    .withHeader("Content-Type", "application/json")
                    .withBody(
                        """
                        {
                          "names": [ { "name": "P%d", "language": { "name": "en" } } ],
                          "genera": [],
                          "generation": { "name": "generation-i" },
                          "evolution_chain": {
                            "url": "https://pokeapi.co/api/v2/evolution-chain/1/"
                          }
                        }
                        """
                            .formatted(id))));
  }
}
