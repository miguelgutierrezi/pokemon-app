package com.miguel.pokemonapp.web.auth;

import com.miguel.pokemonapp.application.auth.AuthSession;

public record AuthResponse(
    String accessToken, String tokenType, long expiresIn, UserResponse user) {

  static AuthResponse from(AuthSession session) {
    return new AuthResponse(
        session.accessToken(),
        session.tokenType(),
        session.expiresIn(),
        UserResponse.from(session.user()));
  }
}
