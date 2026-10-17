package com.miguel.pokemonapp.infrastructure.pokeapi;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.miguel.pokemonapp.application.CatalogUnavailableException;
import com.miguel.pokemonapp.application.port.SpeciesPage;
import com.miguel.pokemonapp.domain.pokemon.CatalogAbility;
import com.miguel.pokemonapp.domain.pokemon.CatalogEntry;
import com.miguel.pokemonapp.domain.pokemon.CatalogPokemon;
import com.miguel.pokemonapp.domain.pokemon.CatalogSpecies;
import com.miguel.pokemonapp.domain.pokemon.LocalizedText;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PokeApiCatalogAdapterTest {

  private WireMockServer wireMock;
  private PokeApiCatalogAdapter adapter;

  @BeforeEach
  void setUp() {
    wireMock = new WireMockServer(wireMockConfig().dynamicPort());
    wireMock.start();
    adapter =
        new PokeApiCatalogAdapter(
            wireMock.baseUrl(), Duration.ofMillis(200), Duration.ofMillis(500));
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
  void speciesPageParsesIdsFromResultUrlsAndCount() {
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

    SpeciesPage page = adapter.speciesPage(0, 2);

    assertThat(page.nationalIds()).containsExactly(1, 2);
    assertThat(page.totalCount()).isEqualTo(1025L);
  }

  @Test
  void fetchEntriesMapsFieldsPreservesOrderAndRunsInParallel() {
    stubPokemon(
        1,
        """
        {
          "id": 1,
          "name": "bulbasaur",
          "weight": 69,
          "sprites": { "front_default": "https://example.com/1.png" },
          "abilities": [
            {
              "ability": { "name": "chlorophyll" },
              "is_hidden": true,
              "slot": 3
            },
            {
              "ability": { "name": "overgrow" },
              "is_hidden": false,
              "slot": 1
            }
          ]
        }
        """,
        300);
    stubSpecies(
        1,
        """
        {
          "names": [
            { "name": "フシギダネ", "language": { "name": "ja" } },
            { "name": "Bulbasaur", "language": { "name": "en" } }
          ],
          "genera": [
            { "genus": "Seed Pokémon", "language": { "name": "en" } },
            { "genus": "たねポケモン", "language": { "name": "ja" } }
          ]
        }
        """,
        300);
    stubPokemon(
        2,
        """
        {
          "id": 2,
          "name": "ivysaur",
          "weight": 130,
          "sprites": { "front_default": null },
          "abilities": [
            { "ability": { "name": "overgrow" }, "is_hidden": false, "slot": 1 }
          ]
        }
        """,
        300);
    stubSpecies(
        2,
        """
        {
          "names": [ { "name": "Ivysaur", "language": { "name": "en" } } ],
          "genera": [ { "genus": "Seed Pokémon", "language": { "name": "en" } } ]
        }
        """,
        300);
    stubPokemon(
        3,
        """
        {
          "id": 3,
          "name": "venusaur",
          "weight": 1000,
          "sprites": {},
          "abilities": []
        }
        """,
        300);
    stubSpecies(
        3,
        """
        {
          "names": [ { "name": "Venusaur", "language": { "name": "en" } } ],
          "genera": []
        }
        """,
        300);
    stubPokemon(
        4,
        """
        {
          "id": 4,
          "name": "charmander",
          "weight": 85,
          "sprites": { "front_default": "https://example.com/4.png" },
          "abilities": [
            { "ability": { "name": "blaze" }, "is_hidden": false, "slot": 1 }
          ]
        }
        """,
        300);
    stubSpecies(
        4,
        """
        {
          "names": [ { "name": "Charmander", "language": { "name": "en" } } ],
          "genera": [ { "genus": "Lizard Pokémon", "language": { "name": "en" } } ]
        }
        """,
        300);

    Instant start = Instant.now();
    List<CatalogEntry> entries = adapter.fetchEntries(List.of(1, 2, 3, 4));
    Duration elapsed = Duration.between(start, Instant.now());

    assertThat(elapsed.toMillis()).isLessThan(1000);
    assertThat(entries).hasSize(4);

    CatalogPokemon first = entries.get(0).pokemon();
    assertThat(first.nationalNumber()).isEqualTo(1);
    assertThat(first.slug()).isEqualTo("bulbasaur");
    assertThat(first.spriteUrl()).isEqualTo("https://example.com/1.png");
    assertThat(first.weightHectograms()).isEqualTo(69);
    assertThat(first.abilities())
        .containsExactly(
            new CatalogAbility(3, "chlorophyll", true), new CatalogAbility(1, "overgrow", false));

    CatalogSpecies firstSpecies = entries.get(0).species();
    assertThat(firstSpecies.names())
        .containsExactly(new LocalizedText("ja", "フシギダネ"), new LocalizedText("en", "Bulbasaur"));
    assertThat(firstSpecies.genera())
        .containsExactly(
            new LocalizedText("en", "Seed Pokémon"), new LocalizedText("ja", "たねポケモン"));

    assertThat(entries.get(1).pokemon().spriteUrl()).isNull();
    assertThat(entries.get(2).pokemon().spriteUrl()).isNull();
    assertThat(entries).extracting(e -> e.pokemon().nationalNumber()).containsExactly(1, 2, 3, 4);
  }

  @Test
  void fetchEntriesFailsEntirelyWhenOneUpstreamReturns500() {
    stubPokemon(1, pokemonBody(1, "bulbasaur"), 0);
    stubSpecies(1, speciesBody("Bulbasaur"), 0);
    wireMock.stubFor(
        get(urlEqualTo("/pokemon/2")).willReturn(aResponse().withStatus(500).withBody("error")));
    stubSpecies(2, speciesBody("Ivysaur"), 0);

    assertThatThrownBy(() -> adapter.fetchEntries(List.of(1, 2)))
        .isInstanceOf(CatalogUnavailableException.class);
  }

  @Test
  void fetchEntriesFailsWhenDelayExceedsReadTimeout() {
    wireMock.stubFor(
        get(urlEqualTo("/pokemon/1"))
            .willReturn(
                aResponse()
                    .withFixedDelay(800)
                    .withHeader("Content-Type", "application/json")
                    .withBody(pokemonBody(1, "bulbasaur"))));
    stubSpecies(1, speciesBody("Bulbasaur"), 0);

    PokeApiCatalogAdapter shortRead =
        new PokeApiCatalogAdapter(
            wireMock.baseUrl(), Duration.ofMillis(200), Duration.ofMillis(200));
    try {
      assertThatThrownBy(() -> shortRead.fetchEntries(List.of(1)))
          .isInstanceOf(CatalogUnavailableException.class);
    } finally {
      shortRead.close();
    }
  }

  @Test
  void speciesPageFailsWhenUpstreamReturns500() {
    wireMock.stubFor(
        get(urlPathEqualTo("/pokemon-species"))
            .willReturn(aResponse().withStatus(500).withBody("error")));

    assertThatThrownBy(() -> adapter.speciesPage(0, 9))
        .isInstanceOf(CatalogUnavailableException.class);
  }

  private void stubPokemon(int id, String body, int delayMs) {
    wireMock.stubFor(
        get(urlEqualTo("/pokemon/" + id))
            .willReturn(
                aResponse()
                    .withFixedDelay(delayMs)
                    .withHeader("Content-Type", "application/json")
                    .withBody(body)));
  }

  private void stubSpecies(int id, String body, int delayMs) {
    wireMock.stubFor(
        get(urlEqualTo("/pokemon-species/" + id))
            .willReturn(
                aResponse()
                    .withFixedDelay(delayMs)
                    .withHeader("Content-Type", "application/json")
                    .withBody(body)));
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
          "genera": [ { "genus": "Seed Pokémon", "language": { "name": "en" } } ]
        }
        """
        .formatted(englishName);
  }
}
