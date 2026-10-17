package com.miguel.pokemonapp.domain.pokemon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class WeightKgTest {

  @Test
  void convertsHectogramsToKilogramsWithOneDecimal() {
    assertThat(WeightKg.fromHectograms(69)).isEqualTo(new BigDecimal("6.9"));
  }

  @Test
  void convertsZeroHectogramsToZeroKilograms() {
    assertThat(WeightKg.fromHectograms(0)).isEqualTo(new BigDecimal("0.0"));
  }

  @Test
  void convertsExactTenthWithOneDecimalScale() {
    assertThat(WeightKg.fromHectograms(100)).isEqualTo(new BigDecimal("10.0"));
  }

  @Test
  void convertsOddHectogramsToSingleDecimal() {
    assertThat(WeightKg.fromHectograms(1)).isEqualTo(new BigDecimal("0.1"));
  }

  @Test
  void rejectsNegativeHectograms() {
    assertThatThrownBy(() -> WeightKg.fromHectograms(-1))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
