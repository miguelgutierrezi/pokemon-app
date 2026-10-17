package com.miguel.pokemonapp.domain.pokemon;

import static com.miguel.pokemonapp.domain.Require.atLeast;

import java.math.BigDecimal;

public final class HeightM {

  private HeightM() {}

  public static BigDecimal fromDecimetres(int decimetres) {
    atLeast(decimetres, 0, "decimetres");
    return BigDecimal.valueOf(decimetres, 1);
  }
}
