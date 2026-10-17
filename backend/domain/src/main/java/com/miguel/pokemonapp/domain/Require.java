package com.miguel.pokemonapp.domain;

public final class Require {

  private Require() {}

  public static <T> T nonNull(T value, String field) {
    if (value == null) {
      throw new IllegalArgumentException(field + " must not be null");
    }
    return value;
  }

  public static String nonBlank(String value, String field) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(field + " must be non-blank");
    }
    return value;
  }

  public static int atLeast(int value, int min, String field) {
    if (value < min) {
      throw new IllegalArgumentException(field + " must be >= " + min);
    }
    return value;
  }

  public static long atLeast(long value, long min, String field) {
    if (value < min) {
      throw new IllegalArgumentException(field + " must be >= " + min);
    }
    return value;
  }
}
