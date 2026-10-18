package com.miguel.pokemonapp.web.config;

import static org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher.pathPattern;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;

/**
 * Shared METHOD+PATH public matchers — single source of truth for {@code authorizeHttpRequests}
 * permitAll and the custom {@code BearerTokenResolver} (ignore Authorization on public routes).
 */
@Component
public final class PublicRoutes implements RequestMatcher {

  private final RequestMatcher delegate;

  public PublicRoutes() {
    List<RequestMatcher> matchers =
        List.of(
            pathPattern(HttpMethod.GET, "/api/v1/pokemon/**"),
            pathPattern(HttpMethod.GET, "/api/v1/collection/**"),
            pathPattern(HttpMethod.POST, "/api/v1/auth/register"),
            pathPattern(HttpMethod.POST, "/api/v1/auth/login"),
            pathPattern(HttpMethod.GET, "/actuator/health/**"),
            pathPattern("/v3/api-docs/**"),
            pathPattern("/swagger-ui/**"),
            pathPattern("/swagger-ui.html"));
    this.delegate = new OrRequestMatcher(matchers);
  }

  @Override
  public boolean matches(HttpServletRequest request) {
    return delegate.matches(request);
  }
}
