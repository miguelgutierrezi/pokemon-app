package com.miguel.pokemonapp.web.config;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("pokemonapp.security.jwt")
public record JwtProperties(String secret, Duration expiry) {

  private static final int MIN_SECRET_UTF8_BYTES = 32;

  public JwtProperties {
    if (secret == null || secret.isBlank()) {
      throw new IllegalArgumentException(
          "JWT_SECRET must be set (pokemonapp.security.jwt.secret); no default is provided");
    }
    if (secret.getBytes(UTF_8).length < MIN_SECRET_UTF8_BYTES) {
      throw new IllegalArgumentException(
          "JWT_SECRET must be at least " + MIN_SECRET_UTF8_BYTES + " UTF-8 bytes");
    }
    if (expiry == null) {
      expiry = Duration.ofHours(1);
    }
    if (expiry.isZero() || expiry.isNegative()) {
      throw new IllegalArgumentException("pokemonapp.security.jwt.expiry must be positive");
    }
  }

  @Override
  public String toString() {
    return "JwtProperties[secret=****, expiry=" + expiry + "]";
  }
}
