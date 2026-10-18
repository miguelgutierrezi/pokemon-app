package com.miguel.pokemonapp.domain.user;

import static com.miguel.pokemonapp.domain.Require.atLeast;
import static com.miguel.pokemonapp.domain.Require.nonBlank;
import static com.miguel.pokemonapp.domain.Require.nonNull;

import java.time.Instant;
import java.util.Optional;

public record UserAccount(
    Optional<Long> id, PersonName name, Email email, String passwordHash, Instant createdAt) {

  public UserAccount {
    id = nonNull(id, "id");
    name = nonNull(name, "name");
    email = nonNull(email, "email");
    passwordHash = nonBlank(passwordHash, "passwordHash");
    createdAt = nonNull(createdAt, "createdAt");
  }

  public static UserAccount register(
      String name, String email, String passwordHash, Instant createdAt) {
    return new UserAccount(
        Optional.empty(), PersonName.of(name), Email.of(email), passwordHash, createdAt);
  }

  public static UserAccount restore(
      long id, String name, String email, String passwordHash, Instant createdAt) {
    atLeast(id, 1L, "id");
    return new UserAccount(
        Optional.of(id), PersonName.of(name), Email.of(email), passwordHash, createdAt);
  }
}
