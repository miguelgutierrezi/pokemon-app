package com.miguel.pokemonapp.web.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.miguel.pokemonapp.application.port.IssuedAccessToken;
import com.miguel.pokemonapp.infrastructure.security.JwtAccessTokenIssuer;
import com.miguel.pokemonapp.support.PostgresTestcontainersConfiguration;
import com.miguel.pokemonapp.web.collection.CollectionSeedReset;
import com.miguel.pokemonapp.web.config.JwtConfiguration;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
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
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.web.servlet.client.RestTestClient;

/**
 * RED expectations for the security route policy (T018). Without a filter chain, 401/403 cases
 * fail; public GETs and auth/docs/health already behave as desired.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
@Import(PostgresTestcontainersConfiguration.class)
class SecurityRoutePolicyIntegrationTest {

  private static final String DEMO_EMAIL = "demo@pokefolio.dev";
  private static final String DEMO_PASSWORD = "Demo-Passw0rd!";
  private static final String STALE_PUT_BODY =
      """
      {
        "localizedName": "x",
        "location": "y",
        "tags": [],
        "version": 99
      }
      """;

  @Autowired private RestTestClient restClient;
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private JwtAccessTokenIssuer jwtAccessTokenIssuer;
  @Autowired private JwtEncoder jwtEncoder;

  private long demoUserId;
  private String validToken;
  private String expiredToken;
  private String tamperedToken;
  private final List<String> createdEmails = new ArrayList<>();

  @BeforeEach
  void setUp() {
    CollectionSeedReset.restore(jdbcTemplate);
    createdEmails.clear();
    demoUserId =
        jdbcTemplate.queryForObject(
            "SELECT id FROM app_user WHERE email = ?", Long.class, DEMO_EMAIL);
    IssuedAccessToken issued = jwtAccessTokenIssuer.issue(demoUserId);
    validToken = issued.accessToken();
    expiredToken = mintExpiredToken(demoUserId);
    tamperedToken = tamperPayload(validToken);
  }

  @AfterEach
  void tearDownCreatedUsers() {
    for (String email : createdEmails) {
      jdbcTemplate.update("DELETE FROM app_user WHERE email = ?", email);
    }
    createdEmails.clear();
  }

  @Test
  void getCollectionIgnoresGarbageAndExpiredBearer() {
    restClient
        .get()
        .uri("/api/v1/collection")
        .header(HttpHeaders.AUTHORIZATION, "Bearer garbage")
        .exchange()
        .expectStatus()
        .isOk();

    restClient
        .get()
        .uri("/api/v1/collection")
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredToken)
        .exchange()
        .expectStatus()
        .isOk();
  }

  @Test
  void getMissingLocalRecordReturnsNotFoundNotUnauthorized() {
    String withoutToken =
        restClient
            .get()
            .uri("/api/v1/collection/99999")
            .exchange()
            .expectStatus()
            .isNotFound()
            .expectHeader()
            .contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
            .expectBody(String.class)
            .returnResult()
            .getResponseBody();

    assertThat(withoutToken)
        .contains("\"type\":\"urn:problem-type:not-found\"")
        .doesNotContain("urn:problem-type:unauthorized");

    String withExpired =
        restClient
            .get()
            .uri("/api/v1/collection/99999")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredToken)
            .exchange()
            .expectStatus()
            .isNotFound()
            .expectHeader()
            .contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
            .expectBody(String.class)
            .returnResult()
            .getResponseBody();

    assertThat(withExpired)
        .contains("\"type\":\"urn:problem-type:not-found\"")
        .doesNotContain("urn:problem-type:unauthorized");
  }

  @Test
  void postCollectionWithoutTokenReturnsUnauthorizedProblemWithWwwAuthenticate() {
    var result =
        restClient
            .post()
            .uri("/api/v1/collection")
            .contentType(MediaType.APPLICATION_JSON)
            .body("{\"nationalNumber\":1}")
            .exchange()
            .expectStatus()
            .isUnauthorized()
            .expectHeader()
            .contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
            .expectHeader()
            .value(HttpHeaders.WWW_AUTHENTICATE, value -> assertThat(value).startsWith("Bearer"))
            .expectBody(String.class)
            .returnResult();

    String body = result.getResponseBody();
    assertThat(body).contains("\"type\":\"urn:problem-type:unauthorized\"");
    assertNoLeak(body);
  }

  @Test
  void postCollectionWithValidTokenReachesControllerAsDuplicateWithoutPokeApi() {
    String body =
        restClient
            .post()
            .uri("/api/v1/collection")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken)
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

    assertThat(body)
        .contains("\"type\":\"urn:problem-type:conflict\"")
        .contains("already in the local collection");
  }

  @Test
  void putCollectionRejectsExpiredAndTamperedTokensAndAcceptsValidForStaleVersion() {
    restClient
        .put()
        .uri("/api/v1/collection/1")
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredToken)
        .contentType(MediaType.APPLICATION_JSON)
        .body(STALE_PUT_BODY)
        .exchange()
        .expectStatus()
        .isUnauthorized();

    restClient
        .put()
        .uri("/api/v1/collection/1")
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tamperedToken)
        .contentType(MediaType.APPLICATION_JSON)
        .body(STALE_PUT_BODY)
        .exchange()
        .expectStatus()
        .isUnauthorized();

    String body =
        restClient
            .put()
            .uri("/api/v1/collection/1")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken)
            .contentType(MediaType.APPLICATION_JSON)
            .body(STALE_PUT_BODY)
            .exchange()
            .expectStatus()
            .isEqualTo(409)
            .expectHeader()
            .contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
            .expectBody(String.class)
            .returnResult()
            .getResponseBody();

    assertThat(body)
        .contains("\"type\":\"urn:problem-type:stale-version\"")
        .doesNotContain("urn:problem-type:conflict");
  }

  @Test
  void deleteCollectionWithoutTokenReturnsUnauthorizedAndKeepsRow() {
    restClient.delete().uri("/api/v1/collection/3").exchange().expectStatus().isUnauthorized();

    Long remaining =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM pokemon_record WHERE national_number = 3", Long.class);
    assertThat(remaining).isEqualTo(1L);
  }

  @Test
  void unknownApiPathIsUnauthorizedAnonymousAndForbiddenWhenAuthenticated() {
    String anonymous =
        restClient
            .get()
            .uri("/api/v1/does-not-exist")
            .exchange()
            .expectStatus()
            .isUnauthorized()
            .expectHeader()
            .contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
            .expectBody(String.class)
            .returnResult()
            .getResponseBody();

    assertThat(anonymous).contains("\"type\":\"urn:problem-type:unauthorized\"");
    assertNoLeak(anonymous);

    String authenticated =
        restClient
            .get()
            .uri("/api/v1/does-not-exist")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken)
            .exchange()
            .expectStatus()
            .isForbidden()
            .expectHeader()
            .contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
            .expectBody(String.class)
            .returnResult()
            .getResponseBody();

    assertThat(authenticated).contains("\"type\":\"urn:problem-type:forbidden\"");
    assertNoLeak(authenticated);
  }

  @Test
  void authLoginIgnoresGarbageBearerAndDocsHealthStayPublic() {
    restClient
        .post()
        .uri("/api/v1/auth/login")
        .header(HttpHeaders.AUTHORIZATION, "Bearer garbage")
        .contentType(MediaType.APPLICATION_JSON)
        .body(
            """
            {
              "email": "%s",
              "password": "%s"
            }
            """
                .formatted(DEMO_EMAIL, DEMO_PASSWORD))
        .exchange()
        .expectStatus()
        .isOk()
        .expectHeader()
        .contentTypeCompatibleWith(MediaType.APPLICATION_JSON);

    restClient.get().uri("/actuator/health").exchange().expectStatus().isOk();
    restClient.get().uri("/v3/api-docs").exchange().expectStatus().isOk();
  }

  @Test
  void getCatalogPageZeroWithGarbageBearerReturnsBadRequestWithoutPokeApi() {
    String body =
        restClient
            .get()
            .uri("/api/v1/pokemon?page=0")
            .header(HttpHeaders.AUTHORIZATION, "Bearer garbage")
            .exchange()
            .expectStatus()
            .isBadRequest()
            .expectHeader()
            .contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
            .expectBody(String.class)
            .returnResult()
            .getResponseBody();

    assertThat(body).contains("\"type\":\"urn:problem-type:bad-request\"");
  }

  @Test
  void swaggerUiIsPublicWithoutToken() {
    restClient.get().uri("/swagger-ui/index.html").exchange().expectStatus().isOk();
  }

  @Test
  void registerIgnoresGarbageBearerAndCreatesAccount() {
    String email = "t018-" + UUID.randomUUID() + "@example.com";
    String normalised = email.toLowerCase(Locale.ROOT);
    createdEmails.add(normalised);

    restClient
        .post()
        .uri("/api/v1/auth/register")
        .header(HttpHeaders.AUTHORIZATION, "Bearer garbage")
        .contentType(MediaType.APPLICATION_JSON)
        .body(
            """
            {
              "name": "T018 Trainer",
              "email": "%s",
              "password": "Pikachu12"
            }
            """
                .formatted(email))
        .exchange()
        .expectStatus()
        .isCreated()
        .expectHeader()
        .contentTypeCompatibleWith(MediaType.APPLICATION_JSON);
  }

  private String mintExpiredToken(long userId) {
    Instant now = Instant.now();
    JwtClaimsSet claims =
        JwtClaimsSet.builder()
            .issuer(JwtConfiguration.ISSUER)
            .subject(Long.toString(userId))
            .issuedAt(now.minus(Duration.ofMinutes(65)))
            .expiresAt(now.minus(Duration.ofMinutes(5)))
            .build();
    return jwtEncoder
        .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
        .getTokenValue();
  }

  private static String tamperPayload(String compactJwt) {
    String[] parts = compactJwt.split("\\.");
    assertThat(parts).hasSize(3);
    char[] payload = parts[1].toCharArray();
    int index = payload.length / 2;
    payload[index] = payload[index] == 'A' ? 'B' : 'A';
    return parts[0] + "." + new String(payload) + "." + parts[2];
  }

  private static void assertNoLeak(String body) {
    assertThat(body)
        .doesNotContain("InvalidCredentialsException")
        .doesNotContain("DuplicateEmailException")
        .doesNotContain("AccessDeniedException")
        .doesNotContain("JwtException")
        .doesNotContain("tools.jackson")
        .doesNotContain("com.fasterxml")
        .doesNotContain("\tat ")
        .doesNotContain("stackTrace")
        .doesNotContain("https://");
  }
}
