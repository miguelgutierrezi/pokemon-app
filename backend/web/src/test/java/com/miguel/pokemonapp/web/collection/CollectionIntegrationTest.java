package com.miguel.pokemonapp.web.collection;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.miguel.pokemonapp.support.PostgresTestcontainersConfiguration;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.client.RestTestClient;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
@Import(PostgresTestcontainersConfiguration.class)
@ExtendWith(OutputCaptureExtension.class)
class CollectionIntegrationTest {

  private static final String ARTWORK_1 =
      "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/1.png";
  private static final String ARTWORK_2 =
      "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/2.png";
  private static final String ARTWORK_3 =
      "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/3.png";
  private static final String ARTWORK_25 =
      "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/25.png";

  private static final WireMockServer WIRE_MOCK =
      new WireMockServer(wireMockConfig().dynamicPort());

  static {
    WIRE_MOCK.start();
  }

  @Autowired private RestTestClient restClient;
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private Environment environment;

  @DynamicPropertySource
  static void pokeApiBaseUrl(DynamicPropertyRegistry registry) {
    registry.add("pokemonapp.pokeapi.base-url", WIRE_MOCK::baseUrl);
  }

  @AfterAll
  static void stopWireMock() {
    WIRE_MOCK.stop();
  }

  @BeforeEach
  void resetState() {
    WIRE_MOCK.resetAll();
    jdbcTemplate.update("DELETE FROM pokemon_record WHERE national_number > 3");
  }

  @Test
  void seedCollectionIsExposedWithExactV3Values() {
    restClient
        .get()
        .uri("/api/v1/collection?page=1&size=9")
        .exchange()
        .expectStatus()
        .isOk()
        .expectHeader()
        .contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
        .expectBody()
        .jsonPath("$.totalElements")
        .isEqualTo(3)
        .jsonPath("$.content[0].nationalNumber")
        .isEqualTo(1)
        .jsonPath("$.content[0].name")
        .isEqualTo("Bulbasaur")
        .jsonPath("$.content[0].localizedName")
        .isEqualTo("Bulbizarre")
        .jsonPath("$.content[0].location")
        .isEqualTo("Pallet Town garden")
        .jsonPath("$.content[0].imageUrl")
        .isEqualTo(ARTWORK_1)
        .jsonPath("$.content[0].tags")
        .value(tags -> assertTags(tags, "first-partner", "forest"))
        .jsonPath("$.content[0].version")
        .isEqualTo(0)
        .jsonPath("$.content[1].nationalNumber")
        .isEqualTo(2)
        .jsonPath("$.content[1].name")
        .isEqualTo("Ivysaur")
        .jsonPath("$.content[1].localizedName")
        .isEqualTo("Herbizarre")
        .jsonPath("$.content[1].location")
        .isEqualTo("Route 1 tall grass")
        .jsonPath("$.content[1].imageUrl")
        .isEqualTo(ARTWORK_2)
        .jsonPath("$.content[1].tags")
        .value(tags -> assertTags(tags, "field-notes", "forest"))
        .jsonPath("$.content[1].version")
        .isEqualTo(0)
        .jsonPath("$.content[2].nationalNumber")
        .isEqualTo(3)
        .jsonPath("$.content[2].name")
        .isEqualTo("Venusaur")
        .jsonPath("$.content[2].localizedName")
        .isEqualTo("Florizarre")
        .jsonPath("$.content[2].location")
        .isEqualTo("Viridian Forest clearing")
        .jsonPath("$.content[2].imageUrl")
        .isEqualTo(ARTWORK_3)
        .jsonPath("$.content[2].tags")
        .value(tags -> assertTags(tags, "first-partner", "field-notes"))
        .jsonPath("$.content[2].version")
        .isEqualTo(0);
  }

  @Test
  void importPikachuThenDuplicateThenGet() {
    stubPikachuCatalog();

    String body =
        restClient
            .post()
            .uri("/api/v1/collection")
            .contentType(MediaType.APPLICATION_JSON)
            .body("{\"nationalNumber\":25}")
            .exchange()
            .expectStatus()
            .isCreated()
            .expectHeader()
            .valueEquals("Location", "/api/v1/collection/25")
            .expectHeader()
            .contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
            .expectBody(String.class)
            .returnResult()
            .getResponseBody();

    assertThat(body)
        .contains("\"nationalNumber\":25")
        .contains("\"name\":\"Pikachu\"")
        .contains("\"category\":\"Mouse Pokémon\"")
        .contains("\"heightM\":0.4")
        .contains("\"weightKg\":6.0")
        .contains("\"generation\":\"generation-i\"")
        .contains("\"imageUrl\":\"" + ARTWORK_25 + "\"")
        .contains("\"types\":[\"electric\"]")
        .contains("\"tags\":[]")
        .contains("\"localizedName\":null")
        .contains("\"location\":null")
        .contains("\"syncedAt\"")
        .contains("\"version\":0");

    assertThat(countByNationalNumber(25)).isEqualTo(1L);
    WIRE_MOCK.verify(exactly(1), getRequestedFor(urlEqualTo("/pokemon/25")));
    WIRE_MOCK.verify(exactly(1), getRequestedFor(urlEqualTo("/pokemon-species/25")));

    WIRE_MOCK.resetRequests();

    String conflictBody =
        restClient
            .post()
            .uri("/api/v1/collection")
            .contentType(MediaType.APPLICATION_JSON)
            .body("{\"nationalNumber\":25}")
            .exchange()
            .expectStatus()
            .isEqualTo(409)
            .expectHeader()
            .contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
            .expectBody(String.class)
            .returnResult()
            .getResponseBody();

    assertThat(conflictBody)
        .contains("\"status\":409")
        .contains("already in the local collection")
        .doesNotContain("DuplicateLocalPokemonException")
        .doesNotContain("\tat ");
    assertThat(countByNationalNumber(25)).isEqualTo(1L);
    WIRE_MOCK.verify(exactly(0), getRequestedFor(urlEqualTo("/pokemon/25")));
    WIRE_MOCK.verify(exactly(0), getRequestedFor(urlEqualTo("/pokemon-species/25")));

    String getBody =
        restClient
            .get()
            .uri("/api/v1/collection/25")
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(String.class)
            .returnResult()
            .getResponseBody();

    assertThat(getBody)
        .contains("\"nationalNumber\":25")
        .contains("\"name\":\"Pikachu\"")
        .contains("\"localizedName\":null")
        .contains("\"location\":null")
        .contains("\"tags\":[]")
        .contains("\"version\":0");
  }

  @Test
  void importSeededNationalNumberConflictsWithoutCatalogCalls() {
    String body =
        restClient
            .post()
            .uri("/api/v1/collection")
            .contentType(MediaType.APPLICATION_JSON)
            .body("{\"nationalNumber\":1}")
            .exchange()
            .expectStatus()
            .isEqualTo(409)
            .expectHeader()
            .contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
            .expectBody(String.class)
            .returnResult()
            .getResponseBody();

    assertThat(body).contains("already in the local collection");
    assertThat(countByNationalNumber(1)).isEqualTo(1L);
    WIRE_MOCK.verify(exactly(0), getRequestedFor(urlEqualTo("/pokemon/1")));
    WIRE_MOCK.verify(exactly(0), getRequestedFor(urlEqualTo("/pokemon-species/1")));
  }

  @Test
  void catalogNotFoundDoesNotPersistRow() {
    WIRE_MOCK.stubFor(get(urlEqualTo("/pokemon/9999")).willReturn(aResponse().withStatus(404)));
    WIRE_MOCK.stubFor(
        get(urlEqualTo("/pokemon-species/9999")).willReturn(aResponse().withStatus(404)));

    String body =
        restClient
            .post()
            .uri("/api/v1/collection")
            .contentType(MediaType.APPLICATION_JSON)
            .body("{\"nationalNumber\":9999}")
            .exchange()
            .expectStatus()
            .isNotFound()
            .expectHeader()
            .contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
            .expectBody(String.class)
            .returnResult()
            .getResponseBody();

    assertThat(body)
        .contains("\"status\":404")
        .contains("Pokémon 9999 was not found")
        .doesNotContain(WIRE_MOCK.baseUrl())
        .doesNotContain("PokemonNotFoundException")
        .doesNotContain("\tat ");
    assertThat(countByNationalNumber(9999)).isEqualTo(0L);
  }

  @Test
  void catalogUnavailableDoesNotPersistRow() {
    WIRE_MOCK.stubFor(
        get(urlEqualTo("/pokemon/9998"))
            .willReturn(aResponse().withStatus(500).withBody("upstream failure")));
    stubSpecies(9998, minimalSpeciesBody(9998, "Missingno"));

    String body =
        restClient
            .post()
            .uri("/api/v1/collection")
            .contentType(MediaType.APPLICATION_JSON)
            .body("{\"nationalNumber\":9998}")
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
        .doesNotContain(WIRE_MOCK.baseUrl())
        .doesNotContain("pokeapi")
        .doesNotContain("/pokemon/9998")
        .doesNotContain("CatalogUnavailableException")
        .doesNotContain("\tat ");
    assertThat(countByNationalNumber(9998)).isEqualTo(0L);
  }

  @Test
  void getMissingLocalReturnsLocalNotFoundWording() {
    String body =
        restClient
            .get()
            .uri("/api/v1/collection/150")
            .exchange()
            .expectStatus()
            .isNotFound()
            .expectHeader()
            .contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
            .expectBody(String.class)
            .returnResult()
            .getResponseBody();

    assertThat(body)
        .contains("\"status\":404")
        .contains("Pokémon 150 is not in the local collection")
        .doesNotContain("was not found")
        .doesNotContain("LocalPokemonNotFoundException")
        .doesNotContain("\tat ");
  }

  @Test
  void listPageBeyondLastReturnsBadRequest() {
    String body =
        restClient
            .get()
            .uri("/api/v1/collection?page=2&size=9")
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
        .contains("beyond last page")
        .doesNotContain("PageOutOfRangeException")
        .doesNotContain("\tat ");
  }

  @Test
  void openInViewIsDisabledAndStartupLogsHaveNoWarn(CapturedOutput output) {
    assertThat(environment.getProperty("spring.jpa.open-in-view")).isEqualTo("false");
    assertThat(output.getAll()).doesNotContain("spring.jpa.open-in-view");
  }

  private static void assertTags(Object tags, String... expected) {
    assertThat(tags).isInstanceOf(List.class);
    assertThat(((List<?>) tags).stream().map(Object::toString).toList())
        .containsExactlyInAnyOrder(expected);
  }

  private long countByNationalNumber(int nationalNumber) {
    Long count =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM pokemon_record WHERE national_number = ?",
            Long.class,
            nationalNumber);
    return count == null ? 0L : count;
  }

  private static void stubPikachuCatalog() {
    stubPokemon(
        25,
        """
        {
          "id": 25,
          "name": "pikachu",
          "height": 4,
          "weight": 60,
          "sprites": {
            "front_default": "https://example.com/25.png",
            "other": {
              "official-artwork": {
                "front_default": "%s"
              }
            }
          },
          "types": [
            { "slot": 1, "type": { "name": "electric" } }
          ],
          "abilities": [
            { "ability": { "name": "static" }, "is_hidden": false, "slot": 1 }
          ],
          "stats": [
            { "base_stat": 35, "stat": { "name": "hp" } }
          ]
        }
        """
            .formatted(ARTWORK_25));
    stubSpecies(
        25,
        """
        {
          "names": [ { "name": "Pikachu", "language": { "name": "en" } } ],
          "genera": [ { "genus": "Mouse Pokémon", "language": { "name": "en" } } ],
          "flavor_text_entries": [],
          "generation": { "name": "generation-i" },
          "evolution_chain": { "url": "https://pokeapi.co/api/v2/evolution-chain/10/" }
        }
        """);
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

  private static String minimalSpeciesBody(int id, String name) {
    return """
        {
          "names": [ { "name": "%s", "language": { "name": "en" } } ],
          "genera": [ { "genus": "Test Pokémon", "language": { "name": "en" } } ],
          "flavor_text_entries": [],
          "generation": { "name": "generation-i" },
          "evolution_chain": { "url": "https://pokeapi.co/api/v2/evolution-chain/%d/" }
        }
        """
        .formatted(name, id);
  }
}
