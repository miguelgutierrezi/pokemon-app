package com.miguel.pokemonapp.web.auth;

import com.miguel.pokemonapp.application.auth.AuthSession;

public record UserResponse(long id, String name, String email) {

  static UserResponse from(AuthSession.User user) {
    return new UserResponse(user.id(), user.name(), user.email());
  }
}
