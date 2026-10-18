package com.miguel.pokemonapp.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.miguel.pokemonapp.application.port.IssuedAccessToken;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

class JwtAccessTokenIssuerTest {

  private static final String ISSUER = "pokefolio";
  private static final Duration EXPIRY = Duration.ofHours(1);
  private static final byte[] SECRET =
      "test-jwt-secret-key-at-least-32b!".getBytes(StandardCharsets.UTF_8);
  private static final byte[] OTHER_SECRET =
      "other-jwt-secret-key-at-least-32b".getBytes(StandardCharsets.UTF_8);

  private Clock clock;
  private NimbusJwtEncoder encoder;
  private NimbusJwtDecoder decoder;
  private JwtAccessTokenIssuer issuer;

  @BeforeEach
  void setUp() {
    clock = Clock.fixed(Instant.now().truncatedTo(ChronoUnit.SECONDS), ZoneOffset.UTC);
    SecretKey key = new SecretKeySpec(SECRET, "HmacSHA256");
    encoder = NimbusJwtEncoder.withSecretKey(key).algorithm(MacAlgorithm.HS256).build();
    decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
    issuer = new JwtAccessTokenIssuer(encoder, ISSUER, EXPIRY, clock);
  }

  @Test
  void issueProducesDecodableTokenWithExpectedClaimsAndExpiry() {
    IssuedAccessToken issued = issuer.issue(7L);

    Jwt jwt = decoder.decode(issued.accessToken());

    Instant iat = clock.instant();
    assertThat(jwt.getClaimAsString("iss")).isEqualTo(ISSUER);
    assertThat(jwt.getSubject()).isEqualTo("7");
    assertThat(jwt.getIssuedAt()).isEqualTo(iat);
    assertThat(jwt.getExpiresAt()).isEqualTo(iat.plus(EXPIRY));
    assertThat(jwt.getHeaders().get("alg")).isEqualTo(MacAlgorithm.HS256.getName());
    assertThat(issued.expiresInSeconds()).isEqualTo(EXPIRY.toSeconds());
  }

  @Test
  void tokenSignedWithDifferentKeyFailsToDecode() {
    IssuedAccessToken issued = issuer.issue(7L);

    SecretKey otherKey = new SecretKeySpec(OTHER_SECRET, "HmacSHA256");
    NimbusJwtDecoder otherDecoder =
        NimbusJwtDecoder.withSecretKey(otherKey).macAlgorithm(MacAlgorithm.HS256).build();

    assertThatThrownBy(() -> otherDecoder.decode(issued.accessToken()))
        .isInstanceOf(JwtException.class);
  }

  @Test
  void constructorRejectsNullEncoder() {
    assertThatThrownBy(() -> new JwtAccessTokenIssuer(null, ISSUER, EXPIRY, clock))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void constructorRejectsNullIssuer() {
    assertThatThrownBy(() -> new JwtAccessTokenIssuer(encoder, null, EXPIRY, clock))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void constructorRejectsBlankIssuer() {
    assertThatThrownBy(() -> new JwtAccessTokenIssuer(encoder, "  ", EXPIRY, clock))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void constructorRejectsNullClock() {
    assertThatThrownBy(() -> new JwtAccessTokenIssuer(encoder, ISSUER, EXPIRY, null))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void constructorRejectsNonPositiveExpiry() {
    assertThatThrownBy(() -> new JwtAccessTokenIssuer(encoder, ISSUER, Duration.ZERO, clock))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () -> new JwtAccessTokenIssuer(encoder, ISSUER, Duration.ofSeconds(-1), clock))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
