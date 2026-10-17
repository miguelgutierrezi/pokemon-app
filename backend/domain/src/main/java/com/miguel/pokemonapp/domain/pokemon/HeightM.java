package com.miguel.pokemonapp.domain.pokemon;

import java.math.BigDecimal;

public final class HeightM {

  private HeightM() {}

  public static BigDecimal fromDecimetres(int decimetres) {
    if (decimetres < 0) {
      throw new IllegalArgumentException("decimetres must be non-negative");
    }
    return BigDecimal.valueOf(decimetres, 1);
  }
}
