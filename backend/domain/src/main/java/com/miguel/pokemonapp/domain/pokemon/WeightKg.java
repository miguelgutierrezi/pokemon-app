package com.miguel.pokemonapp.domain.pokemon;

import static com.miguel.pokemonapp.domain.Require.atLeast;

import java.math.BigDecimal;

public final class WeightKg {

  private WeightKg() {}

  public static BigDecimal fromHectograms(int hectograms) {
    atLeast(hectograms, 0, "hectograms");
    return BigDecimal.valueOf(hectograms, 1);
  }
}
