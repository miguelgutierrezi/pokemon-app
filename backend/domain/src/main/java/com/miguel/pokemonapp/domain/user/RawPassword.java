package com.miguel.pokemonapp.domain.user;

import static com.miguel.pokemonapp.domain.Require.nonNull;
import static java.nio.charset.StandardCharsets.UTF_8;

public record RawPassword(String value) {

  public static final int MIN_LENGTH = 8;
  public static final int MAX_UTF8_BYTES = 72;

  public RawPassword {
    nonNull(value, "password");
    if (value.length() < MIN_LENGTH) {
      throw new IllegalArgumentException("password length must be >= " + MIN_LENGTH);
    }
    if (value.getBytes(UTF_8).length > MAX_UTF8_BYTES) {
      throw new IllegalArgumentException(
          "password UTF-8 length must be <= " + MAX_UTF8_BYTES + " bytes");
    }
  }

  public static RawPassword of(String raw) {
    return new RawPassword(raw);
  }
}
