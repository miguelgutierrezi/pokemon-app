package com.miguel.pokemonapp.domain.user;

import static com.miguel.pokemonapp.domain.Require.nonNull;

public record PersonName(String value) {

  public static final int MIN_LENGTH = 1;
  public static final int MAX_LENGTH = 100;

  public PersonName {
    nonNull(value, "name");
    if (value.isBlank()) {
      throw new IllegalArgumentException("name must be non-blank");
    }
    if (!value.equals(value.trim())) {
      throw new IllegalArgumentException("name must be trimmed");
    }
    if (value.length() > MAX_LENGTH) {
      throw new IllegalArgumentException("name length must be <= " + MAX_LENGTH);
    }
  }

  public static PersonName of(String raw) {
    nonNull(raw, "name");
    return new PersonName(raw.trim());
  }
}
