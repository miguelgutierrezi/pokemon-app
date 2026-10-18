package com.miguel.pokemonapp.web.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.miguel.pokemonapp.application.port.IssuedAccessToken;
import com.miguel.pokemonapp.infrastructure.security.JwtAccessTokenIssuer;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;

class JwtPropertiesValidationTest {

  private static final String VALID_SECRET = "pokemonapp-test-jwt-secret-not-for-prod!!";

  private final ApplicationContextRunner runner =
      new ApplicationContextRunner()
          .withUserConfiguration(JwtConfiguration.class, TestClockConfiguration.class);

  @Test
  void toStringMasksSecret() {
    JwtProperties properties = new JwtProperties(VALID_SECRET, Duration.ofHours(1));

    assertThat(properties.toString())
        .isEqualTo("JwtProperties[secret=****, expiry=PT1H]")
        .doesNotContain(VALID_SECRET);
  }

  @Test
  void missingSecretFailsStartupWithoutEchoingSecret() {
    // Empty value mirrors ${JWT_SECRET:} when unset. Override the surefire test secret.
    runner
        .withPropertyValues("pokemonapp.security.jwt.secret=")
        .run(
            context -> {
              assertThat(context).hasFailed();
              List<String> messages = failureMessages(context.getStartupFailure());
              assertThat(messages).anyMatch(m -> m.contains("JWT_SECRET"));
            });
  }

  @Test
  void secretOfThirtyOneUtf8BytesFailsStartupWithoutEchoingSecret() {
    String secret31 = "a".repeat(31);
    assertThat(secret31.getBytes(StandardCharsets.UTF_8)).hasSize(31);

    runner
        .withPropertyValues("pokemonapp.security.jwt.secret=" + secret31)
        .run(
            context -> {
              assertThat(context).hasFailed();
              List<String> messages = failureMessages(context.getStartupFailure());
              assertThat(messages).anyMatch(m -> m.contains("JWT_SECRET"));
              assertThat(String.join("\n", messages)).doesNotContain(secret31);
            });
  }

  @Test
  void multibyteSecretValidatedByUtf8ByteLength() {
    String sixteenN = "ñ".repeat(16); // 32 UTF-8 bytes, 16 chars
    String fifteenN = "ñ".repeat(15); // 30 UTF-8 bytes
    assertThat(sixteenN.getBytes(StandardCharsets.UTF_8)).hasSize(32);
    assertThat(fifteenN.getBytes(StandardCharsets.UTF_8)).hasSize(30);

    runner
        .withPropertyValues("pokemonapp.security.jwt.secret=" + fifteenN)
        .run(
            context -> {
              assertThat(context).hasFailed();
              List<String> messages = failureMessages(context.getStartupFailure());
              assertThat(messages).anyMatch(m -> m.contains("JWT_SECRET"));
              assertThat(String.join("\n", messages)).doesNotContain(fifteenN);
            });

    runner
        .withPropertyValues("pokemonapp.security.jwt.secret=" + sixteenN)
        .run(
            context -> {
              assertThat(context).hasNotFailed();
              assertThat(context).hasSingleBean(SecretKey.class);
              assertThat(context).hasSingleBean(JwtEncoder.class);
              assertThat(context).hasSingleBean(JwtDecoder.class);
            });
  }

  @Test
  void validSecretStartsWithJwtBeansAndDefaultExpiryOneHour() {
    runner
        .withPropertyValues("pokemonapp.security.jwt.secret=" + VALID_SECRET)
        .run(
            context -> {
              assertThat(context).hasNotFailed();
              assertThat(context).hasSingleBean(SecretKey.class);
              assertThat(context).hasSingleBean(JwtEncoder.class);
              assertThat(context).hasSingleBean(JwtDecoder.class);
              assertThat(context).hasSingleBean(JwtAccessTokenIssuer.class);
              assertThat(context.getBean(JwtProperties.class).expiry())
                  .isEqualTo(Duration.ofHours(1));
            });
  }

  @Test
  void issuedTokenRoundTripsAndWrongIssuerIsRejected() {
    runner
        .withPropertyValues("pokemonapp.security.jwt.secret=" + VALID_SECRET)
        .run(
            context -> {
              assertThat(context).hasNotFailed();
              JwtAccessTokenIssuer issuer = context.getBean(JwtAccessTokenIssuer.class);
              JwtDecoder decoder = context.getBean(JwtDecoder.class);
              JwtEncoder encoder = context.getBean(JwtEncoder.class);

              IssuedAccessToken issued = issuer.issue(7L);
              assertThat(decoder.decode(issued.accessToken()).getSubject()).isEqualTo("7");

              Instant now = context.getBean(Clock.class).instant();
              String wrongIss =
                  encoder
                      .encode(
                          JwtEncoderParameters.from(
                              JwsHeader.with(MacAlgorithm.HS256).build(),
                              JwtClaimsSet.builder()
                                  .issuer("not-pokefolio")
                                  .subject("7")
                                  .issuedAt(now)
                                  .expiresAt(now.plus(Duration.ofHours(1)))
                                  .build()))
                      .getTokenValue();

              assertThatThrownBy(() -> decoder.decode(wrongIss)).isInstanceOf(JwtException.class);
            });
  }

  private static List<String> failureMessages(Throwable failure) {
    List<String> messages = new ArrayList<>();
    for (Throwable t = failure; t != null; t = t.getCause()) {
      if (t.getMessage() != null) {
        messages.add(t.getMessage());
      }
    }
    return messages;
  }

  @Configuration
  static class TestClockConfiguration {
    @Bean
    Clock clock() {
      return Clock.fixed(Instant.now().truncatedTo(ChronoUnit.SECONDS), ZoneOffset.UTC);
    }
  }
}
