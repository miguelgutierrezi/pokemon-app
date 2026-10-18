package com.miguel.pokemonapp.web.config;

import static org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher.pathPattern;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.servlet.HandlerExceptionResolver;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

  @Bean
  BearerTokenResolver bearerTokenResolver(PublicRoutes publicRoutes) {
    BearerTokenResolver defaults = new DefaultBearerTokenResolver();
    return request -> {
      if (publicRoutes.matches(request)) {
        return null;
      }
      return defaults.resolve(request);
    };
  }

  @Bean
  AuthenticationEntryPoint authenticationEntryPoint(
      @Qualifier("handlerExceptionResolver") HandlerExceptionResolver exceptionResolver) {
    return (request, response, authException) -> {
      if (exceptionResolver.resolveException(request, response, null, authException) == null) {
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
      }
    };
  }

  @Bean
  AccessDeniedHandler accessDeniedHandler(
      @Qualifier("handlerExceptionResolver") HandlerExceptionResolver exceptionResolver) {
    return (request, response, accessDeniedException) -> {
      if (exceptionResolver.resolveException(request, response, null, accessDeniedException)
          == null) {
        response.sendError(HttpServletResponse.SC_FORBIDDEN);
      }
    };
  }

  @Bean
  SecurityFilterChain securityFilterChain(
      HttpSecurity http,
      JwtDecoder jwtDecoder,
      PublicRoutes publicRoutes,
      BearerTokenResolver bearerTokenResolver,
      AuthenticationEntryPoint authenticationEntryPoint,
      AccessDeniedHandler accessDeniedHandler)
      throws Exception {
    http.csrf(AbstractHttpConfigurer::disable)
        .httpBasic(AbstractHttpConfigurer::disable)
        .formLogin(AbstractHttpConfigurer::disable)
        .logout(AbstractHttpConfigurer::disable)
        .requestCache(cache -> cache.disable())
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth ->
                auth.dispatcherTypeMatchers(DispatcherType.ERROR)
                    .permitAll()
                    .requestMatchers(publicRoutes)
                    .permitAll()
                    .requestMatchers(pathPattern(HttpMethod.POST, "/api/v1/collection"))
                    .authenticated()
                    .requestMatchers(pathPattern(HttpMethod.PUT, "/api/v1/collection/*"))
                    .authenticated()
                    .requestMatchers(pathPattern(HttpMethod.DELETE, "/api/v1/collection/*"))
                    .authenticated()
                    .anyRequest()
                    .denyAll())
        .oauth2ResourceServer(
            oauth2 ->
                oauth2
                    .bearerTokenResolver(bearerTokenResolver)
                    .authenticationEntryPoint(authenticationEntryPoint)
                    .accessDeniedHandler(accessDeniedHandler)
                    .jwt(jwt -> jwt.decoder(jwtDecoder)))
        .exceptionHandling(
            exceptions ->
                exceptions
                    .authenticationEntryPoint(authenticationEntryPoint)
                    .accessDeniedHandler(accessDeniedHandler));
    return http.build();
  }
}
