package com.miguel.pokemonapp.web.pokemon;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.miguel.pokemonapp.support.PostgresTestcontainersConfiguration;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
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
class PokemonDetailIntegrationTest {

  private static final WireMockServer WIRE_MOCK =
      new WireMockServer(wireMockConfig().dynamicPort());

  private static final int[] EEVEE_LINE = {133, 134, 135, 136, 196, 197, 470, 471, 700};
  private static final String[] EEVEE_NAMES = {
    "Eevee", "Vaporeon", "Jolteon", "Flareon", "Espeon", "Umbreon", "Leafeon", "Glaceon", "Sylveon"
  };
  private static final String[] EEVEE_SLUGS = {
    "eevee", "vaporeon", "jolteon", "flareon", "espeon", "umbreon", "leafeon", "glaceon", "sylveon"
  };

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
  void returnsEeveeDetailWithBranchingEvolutionAndCurrentRoot() {
    stubEeveeLineCatalog();

    restClient
        .get()
        .uri("/api/v1/pokemon/133")
        .exchange()
        .expectStatus()
        .isOk()
        .expectHeader()
        .contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
        .expectBody()
        .jsonPath("$.nationalNumber")
        .isEqualTo(133)
        .jsonPath("$.name")
        .isEqualTo("Eevee")
        .jsonPath("$.evolution.nationalNumber")
        .isEqualTo(133)
        .jsonPath("$.evolution.isCurrent")
        .isEqualTo(true)
        .jsonPath("$.evolution.children.length()")
        .isEqualTo(8)
        .jsonPath("$.evolution.children[0].node.nationalNumber")
        .isEqualTo(134)
        .jsonPath("$.evolution.children[0].node.isCurrent")
        .isEqualTo(false)
        .jsonPath("$.evolution.children[0].trigger")
        .isEqualTo("use-item")
        .jsonPath("$.evolution.children[0].itemSlug")
        .isEqualTo("water-stone")
        .jsonPath("$.evolution.children[0].minLevel")
        .isEqualTo(null)
        .jsonPath("$.evolution.children[1].node.nationalNumber")
        .isEqualTo(135)
        .jsonPath("$.evolution.children[1].trigger")
        .isEqualTo("use-item")
        .jsonPath("$.evolution.children[1].itemSlug")
        .isEqualTo("thunder-stone")
        .jsonPath("$.evolution.children[2].node.nationalNumber")
        .isEqualTo(136)
        .jsonPath("$.evolution.children[2].itemSlug")
        .isEqualTo("fire-stone")
        .jsonPath("$.evolution.children[3].node.nationalNumber")
        .isEqualTo(196)
        .jsonPath("$.evolution.children[3].trigger")
        .isEqualTo("level-up")
        .jsonPath("$.evolution.children[4].node.nationalNumber")
        .isEqualTo(197)
        .jsonPath("$.evolution.children[5].node.nationalNumber")
        .isEqualTo(470)
        .jsonPath("$.evolution.children[6].node.nationalNumber")
        .isEqualTo(471)
        .jsonPath("$.evolution.children[7].node.nationalNumber")
        .isEqualTo(700);
  }

  @Test
  void returnsUmbreonDetailWithOnlyUmbreonMarkedCurrent() {
    stubEeveeLineCatalog();

    restClient
        .get()
        .uri("/api/v1/pokemon/197")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.nationalNumber")
        .isEqualTo(197)
        .jsonPath("$.name")
        .isEqualTo("Umbreon")
        .jsonPath("$.evolution.nationalNumber")
        .isEqualTo(133)
        .jsonPath("$.evolution.isCurrent")
        .isEqualTo(false)
        .jsonPath("$.evolution.children[0].node.isCurrent")
        .isEqualTo(false)
        .jsonPath("$.evolution.children[1].node.isCurrent")
        .isEqualTo(false)
        .jsonPath("$.evolution.children[2].node.isCurrent")
        .isEqualTo(false)
        .jsonPath("$.evolution.children[3].node.isCurrent")
        .isEqualTo(false)
        .jsonPath("$.evolution.children[4].node.nationalNumber")
        .isEqualTo(197)
        .jsonPath("$.evolution.children[4].node.isCurrent")
        .isEqualTo(true)
        .jsonPath("$.evolution.children[5].node.isCurrent")
        .isEqualTo(false)
        .jsonPath("$.evolution.children[6].node.isCurrent")
        .isEqualTo(false)
        .jsonPath("$.evolution.children[7].node.isCurrent")
        .isEqualTo(false);
  }

  @Test
  void returnsBulbasaurDetailWithMappedFieldsAndSprites() {
    stubBulbasaurLineCatalog();

    restClient
        .get()
        .uri("/api/v1/pokemon/1")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.nationalNumber")
        .isEqualTo(1)
        .jsonPath("$.name")
        .isEqualTo("Bulbasaur")
        .jsonPath("$.heightM")
        .isEqualTo(0.7)
        .jsonPath("$.weightKg")
        .isEqualTo(6.9)
        .jsonPath("$.stats.total")
        .isEqualTo(318)
        .jsonPath("$.flavorText.text")
        .isEqualTo(
            "A strange seed was planted on its back at birth. The plant sprouts and grows with this Pokémon.")
        .jsonPath("$.flavorText.version")
        .isEqualTo("red")
        .jsonPath("$.imageUrl")
        .isEqualTo(artwork(1))
        .jsonPath("$.evolution.isCurrent")
        .isEqualTo(true)
        .jsonPath("$.evolution.imageUrl")
        .isEqualTo(sprite(1))
        .jsonPath("$.evolution.children[0].node.imageUrl")
        .isEqualTo(sprite(2))
        .jsonPath("$.evolution.children[0].node.children[0].node.imageUrl")
        .isEqualTo(sprite(3));
  }

  @Test
  void returnsTaurosDetailWithSingleNodeEvolution() {
    stubPokemon(128, detailPokemonBody(128, "tauros", 14, 884));
    stubSpecies(128, speciesBody("Tauros", "generation-i", 63));
    stubEvolutionChain(63, resource("evolution-chain-63.json"));

    restClient
        .get()
        .uri("/api/v1/pokemon/128")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.nationalNumber")
        .isEqualTo(128)
        .jsonPath("$.name")
        .isEqualTo("Tauros")
        .jsonPath("$.evolution.nationalNumber")
        .isEqualTo(128)
        .jsonPath("$.evolution.isCurrent")
        .isEqualTo(true)
        .jsonPath("$.evolution.children.length()")
        .isEqualTo(0);
  }

  @Test
  void identityUpstream404ReturnsNotFoundProblemWithoutLeakingUpstream() {
    WIRE_MOCK.stubFor(get(urlEqualTo("/pokemon/9999")).willReturn(aResponse().withStatus(404)));
    stubSpecies(9999, speciesBody("Missing", "generation-i", 1));

    restClient
        .get()
        .uri("/api/v1/pokemon/9999")
        .exchange()
        .expectStatus()
        .isNotFound()
        .expectHeader()
        .contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
        .expectBody(String.class)
        .value(
            body -> {
              assertThat(body).contains("\"status\":404");
              assertThat(body).contains("Pokémon 9999 was not found");
              assertThat(body)
                  .doesNotContain("catalog returned")
                  .doesNotContain("/pokemon/{id}")
                  .doesNotContain("pokeapi.co");
            });
  }

  @Test
  void upstream500ReturnsBadGatewayProblemWithoutLeakingUpstream() {
    WIRE_MOCK.stubFor(
        get(urlEqualTo("/pokemon/1")).willReturn(aResponse().withStatus(500).withBody("error")));
    stubSpecies(1, speciesBody("Bulbasaur", "generation-i", 1));

    restClient
        .get()
        .uri("/api/v1/pokemon/1")
        .exchange()
        .expectStatus()
        .isEqualTo(502)
        .expectHeader()
        .contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
        .expectBody(String.class)
        .value(
            body ->
                assertThat(body)
                    .contains("\"status\":502")
                    .doesNotContain("catalog returned")
                    .doesNotContain("/pokemon/{id}")
                    .doesNotContain("pokeapi.co")
                    .doesNotContain("INTERNAL_SERVER_ERROR"));
  }

  @Test
  void detailCallHitsIdentityResourcesExactlyOnceAndSecondCallUsesCache() {
    stubBulbasaurLineCatalog();

    restClient.get().uri("/api/v1/pokemon/1").exchange().expectStatus().isOk();

    WIRE_MOCK.verify(exactly(1), getRequestedFor(urlEqualTo("/pokemon/1")));
    WIRE_MOCK.verify(exactly(1), getRequestedFor(urlEqualTo("/pokemon-species/1")));
    WIRE_MOCK.verify(exactly(1), getRequestedFor(urlEqualTo("/evolution-chain/1")));
    WIRE_MOCK.verify(exactly(1), getRequestedFor(urlEqualTo("/pokemon/2")));
    WIRE_MOCK.verify(exactly(1), getRequestedFor(urlEqualTo("/pokemon/3")));

    restClient.get().uri("/api/v1/pokemon/1").exchange().expectStatus().isOk();

    WIRE_MOCK.verify(exactly(1), getRequestedFor(urlEqualTo("/pokemon/1")));
    WIRE_MOCK.verify(exactly(1), getRequestedFor(urlEqualTo("/pokemon-species/1")));
    WIRE_MOCK.verify(exactly(1), getRequestedFor(urlEqualTo("/evolution-chain/1")));
    WIRE_MOCK.verify(exactly(1), getRequestedFor(urlEqualTo("/pokemon/2")));
    WIRE_MOCK.verify(exactly(1), getRequestedFor(urlEqualTo("/pokemon/3")));
  }

  private static void stubEeveeLineCatalog() {
    for (int i = 0; i < EEVEE_LINE.length; i++) {
      int id = EEVEE_LINE[i];
      stubPokemon(id, detailPokemonBody(id, EEVEE_SLUGS[i], 3, 65));
      stubSpecies(id, speciesBody(EEVEE_NAMES[i], "generation-i", 67));
    }
    stubEvolutionChain(67, resource("evolution-chain-67.json"));
  }

  private static void stubBulbasaurLineCatalog() {
    stubPokemon(1, bulbasaurPokemonBody());
    stubSpecies(1, bulbasaurSpeciesBody());
    stubPokemon(2, detailPokemonBody(2, "ivysaur", 10, 130));
    stubSpecies(2, speciesBody("Ivysaur", "generation-i", 1));
    stubPokemon(3, detailPokemonBody(3, "venusaur", 20, 1000));
    stubSpecies(3, speciesBody("Venusaur", "generation-i", 1));
    stubEvolutionChain(1, resource("evolution-chain-1.json"));
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

  private static void stubEvolutionChain(int id, String body) {
    WIRE_MOCK.stubFor(
        get(urlEqualTo("/evolution-chain/" + id))
            .willReturn(aResponse().withHeader("Content-Type", "application/json").withBody(body)));
  }

  private static String bulbasaurPokemonBody() {
    return """
        {
          "id": 1,
          "name": "bulbasaur",
          "height": 7,
          "weight": 69,
          "sprites": {
            "front_default": "%s",
            "other": {
              "official-artwork": {
                "front_default": "%s"
              }
            }
          },
          "abilities": [
            { "ability": { "name": "chlorophyll" }, "is_hidden": true, "slot": 3 },
            { "ability": { "name": "overgrow" }, "is_hidden": false, "slot": 1 }
          ],
          "types": [
            { "slot": 1, "type": { "name": "grass" } },
            { "slot": 2, "type": { "name": "poison" } }
          ],
          "stats": [
            { "base_stat": 45, "stat": { "name": "hp" } },
            { "base_stat": 49, "stat": { "name": "attack" } },
            { "base_stat": 49, "stat": { "name": "defense" } },
            { "base_stat": 65, "stat": { "name": "special-attack" } },
            { "base_stat": 65, "stat": { "name": "special-defense" } },
            { "base_stat": 45, "stat": { "name": "speed" } }
          ]
        }
        """
        .formatted(sprite(1), artwork(1));
  }

  private static String bulbasaurSpeciesBody() {
    return """
        {
          "names": [
            { "name": "フシギダネ", "language": { "name": "ja" } },
            { "name": "Bulbasaur", "language": { "name": "en" } }
          ],
          "genera": [
            { "genus": "Seed Pokémon", "language": { "name": "en" } }
          ],
          "flavor_text_entries": [
            {
              "flavor_text": "A strange seed was\\nplanted on its\\nback at birth.\\fThe plant sprouts\\nand grows with\\nthis POKéMON.",
              "language": { "name": "en" },
              "version": { "name": "red" }
            }
          ],
          "generation": { "name": "generation-i" },
          "evolution_chain": { "url": "https://pokeapi.co/api/v2/evolution-chain/1/" }
        }
        """;
  }

  private static String detailPokemonBody(int id, String slug, int height, int weight) {
    return """
        {
          "id": %d,
          "name": "%s",
          "height": %d,
          "weight": %d,
          "sprites": {
            "front_default": "%s",
            "other": {
              "official-artwork": {
                "front_default": "%s"
              }
            }
          },
          "abilities": [
            { "ability": { "name": "run-away" }, "is_hidden": false, "slot": 1 }
          ],
          "types": [
            { "slot": 1, "type": { "name": "normal" } }
          ],
          "stats": [
            { "base_stat": 55, "stat": { "name": "hp" } },
            { "base_stat": 55, "stat": { "name": "attack" } },
            { "base_stat": 50, "stat": { "name": "defense" } },
            { "base_stat": 45, "stat": { "name": "special-attack" } },
            { "base_stat": 65, "stat": { "name": "special-defense" } },
            { "base_stat": 55, "stat": { "name": "speed" } }
          ]
        }
        """
        .formatted(id, slug, height, weight, sprite(id), artwork(id));
  }

  private static String speciesBody(String englishName, String generation, int chainId) {
    return """
        {
          "names": [ { "name": "%s", "language": { "name": "en" } } ],
          "genera": [ { "genus": "Pokémon", "language": { "name": "en" } } ],
          "flavor_text_entries": [],
          "generation": { "name": "%s" },
          "evolution_chain": {
            "url": "https://pokeapi.co/api/v2/evolution-chain/%d/"
          }
        }
        """
        .formatted(englishName, generation, chainId);
  }

  private static String sprite(int id) {
    return "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/"
        + id
        + ".png";
  }

  private static String artwork(int id) {
    return "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/"
        + id
        + ".png";
  }

  private static String resource(String name) {
    String path = "/pokeapi/" + name;
    try (InputStream in = PokemonDetailIntegrationTest.class.getResourceAsStream(path)) {
      Objects.requireNonNull(in, "missing test resource " + path);
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new IllegalStateException("failed to read " + path, e);
    }
  }
}
