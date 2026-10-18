package com.miguel.pokemonapp.infrastructure.security;

import com.miguel.pokemonapp.application.port.AccessTokenIssuer;
import com.miguel.pokemonapp.application.port.IssuedAccessToken;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

public final class JwtAccessTokenIssuer implements AccessTokenIssuer {

  private final JwtEncoder encoder;
  private final String issuer;
  private final Duration expiry;
  private final Clock clock;

  public JwtAccessTokenIssuer(JwtEncoder encoder, String issuer, Duration expiry, Clock clock) {
    this.encoder = Objects.requireNonNull(encoder, "encoder");
    this.issuer = Objects.requireNonNull(issuer, "issuer");
    if (this.issuer.isBlank()) {
      throw new IllegalArgumentException("issuer must be non-blank");
    }
    this.expiry = Objects.requireNonNull(expiry, "expiry");
    if (this.expiry.isZero() || this.expiry.isNegative()) {
      throw new IllegalArgumentException("expiry must be positive");
    }
    this.clock = Objects.requireNonNull(clock, "clock");
  }

  @Override
  public IssuedAccessToken issue(long userId) {
    Instant iat = clock.instant();
    Instant exp = iat.plus(expiry);
    JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
    JwtClaimsSet claims =
        JwtClaimsSet.builder()
            .issuer(issuer)
            .subject(Long.toString(userId))
            .issuedAt(iat)
            .expiresAt(exp)
            .build();
    String token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    return new IssuedAccessToken(token, expiry.toSeconds());
  }
}
