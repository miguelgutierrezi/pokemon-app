package com.miguel.pokemonapp.application.auth;

import com.miguel.pokemonapp.application.port.IssuedAccessToken;
import com.miguel.pokemonapp.domain.user.UserAccount;

public record AuthSession(String accessToken, String tokenType, long expiresIn, User user) {

  public static final String BEARER = "Bearer";

  public record User(long id, String name, String email) {}

  static long requirePersistedId(UserAccount account) {
    return account
        .id()
        .orElseThrow(() -> new IllegalStateException("persisted account must have an id"));
  }

  public static AuthSession of(UserAccount account, IssuedAccessToken token) {
    long id = requirePersistedId(account);
    return new AuthSession(
        token.accessToken(),
        BEARER,
        token.expiresInSeconds(),
        new User(id, account.name().value(), account.email().value()));
  }
}
