package com.miguel.pokemonapp.web.collection;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.miguel.pokemonapp.infrastructure.security.JwtAccessTokenIssuer;
import com.miguel.pokemonapp.support.PostgresTestcontainersConfiguration;
import com.miguel.pokemonapp.web.support.AuthTestSupport;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.client.RestTestClient;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
@Import(PostgresTestcontainersConfiguration.class)
class CollectionUpdateIntegrationTest {

  private static final String ARTWORK_1 =
      "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/1.png";
  private static final String ARTWORK_2 =
      "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/2.png";

  private static final WireMockServer WIRE_MOCK =
      new WireMockServer(wireMockConfig().dynamicPort());

  static {
    WIRE_MOCK.start();
  }

  @Autowired private RestTestClient restClient;
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private JwtAccessTokenIssuer jwtAccessTokenIssuer;

  private String demoAuth;

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
    CollectionSeedReset.restore(jdbcTemplate);
    demoAuth = AuthTestSupport.bearerForDemoUser(jwtAccessTokenIssuer, jdbcTemplate);
  }

  @Test
  void putSeedBulbasaurUpdatesProprietaryAdvancesVersionAndUpdatedAt() {
    Map<String, Object> before = loadRecord(1);
    Instant syncedAtBefore = toInstant(before.get("synced_at"));
    Instant updatedAtBefore = toInstant(before.get("updated_at"));

    String body =
        restClient
            .put()
            .uri("/api/v1/collection/1")
            .header(HttpHeaders.AUTHORIZATION, demoAuth)
            .contentType(MediaType.APPLICATION_JSON)
            .body(
                """
                {
                  "localizedName": "  My Bulba  ",
                  "location": "  Cerulean Cave  ",
                  "tags": [" GARDEN ", "Notes"],
                  "version": 0
                }
                """)
            .exchange()
            .expectStatus()
            .isOk()
            .expectHeader()
            .contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
            .expectBody(String.class)
            .returnResult()
            .getResponseBody();

    assertThat(body)
        .contains("\"nationalNumber\":1")
        .contains("\"name\":\"Bulbasaur\"")
        .contains("\"localizedName\":\"My Bulba\"")
        .contains("\"location\":\"Cerulean Cave\"")
        .contains("\"version\":1")
        .contains("\"syncedAt\":\"2026-01-01T00:00:00Z\"");
    assertThat(body).contains("\"types\":[\"grass\",\"poison\"]");

    Map<String, Object> after = loadRecord(1);
    assertThat(after.get("name")).isEqualTo("Bulbasaur");
    assertThat(after.get("localized_name")).isEqualTo("My Bulba");
    assertThat(after.get("location")).isEqualTo("Cerulean Cave");
    assertThat(((Number) after.get("version")).longValue()).isEqualTo(1L);
    assertThat(((BigDecimal) after.get("height_m")).compareTo(new BigDecimal("0.7"))).isZero();
    assertThat(((BigDecimal) after.get("weight_kg")).compareTo(new BigDecimal("6.9"))).isZero();
    assertThat(after.get("image_url")).isEqualTo(ARTWORK_1);
    assertThat(after.get("generation")).isEqualTo("generation-i");
    assertThat(toInstant(after.get("synced_at"))).isEqualTo(syncedAtBefore);
    assertThat(toInstant(after.get("updated_at"))).isAfter(updatedAtBefore);
    assertThat(tagsFor(1)).containsExactlyInAnyOrder("garden", "notes");
    assertThat(typesFor(1)).containsExactly("grass", "poison");
  }

  @Test
  void twoSequentialPutsWithSameVersionSecondIsStaleAndFirstValuesWin() {
    restClient
        .put()
        .uri("/api/v1/collection/1")
        .header(HttpHeaders.AUTHORIZATION, demoAuth)
        .contentType(MediaType.APPLICATION_JSON)
        .body(
            """
            {
              "localizedName": "First Wins",
              "location": "First Location",
              "tags": ["alpha"],
              "version": 0
            }
            """)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.version")
        .isEqualTo(1)
        .jsonPath("$.localizedName")
        .isEqualTo("First Wins");

    String conflict =
        restClient
            .put()
            .uri("/api/v1/collection/1")
            .header(HttpHeaders.AUTHORIZATION, demoAuth)
            .contentType(MediaType.APPLICATION_JSON)
            .body(
                """
                {
                  "localizedName": "Second Loses",
                  "location": "Second Location",
                  "tags": ["beta"],
                  "version": 0
                }
                """)
            .exchange()
            .expectStatus()
            .isEqualTo(409)
            .expectHeader()
            .contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
            .expectBody(String.class)
            .returnResult()
            .getResponseBody();

    assertThat(conflict)
        .contains("\"type\":\"urn:problem-type:stale-version\"")
        .contains("Pokémon 1 was modified by another request; reload and retry")
        .doesNotContain("urn:problem-type:conflict")
        .doesNotContain("StaleLocalPokemonException")
        .doesNotContain("\tat ");

    Map<String, Object> stored = loadRecord(1);
    assertThat(stored.get("localized_name")).isEqualTo("First Wins");
    assertThat(stored.get("location")).isEqualTo("First Location");
    assertThat(((Number) stored.get("version")).longValue()).isEqualTo(1L);
    assertThat(tagsFor(1)).containsExactly("alpha");
  }

  @Test
  void putNullTextsAndEmptyTagsClearsFieldsInDb() {
    restClient
        .put()
        .uri("/api/v1/collection/1")
        .header(HttpHeaders.AUTHORIZATION, demoAuth)
        .contentType(MediaType.APPLICATION_JSON)
        .body(
            """
            {
              "localizedName": null,
              "location": null,
              "tags": [],
              "version": 0
            }
            """)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.localizedName")
        .isEmpty()
        .jsonPath("$.location")
        .isEmpty()
        .jsonPath("$.tags")
        .isEmpty()
        .jsonPath("$.version")
        .isEqualTo(1);

    Map<String, Object> stored = loadRecord(1);
    assertThat(stored.get("localized_name")).isNull();
    assertThat(stored.get("location")).isNull();
    assertThat(tagsFor(1)).isEmpty();
  }

  @Test
  void putUnknownAndReadOnlyPropertiesReturn400NamingProperty() {
    String unknown =
        restClient
            .put()
            .uri("/api/v1/collection/1")
            .header(HttpHeaders.AUTHORIZATION, demoAuth)
            .contentType(MediaType.APPLICATION_JSON)
            .body(
                """
                {
                  "localizedName": "x",
                  "location": "y",
                  "tags": [],
                  "version": 0,
                  "extra": true
                }
                """)
            .exchange()
            .expectStatus()
            .isBadRequest()
            .expectHeader()
            .contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
            .expectBody(String.class)
            .returnResult()
            .getResponseBody();

    assertThat(unknown)
        .contains("Unknown or read-only property 'extra'")
        .doesNotContain("tools.jackson")
        .doesNotContain("com.fasterxml");

    String readOnly =
        restClient
            .put()
            .uri("/api/v1/collection/1")
            .header(HttpHeaders.AUTHORIZATION, demoAuth)
            .contentType(MediaType.APPLICATION_JSON)
            .body(
                """
                {
                  "localizedName": "x",
                  "location": "y",
                  "tags": [],
                  "version": 0,
                  "name": "Hack"
                }
                """)
            .exchange()
            .expectStatus()
            .isBadRequest()
            .expectHeader()
            .contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
            .expectBody(String.class)
            .returnResult()
            .getResponseBody();

    assertThat(readOnly)
        .contains("Unknown or read-only property 'name'")
        .doesNotContain("tools.jackson")
        .doesNotContain("com.fasterxml");
  }

  @Test
  void putLimitAndBlankTagReturn400NamingField() {
    String tooLongName = "x".repeat(101);
    String limits =
        restClient
            .put()
            .uri("/api/v1/collection/1")
            .header(HttpHeaders.AUTHORIZATION, demoAuth)
            .contentType(MediaType.APPLICATION_JSON)
            .body(
                """
                {
                  "localizedName": "%s",
                  "location": "y",
                  "tags": [],
                  "version": 0
                }
                """
                    .formatted(tooLongName))
            .exchange()
            .expectStatus()
            .isBadRequest()
            .expectHeader()
            .contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
            .expectBody(String.class)
            .returnResult()
            .getResponseBody();

    assertThat(limits).contains("localizedName");

    String blankTag =
        restClient
            .put()
            .uri("/api/v1/collection/1")
            .header(HttpHeaders.AUTHORIZATION, demoAuth)
            .contentType(MediaType.APPLICATION_JSON)
            .body(
                """
                {
                  "localizedName": "x",
                  "location": "y",
                  "tags": ["  "],
                  "version": 0
                }
                """)
            .exchange()
            .expectStatus()
            .isBadRequest()
            .expectHeader()
            .contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
            .expectBody(String.class)
            .returnResult()
            .getResponseBody();

    assertThat(blankTag).contains("tags[0]");

    String nullTags =
        restClient
            .put()
            .uri("/api/v1/collection/1")
            .header(HttpHeaders.AUTHORIZATION, demoAuth)
            .contentType(MediaType.APPLICATION_JSON)
            .body(
                """
                {
                  "localizedName": "x",
                  "location": "y",
                  "tags": null,
                  "version": 0
                }
                """)
            .exchange()
            .expectStatus()
            .isBadRequest()
            .expectHeader()
            .contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
            .expectBody(String.class)
            .returnResult()
            .getResponseBody();

    assertThat(nullTags).containsIgnoringCase("tags");
  }

  @Test
  void putAndDeleteMissingRecordReturnLocalNotFound() {
    String putBody =
        restClient
            .put()
            .uri("/api/v1/collection/150")
            .header(HttpHeaders.AUTHORIZATION, demoAuth)
            .contentType(MediaType.APPLICATION_JSON)
            .body(
                """
                {
                  "localizedName": null,
                  "location": null,
                  "tags": [],
                  "version": 0
                }
                """)
            .exchange()
            .expectStatus()
            .isNotFound()
            .expectHeader()
            .contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
            .expectBody(String.class)
            .returnResult()
            .getResponseBody();

    assertThat(putBody)
        .contains("Pokémon 150 is not in the local collection")
        .doesNotContain("was not found");

    String deleteBody =
        restClient
            .delete()
            .uri("/api/v1/collection/150")
            .header(HttpHeaders.AUTHORIZATION, demoAuth)
            .exchange()
            .expectStatus()
            .isNotFound()
            .expectHeader()
            .contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
            .expectBody(String.class)
            .returnResult()
            .getResponseBody();

    assertThat(deleteBody)
        .contains("Pokémon 150 is not in the local collection")
        .doesNotContain("was not found");
  }

  @Test
  void deleteSeedIvysaurThenGet404ThenReimport201() {
    long ivysaurId = recordId(2);

    restClient
        .delete()
        .uri("/api/v1/collection/2")
        .header(HttpHeaders.AUTHORIZATION, demoAuth)
        .exchange()
        .expectStatus()
        .isNoContent();

    restClient
        .get()
        .uri("/api/v1/collection/2")
        .exchange()
        .expectStatus()
        .isNotFound()
        .expectHeader()
        .contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
        .expectBody()
        .jsonPath("$.detail")
        .isEqualTo("Pokémon 2 is not in the local collection");

    assertThat(countByNationalNumber(2)).isZero();
    assertThat(countTypesForRecordId(ivysaurId)).isZero();
    assertThat(countTagsForRecordId(ivysaurId)).isZero();

    stubIvysaurCatalog();

    String created =
        restClient
            .post()
            .uri("/api/v1/collection")
            .header(HttpHeaders.AUTHORIZATION, demoAuth)
            .contentType(MediaType.APPLICATION_JSON)
            .body("{\"nationalNumber\":2}")
            .exchange()
            .expectStatus()
            .isCreated()
            .expectHeader()
            .valueEquals("Location", "/api/v1/collection/2")
            .expectBody(String.class)
            .returnResult()
            .getResponseBody();

    assertThat(created)
        .contains("\"nationalNumber\":2")
        .contains("\"name\":\"Ivysaur\"")
        .contains("\"version\":0")
        .contains("\"tags\":[]")
        .contains("\"localizedName\":null");
    assertThat(countByNationalNumber(2)).isEqualTo(1L);
  }

  private Map<String, Object> loadRecord(int nationalNumber) {
    return jdbcTemplate.queryForMap(
        "SELECT * FROM pokemon_record WHERE national_number = ?", nationalNumber);
  }

  private long recordId(int nationalNumber) {
    Long id =
        jdbcTemplate.queryForObject(
            "SELECT id FROM pokemon_record WHERE national_number = ?", Long.class, nationalNumber);
    assertThat(id).isNotNull();
    return id;
  }

  private List<String> tagsFor(int nationalNumber) {
    return jdbcTemplate.query(
        """
        SELECT t.tag FROM pokemon_record_tag t
        JOIN pokemon_record r ON r.id = t.record_id
        WHERE r.national_number = ?
        ORDER BY t.tag
        """,
        (rs, rowNum) -> rs.getString(1),
        nationalNumber);
  }

  private List<String> typesFor(int nationalNumber) {
    return jdbcTemplate.query(
        """
        SELECT t.type FROM pokemon_record_type t
        JOIN pokemon_record r ON r.id = t.record_id
        WHERE r.national_number = ?
        ORDER BY t.slot
        """,
        (rs, rowNum) -> rs.getString(1),
        nationalNumber);
  }

  private long countByNationalNumber(int nationalNumber) {
    Long count =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM pokemon_record WHERE national_number = ?",
            Long.class,
            nationalNumber);
    return count == null ? 0L : count;
  }

  private long countTypesForRecordId(long recordId) {
    Long count =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM pokemon_record_type WHERE record_id = ?", Long.class, recordId);
    return count == null ? 0L : count;
  }

  private long countTagsForRecordId(long recordId) {
    Long count =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM pokemon_record_tag WHERE record_id = ?", Long.class, recordId);
    return count == null ? 0L : count;
  }

  private static Instant toInstant(Object value) {
    if (value instanceof Timestamp timestamp) {
      return timestamp.toInstant();
    }
    if (value instanceof Instant instant) {
      return instant;
    }
    throw new IllegalArgumentException("Unexpected temporal: " + value);
  }

  private static void stubIvysaurCatalog() {
    WIRE_MOCK.stubFor(
        get(urlEqualTo("/pokemon/2"))
            .willReturn(
                aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withBody(
                        """
                        {
                          "id": 2,
                          "name": "ivysaur",
                          "height": 10,
                          "weight": 130,
                          "sprites": {
                            "front_default": "https://example.com/2.png",
                            "other": {
                              "official-artwork": {
                                "front_default": "%s"
                              }
                            }
                          },
                          "types": [
                            { "slot": 1, "type": { "name": "grass" } },
                            { "slot": 2, "type": { "name": "poison" } }
                          ],
                          "abilities": [
                            { "ability": { "name": "overgrow" }, "is_hidden": false, "slot": 1 }
                          ],
                          "stats": [
                            { "base_stat": 60, "stat": { "name": "hp" } }
                          ]
                        }
                        """
                            .formatted(ARTWORK_2))));
    WIRE_MOCK.stubFor(
        get(urlEqualTo("/pokemon-species/2"))
            .willReturn(
                aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withBody(
                        """
                        {
                          "names": [ { "name": "Ivysaur", "language": { "name": "en" } } ],
                          "genera": [ { "genus": "Seed Pokémon", "language": { "name": "en" } } ],
                          "flavor_text_entries": [],
                          "generation": { "name": "generation-i" },
                          "evolution_chain": { "url": "https://pokeapi.co/api/v2/evolution-chain/1/" }
                        }
                        """)));
  }
}
