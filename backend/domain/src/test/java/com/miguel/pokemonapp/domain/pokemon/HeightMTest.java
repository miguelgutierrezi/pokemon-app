package com.miguel.pokemonapp.domain.pokemon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class HeightMTest {

  @Test
  void convertsDecimetresToMetresWithOneDecimal() {
    assertThat(HeightM.fromDecimetres(7)).isEqualTo(new BigDecimal("0.7"));
  }

  @Test
  void convertsZeroDecimetresToZeroMetres() {
    assertThat(HeightM.fromDecimetres(0)).isEqualTo(new BigDecimal("0.0"));
  }

  @Test
  void convertsExactTenthWithOneDecimalScale() {
    assertThat(HeightM.fromDecimetres(10)).isEqualTo(new BigDecimal("1.0"));
  }

  @Test
  void convertsOddDecimetresToSingleDecimal() {
    assertThat(HeightM.fromDecimetres(1)).isEqualTo(new BigDecimal("0.1"));
  }

  @Test
  void rejectsNegativeDecimetres() {
    assertThatThrownBy(() -> HeightM.fromDecimetres(-1))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
