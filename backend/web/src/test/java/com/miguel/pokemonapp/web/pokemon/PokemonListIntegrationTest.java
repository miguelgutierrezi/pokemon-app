package com.miguel.pokemonapp.web.pokemon;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.miguel.pokemonapp.support.PostgresTestcontainersConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.client.RestTestClient;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
@Import(PostgresTestcontainersConfiguration.class)
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
class PokemonListIntegrationTest {

  private static final WireMockServer WIRE_MOCK =
      new WireMockServer(wireMockConfig().dynamicPort());

  static {
    WIRE_MOCK.start();
  }

  @Autowired private RestTestClient restClient;

  @DynamicPropertySource
  static void pokeApiBaseUrl(DynamicPropertyRegistry registry) {
    registry.add("pokemonapp.pokeapi.base-url", WIRE_MOCK::baseUrl);
  }

  @AfterAll
  static void stopWireMock() {
    WIRE_MOCK.stop();
  }

  @BeforeEach
  void resetWireMock() {
    WIRE_MOCK.resetAll();
  }

  @Test
  void listsFirstPageWithMappedFields() {
    stubSpeciesPage(0, 2, 1025, 1, 2);
    stubPokemon(
        1,
        """
        {
          "id": 1,
          "name": "bulbasaur",
          "weight": 69,
          "sprites": { "front_default": "https://example.com/1.png" },
          "abilities": [
            { "ability": { "name": "chlorophyll" }, "is_hidden": true, "slot": 3 },
            { "ability": { "name": "overgrow" }, "is_hidden": false, "slot": 1 }
          ]
        }
        """);
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
          ],
          "generation": { "name": "generation-i" },
          "evolution_chain": { "url": "https://pokeapi.co/api/v2/evolution-chain/1/" }
        }
        """);
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
        """);
    stubSpecies(
        2,
        """
        {
          "names": [ { "name": "Ivysaur", "language": { "name": "en" } } ],
          "genera": [ { "genus": "Seed Pokémon", "language": { "name": "en" } } ],
          "generation": { "name": "generation-i" },
          "evolution_chain": { "url": "https://pokeapi.co/api/v2/evolution-chain/1/" }
        }
        """);

    restClient
        .get()
        .uri("/api/v1/pokemon?page=1&size=2")
        .exchange()
        .expectStatus()
        .isOk()
        .expectHeader()
        .contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
        .expectBody()
        .jsonPath("$.content[0].nationalNumber")
        .isEqualTo(1)
        .jsonPath("$.content[0].name")
        .isEqualTo("Bulbasaur")
        .jsonPath("$.content[0].spriteUrl")
        .isEqualTo("https://example.com/1.png")
        .jsonPath("$.content[0].category")
        .isEqualTo("Seed Pokémon")
        .jsonPath("$.content[0].weightKg")
        .isEqualTo(6.9)
        .jsonPath("$.content[0].abilities[0].name")
        .isEqualTo("overgrow")
        .jsonPath("$.content[0].abilities[0].hidden")
        .isEqualTo(false)
        .jsonPath("$.content[0].abilities[1].name")
        .isEqualTo("chlorophyll")
        .jsonPath("$.content[0].abilities[1].hidden")
        .isEqualTo(true)
        .jsonPath("$.content[1].nationalNumber")
        .isEqualTo(2)
        .jsonPath("$.content[1].name")
        .isEqualTo("Ivysaur")
        .jsonPath("$.content[1].spriteUrl")
        .isEqualTo(null)
        .jsonPath("$.page")
        .isEqualTo(1)
        .jsonPath("$.size")
        .isEqualTo(2)
        .jsonPath("$.totalElements")
        .isEqualTo(1025)
        .jsonPath("$.totalPages")
        .isEqualTo(513);
  }

  @Test
  void secondIdenticalRequestHitsUpstreamZeroExtraTimes() {
    stubSpeciesPage(0, 2, 1025, 1, 2);
    stubPokemon(1, pokemonBody(1, "bulbasaur", "https://example.com/1.png"));
    stubSpecies(1, speciesBody("Bulbasaur"));
    stubPokemon(2, pokemonBody(2, "ivysaur", null));
    stubSpecies(2, speciesBody("Ivysaur"));

    restClient.get().uri("/api/v1/pokemon?page=1&size=2").exchange().expectStatus().isOk();
    restClient.get().uri("/api/v1/pokemon?page=1&size=2").exchange().expectStatus().isOk();

    WIRE_MOCK.verify(
        exactly(1),
        getRequestedFor(urlPathEqualTo("/pokemon-species"))
            .withQueryParam("offset", equalTo("0"))
            .withQueryParam("limit", equalTo("2")));
    WIRE_MOCK.verify(exactly(1), getRequestedFor(urlEqualTo("/pokemon/1")));
    WIRE_MOCK.verify(exactly(1), getRequestedFor(urlEqualTo("/pokemon/2")));
    WIRE_MOCK.verify(exactly(1), getRequestedFor(urlEqualTo("/pokemon-species/1")));
    WIRE_MOCK.verify(exactly(1), getRequestedFor(urlEqualTo("/pokemon-species/2")));
  }

  @Test
  void upstreamSpeciesListErrorReturnsBadGatewayProblemWithoutUrlLeak() {
    WIRE_MOCK.stubFor(
        get(urlPathEqualTo("/pokemon-species"))
            .willReturn(aResponse().withStatus(500).withBody("upstream failure")));

    String body =
        restClient
            .get()
            .uri("/api/v1/pokemon?page=1&size=2")
            .exchange()
            .expectStatus()
            .isEqualTo(502)
            .expectHeader()
            .contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
            .expectBody(String.class)
            .returnResult()
            .getResponseBody();

    assertThat(body)
        .contains("\"status\":502")
        .contains("\"title\"")
        .contains("\"detail\"")
        .contains("\"type\"")
        .doesNotContain("pokemon-species")
        .doesNotContain(WIRE_MOCK.baseUrl())
        .doesNotContain("CatalogUnavailableException")
        .doesNotContain("\tat ");
  }

  @Test
  void pageBeyondLastReturnsBadRequestAndDoesNotFetchPokemonById() {
    WIRE_MOCK.stubFor(
        get(urlPathEqualTo("/pokemon-species"))
            .withQueryParam("offset", equalTo("2"))
            .withQueryParam("limit", equalTo("2"))
            .willReturn(
                aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withBody(
                        """
                        { "count": 2, "results": [] }
                        """)));

    String body =
        restClient
            .get()
            .uri("/api/v1/pokemon?page=2&size=2")
            .exchange()
            .expectStatus()
            .isBadRequest()
            .expectHeader()
            .contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
            .expectBody(String.class)
            .returnResult()
            .getResponseBody();

    assertThat(body)
        .contains("\"status\":400")
        .contains("\"detail\"")
        .doesNotContain("PageOutOfRangeException")
        .doesNotContain("\tat ");

    WIRE_MOCK.verify(exactly(0), getRequestedFor(urlPathMatching("/pokemon/[0-9]+")));
    WIRE_MOCK.verify(exactly(0), getRequestedFor(urlPathMatching("/pokemon-species/[0-9]+")));
  }

  private static void stubSpeciesPage(int offset, int limit, long count, int... ids) {
    StringBuilder results = new StringBuilder();
    for (int i = 0; i < ids.length; i++) {
      if (i > 0) {
        results.append(',');
      }
      results
          .append("{\"name\":\"p")
          .append(ids[i])
          .append("\",\"url\":\"https://pokeapi.co/api/v2/pokemon-species/")
          .append(ids[i])
          .append("/\"}");
    }
    WIRE_MOCK.stubFor(
        get(urlPathEqualTo("/pokemon-species"))
            .withQueryParam("offset", equalTo(String.valueOf(offset)))
            .withQueryParam("limit", equalTo(String.valueOf(limit)))
            .willReturn(
                aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"count\":" + count + ",\"results\":[" + results + "]}")));
  }

  private static void stubPokemon(int id, String body) {
    WIRE_MOCK.stubFor(
        get(urlEqualTo("/pokemon/" + id))
            .willReturn(aResponse().withHeader("Content-Type", "application/json").withBody(body)));
  }

  private static void stubSpecies(int id, String body) {
    WIRE_MOCK.stubFor(
        get(urlEqualTo("/pokemon-species/" + id))
            .willReturn(aResponse().withHeader("Content-Type", "application/json").withBody(body)));
  }

  private static String pokemonBody(int id, String name, String spriteUrl) {
    String sprite = spriteUrl == null ? "null" : "\"" + spriteUrl + "\"";
    return """
        {
          "id": %d,
          "name": "%s",
          "weight": 69,
          "sprites": { "front_default": %s },
          "abilities": [
            { "ability": { "name": "overgrow" }, "is_hidden": false, "slot": 1 }
          ]
        }
        """
        .formatted(id, name, sprite);
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
}
