package com.miguel.pokemonapp.web.support;

import com.miguel.pokemonapp.infrastructure.security.JwtAccessTokenIssuer;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;

/** Issues a Bearer token for the Flyway V5 demo user (`demo@pokefolio.dev`). */
public final class AuthTestSupport {

  public static final String DEMO_EMAIL = "demo@pokefolio.dev";

  private AuthTestSupport() {}

  public static String bearerForDemoUser(JwtAccessTokenIssuer issuer, JdbcTemplate jdbcTemplate) {
    Long id =
        jdbcTemplate.queryForObject(
            "SELECT id FROM app_user WHERE email = ?", Long.class, DEMO_EMAIL);
    if (id == null) {
      throw new IllegalStateException("demo user missing from app_user");
    }
    return "Bearer " + issuer.issue(id).accessToken();
  }

  public static void applyBearer(HttpHeaders headers, String bearerAuthorization) {
    headers.set(HttpHeaders.AUTHORIZATION, bearerAuthorization);
  }
}
