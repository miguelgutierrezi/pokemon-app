package com.miguel.pokemonapp.domain.user;

import java.util.Locale;
import java.util.regex.Pattern;

public record Email(String value) {

  public static final int MAX_LENGTH = 254;

  public static final String PATTERN =
      "^[a-z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?(?:\\.[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?)+$";

  private static final Pattern COMPILED = Pattern.compile(PATTERN);

  public Email {
    if (value == null || value.length() > MAX_LENGTH || !COMPILED.matcher(value).matches()) {
      throw new IllegalArgumentException("email is invalid");
    }
  }

  public static boolean isValid(String raw) {
    if (raw == null) {
      return false;
    }
    String normalised = raw.trim().toLowerCase(Locale.ROOT);
    if (normalised.isEmpty() || normalised.length() > MAX_LENGTH) {
      return false;
    }
    return COMPILED.matcher(normalised).matches();
  }

  public static Email of(String raw) {
    if (!isValid(raw)) {
      throw new IllegalArgumentException("email is invalid");
    }
    return new Email(raw.trim().toLowerCase(Locale.ROOT));
  }
}
