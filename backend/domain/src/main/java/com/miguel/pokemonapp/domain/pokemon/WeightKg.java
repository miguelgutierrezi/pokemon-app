package com.miguel.pokemonapp.domain.pokemon;

import java.math.BigDecimal;

public final class WeightKg {

  private WeightKg() {}

  public static BigDecimal fromHectograms(int hectograms) {
    if (hectograms < 0) {
      throw new IllegalArgumentException("hectograms must be non-negative");
    }
    return BigDecimal.valueOf(hectograms, 1);
  }
}
