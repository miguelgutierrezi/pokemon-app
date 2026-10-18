package com.miguel.pokemonapp.web.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.miguel.pokemonapp.support.PostgresTestcontainersConfiguration;
import com.miguel.pokemonapp.web.collection.CollectionSeedReset;
import com.miguel.pokemonapp.web.config.JwtConfiguration;
import com.miguel.pokemonapp.web.support.AuthTestSupport;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
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

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
@Import(PostgresTestcontainersConfiguration.class)
class AuthIntegrationTest {

  private static final String DEMO_EMAIL = AuthTestSupport.DEMO_EMAIL;
  private static final String DEMO_PASSWORD = "Demo-Passw0rd!";
  private static final String VALID_PASSWORD = "Pikachu12";
  private static final String INVALID_CREDENTIALS_DETAIL = "Invalid email or password";

  @Autowired private RestTestClient restClient;
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private JwtEncoder jwtEncoder;

  private final List<String> createdEmails = new ArrayList<>();

  @BeforeEach
  void setUp() {
    CollectionSeedReset.restore(jdbcTemplate);
    createdEmails.clear();
  }

  @AfterEach
  void tearDownCreatedUsers() {
    for (String email : createdEmails) {
      jdbcTemplate.update("DELETE FROM app_user WHERE email = ?", email);
    }
    createdEmails.clear();
  }

  @Test
  void registerTokenAllowsProtectedDuplicatePostAndSuccessfulPut() {
    String email = uniqueEmail("trainer");
    String token = registerAndTrack(email, "Ash", VALID_PASSWORD);

    String conflictBody =
        restClient
            .post()
            .uri("/api/v1/collection")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
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

    assertThat(conflictBody).contains("\"type\":\"urn:problem-type:conflict\"");
    assertNoSecretOrFrameworkLeak(conflictBody);

    Integer version =
        jdbcTemplate.queryForObject(
            "SELECT version FROM pokemon_record WHERE national_number = 1", Integer.class);

    String putBody =
        restClient
            .put()
            .uri("/api/v1/collection/1")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .body(
                """
                {
                  "localizedName": "Auth IT Bulba",
                  "location": "Auth IT garden",
                  "tags": ["auth-it"],
                  "version": %d
                }
                """
                    .formatted(version))
            .exchange()
            .expectStatus()
            .isOk()
            .expectHeader()
            .contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
            .expectBody(String.class)
            .returnResult()
            .getResponseBody();

    assertThat(putBody)
        .contains("\"localizedName\":\"Auth IT Bulba\"")
        .contains("\"version\":" + (version + 1));
    assertNoSecretOrFrameworkLeak(putBody);
  }

  @Test
  void registerWithoutTokenOnProtectedWriteReturnsUnauthorized() {
    String body =
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
            .expectBody(String.class)
            .returnResult()
            .getResponseBody();

    assertThat(body).contains("\"type\":\"urn:problem-type:unauthorized\"");
    assertNoSecretOrFrameworkLeak(body);
  }

  @Test
  void registerNormalisesEmailCasePaddingAndRejectsDuplicateWithoutEchoingEmail() {
    String uuid = UUID.randomUUID().toString();
    String mixed = "Ash-" + uuid + "@Example.com";
    String normalised = mixed.toLowerCase(Locale.ROOT);
    String duplicate = "ASH-" + uuid + "@example.com";
    String padded = "  ash-" + uuid + "@example.com  ";

    String registerBody =
        restClient
            .post()
            .uri("/api/v1/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .body(registerJson("Ash", mixed, VALID_PASSWORD))
            .exchange()
            .expectStatus()
            .isCreated()
            .expectHeader()
            .contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
            .expectBody(String.class)
            .returnResult()
            .getResponseBody();

    trackEmail(normalised);
    assertThat(registerBody).contains("\"email\":\"" + normalised + "\"");
    assertNoSecretOrFrameworkLeak(registerBody);

    String conflictBody =
        restClient
            .post()
            .uri("/api/v1/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .body(registerJson("Ash", duplicate, VALID_PASSWORD))
            .exchange()
            .expectStatus()
            .isEqualTo(409)
            .expectHeader()
            .contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
            .expectBody(String.class)
            .returnResult()
            .getResponseBody();

    assertThat(conflictBody)
        .contains("\"type\":\"urn:problem-type:conflict\"")
        .doesNotContain(duplicate)
        .doesNotContain(normalised)
        .doesNotContain(mixed)
        .doesNotContain(uuid);
    assertNoSecretOrFrameworkLeak(conflictBody);

    // Same local-part after trim/lower — already registered, so padded form is also 409.
    // Spec: a distinct padded email that normalises to a *new* address yields 201.
    String paddedUnique = "  pad-" + UUID.randomUUID() + "@Example.com  ";
    String paddedNormalised = paddedUnique.trim().toLowerCase(Locale.ROOT);
    String paddedBody =
        restClient
            .post()
            .uri("/api/v1/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .body(registerJson("Misty", paddedUnique, VALID_PASSWORD))
            .exchange()
            .expectStatus()
            .isCreated()
            .expectBody(String.class)
            .returnResult()
            .getResponseBody();

    trackEmail(paddedNormalised);
    assertThat(paddedBody).contains("\"email\":\"" + paddedNormalised + "\"");
    assertThat(paddedBody).doesNotContain(paddedUnique);
    assertNoSecretOrFrameworkLeak(paddedBody);

    // Also confirm padded variant of the first email is treated as the same account (409).
    String paddedDuplicateBody =
        restClient
            .post()
            .uri("/api/v1/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .body(registerJson("Ash", padded, VALID_PASSWORD))
            .exchange()
            .expectStatus()
            .isEqualTo(409)
            .expectBody(String.class)
            .returnResult()
            .getResponseBody();
    assertThat(paddedDuplicateBody).doesNotContain(normalised).doesNotContain(padded.trim());
  }

  @Test
  void registerPasswordOfFortyNTildeReturnsBadRequestNamingPassword() {
    String email = uniqueEmail("ntilde");
    String body =
        restClient
            .post()
            .uri("/api/v1/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .body(registerJson("Ash", email, "ñ".repeat(40)))
            .exchange()
            .expectStatus()
            .isBadRequest()
            .expectHeader()
            .contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
            .expectBody(String.class)
            .returnResult()
            .getResponseBody();

    assertThat(body).containsIgnoringCase("password");
    assertNoHashOrFrameworkLeak(body);
    assertThat(countUsers(email.toLowerCase(Locale.ROOT))).isZero();
  }

  @Test
  void demoLoginReturnsNoStoreTokenThatWorksOnProtectedWrite() {
    var result =
        restClient
            .post()
            .uri("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .body(loginJson(DEMO_EMAIL, DEMO_PASSWORD))
            .exchange()
            .expectStatus()
            .isOk()
            .expectHeader()
            .contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
            .expectHeader()
            .value(HttpHeaders.CACHE_CONTROL, value -> assertThat(value).contains("no-store"))
            .expectBody(String.class)
            .returnResult();

    String body = result.getResponseBody();
    assertThat(body)
        .contains("\"accessToken\"")
        .contains("\"tokenType\":\"Bearer\"")
        .contains("\"email\":\"" + DEMO_EMAIL + "\"");
    assertNoSecretOrFrameworkLeak(body);

    String token = extractAccessToken(body);
    restClient
        .post()
        .uri("/api/v1/collection")
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
        .contentType(MediaType.APPLICATION_JSON)
        .body("{\"nationalNumber\":1}")
        .exchange()
        .expectStatus()
        .isEqualTo(409);
  }

  @Test
  void loginFailuresShareIdenticalInvalidCredentialsDetail() {
    String oversize = "ñ".repeat(40);
    assertIdenticalUnauthorizedLogin(DEMO_EMAIL, "wrong-password");
    assertIdenticalUnauthorizedLogin(
        "unknown-" + UUID.randomUUID() + "@example.com", VALID_PASSWORD);
    assertIdenticalUnauthorizedLogin("not-an-email", VALID_PASSWORD);
    assertIdenticalUnauthorizedLogin(DEMO_EMAIL, oversize);
  }

  @Test
  void concurrentRegisterSameEmailYieldsOneCreatedAndOneConflict() throws Exception {
    String email = uniqueEmail("race");
    String normalised = email.toLowerCase(Locale.ROOT);
    CountDownLatch ready = new CountDownLatch(2);
    CountDownLatch start = new CountDownLatch(1);
    AtomicInteger created = new AtomicInteger();
    AtomicInteger conflict = new AtomicInteger();
    AtomicInteger other = new AtomicInteger();

    try (ExecutorService pool = Executors.newFixedThreadPool(2)) {
      Future<?> first =
          pool.submit(() -> raceRegister(email, ready, start, created, conflict, other));
      Future<?> second =
          pool.submit(() -> raceRegister(email, ready, start, created, conflict, other));

      assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
      start.countDown();
      first.get(30, TimeUnit.SECONDS);
      second.get(30, TimeUnit.SECONDS);
    }

    trackEmail(normalised);
    assertThat(created.get()).isEqualTo(1);
    assertThat(conflict.get()).isEqualTo(1);
    assertThat(other.get()).isZero();
    assertThat(countUsers(normalised)).isEqualTo(1L);
  }

  @Test
  void collectionSeedResetLeavesDemoUserIntact() {
    CollectionSeedReset.restore(jdbcTemplate);

    restClient
        .post()
        .uri("/api/v1/auth/login")
        .contentType(MediaType.APPLICATION_JSON)
        .body(loginJson(DEMO_EMAIL, DEMO_PASSWORD))
        .exchange()
        .expectStatus()
        .isOk()
        .expectHeader()
        .value(HttpHeaders.CACHE_CONTROL, value -> assertThat(value).contains("no-store"));
  }

  @Test
  void expiredAndTamperedBearerOnProtectedWriteReturnUnauthorizedWhilePublicGetStaysOk() {
    long demoUserId =
        jdbcTemplate.queryForObject(
            "SELECT id FROM app_user WHERE email = ?", Long.class, DEMO_EMAIL);
    String expired = mintExpiredToken(demoUserId);
    String loginBody =
        restClient
            .post()
            .uri("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .body(loginJson(DEMO_EMAIL, DEMO_PASSWORD))
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(String.class)
            .returnResult()
            .getResponseBody();
    String tampered = tamperPayload(extractAccessToken(loginBody));

    String expiredBody =
        restClient
            .post()
            .uri("/api/v1/collection")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + expired)
            .contentType(MediaType.APPLICATION_JSON)
            .body("{\"nationalNumber\":1}")
            .exchange()
            .expectStatus()
            .isUnauthorized()
            .expectBody(String.class)
            .returnResult()
            .getResponseBody();
    assertThat(expiredBody).contains("\"type\":\"urn:problem-type:unauthorized\"");
    assertNoSecretOrFrameworkLeak(expiredBody);

    String tamperedBody =
        restClient
            .put()
            .uri("/api/v1/collection/1")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tampered)
            .contentType(MediaType.APPLICATION_JSON)
            .body(
                """
                {
                  "localizedName": "x",
                  "location": "y",
                  "tags": [],
                  "version": 0
                }
                """)
            .exchange()
            .expectStatus()
            .isUnauthorized()
            .expectBody(String.class)
            .returnResult()
            .getResponseBody();
    assertThat(tamperedBody).contains("\"type\":\"urn:problem-type:unauthorized\"");
    assertNoSecretOrFrameworkLeak(tamperedBody);

    restClient
        .get()
        .uri("/api/v1/collection")
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + expired)
        .exchange()
        .expectStatus()
        .isOk();
  }

  private void raceRegister(
      String email,
      CountDownLatch ready,
      CountDownLatch start,
      AtomicInteger created,
      AtomicInteger conflict,
      AtomicInteger other) {
    try {
      ready.countDown();
      assertThat(start.await(10, TimeUnit.SECONDS)).isTrue();
      var result =
          restClient
              .post()
              .uri("/api/v1/auth/register")
              .contentType(MediaType.APPLICATION_JSON)
              .body(registerJson("Race", email, VALID_PASSWORD))
              .exchange()
              .returnResult(String.class);
      int status = result.getStatus().value();
      String body = result.getResponseBody();
      if (status == 201) {
        created.incrementAndGet();
      } else if (status == 409) {
        conflict.incrementAndGet();
      } else {
        other.incrementAndGet();
      }
      assertThat(status).isNotEqualTo(500);
      assertNoSecretOrFrameworkLeak(body);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      other.incrementAndGet();
    }
  }

  private void assertIdenticalUnauthorizedLogin(String email, String password) {
    String body =
        restClient
            .post()
            .uri("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .body(loginJson(email, password))
            .exchange()
            .expectStatus()
            .isUnauthorized()
            .expectHeader()
            .contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
            .expectBody(String.class)
            .returnResult()
            .getResponseBody();

    assertThat(body)
        .contains("\"type\":\"urn:problem-type:unauthorized\"")
        .contains("\"detail\":\"" + INVALID_CREDENTIALS_DETAIL + "\"");
    assertNoSecretOrFrameworkLeak(body);
    assertThat(body).doesNotContain(password);
  }

  private String registerAndTrack(String email, String name, String password) {
    String body =
        restClient
            .post()
            .uri("/api/v1/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .body(registerJson(name, email, password))
            .exchange()
            .expectStatus()
            .isCreated()
            .expectHeader()
            .contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
            .expectHeader()
            .value(HttpHeaders.CACHE_CONTROL, value -> assertThat(value).contains("no-store"))
            .expectBody(String.class)
            .returnResult()
            .getResponseBody();

    trackEmail(email.toLowerCase(Locale.ROOT));
    assertNoSecretOrFrameworkLeak(body);
    return extractAccessToken(body);
  }

  private void trackEmail(String normalisedEmail) {
    createdEmails.add(normalisedEmail);
  }

  private String uniqueEmail(String prefix) {
    return prefix + "-" + UUID.randomUUID() + "@example.com";
  }

  private Long countUsers(String email) {
    return jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM app_user WHERE email = ?", Long.class, email);
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

  private static String extractAccessToken(String body) {
    String marker = "\"accessToken\":\"";
    int start = body.indexOf(marker);
    assertThat(start).isGreaterThanOrEqualTo(0);
    int from = start + marker.length();
    int end = body.indexOf('"', from);
    assertThat(end).isGreaterThan(from);
    return body.substring(from, end);
  }

  private static String registerJson(String name, String email, String password) {
    return """
        {
          "name": %s,
          "email": %s,
          "password": %s
        }
        """
        .formatted(jsonString(name), jsonString(email), jsonString(password));
  }

  private static String loginJson(String email, String password) {
    return """
        {
          "email": %s,
          "password": %s
        }
        """
        .formatted(jsonString(email), jsonString(password));
  }

  private static String jsonString(String value) {
    return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
  }

  private static void assertNoSecretOrFrameworkLeak(String body) {
    // Ban JSON property / hash material; allow the public detail "Invalid email or password".
    assertThat(body)
        .doesNotContain("\"password\"")
        .doesNotContain("passwordHash")
        .doesNotContain("$2");
    assertNoHashOrFrameworkLeak(body);
  }

  private static void assertNoHashOrFrameworkLeak(String body) {
    assertThat(body)
        .doesNotContain("passwordHash")
        .doesNotContain("$2")
        .doesNotContain("InvalidCredentialsException")
        .doesNotContain("DuplicateEmailException")
        .doesNotContain("AccessDeniedException")
        .doesNotContain("JwtException")
        .doesNotContain("tools.jackson")
        .doesNotContain("com.fasterxml")
        .doesNotContain("\tat ")
        .doesNotContain("stackTrace");
  }
}
