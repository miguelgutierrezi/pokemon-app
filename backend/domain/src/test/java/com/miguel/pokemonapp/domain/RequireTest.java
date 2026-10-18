package com.miguel.pokemonapp.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class RequireTest {

  @Test
  void nonNullReturnsValueWhenPresent() {
    assertThat(Require.nonNull("x", "field")).isEqualTo("x");
  }

  @Test
  void nonNullRejectsNull() {
    assertThatThrownBy(() -> Require.nonNull(null, "weightKg"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("weightKg must not be null");
  }

  @Test
  void nonBlankReturnsValueWhenPresent() {
    assertThat(Require.nonBlank("Bulbasaur", "name")).isEqualTo("Bulbasaur");
  }

  @Test
  void nonBlankRejectsNull() {
    assertThatThrownBy(() -> Require.nonBlank(null, "slug"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("slug must be non-blank");
  }

  @Test
  void nonBlankRejectsBlank() {
    assertThatThrownBy(() -> Require.nonBlank("  ", "name"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("name must be non-blank");
  }

  @Test
  void nonBlankRejectsEmpty() {
    assertThatThrownBy(() -> Require.nonBlank("", "generation"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("generation must be non-blank");
  }

  @Test
  void atLeastReturnsValueWhenEqualToMin() {
    assertThat(Require.atLeast(1, 1, "nationalNumber")).isEqualTo(1);
  }

  @Test
  void atLeastReturnsValueWhenAboveMin() {
    assertThat(Require.atLeast(5, 1, "page")).isEqualTo(5);
  }

  @Test
  void atLeastRejectsBelowMin() {
    assertThatThrownBy(() -> Require.atLeast(0, 1, "nationalNumber"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("nationalNumber must be >= 1");
  }

  @Test
  void atLeastRejectsBelowZeroMin() {
    assertThatThrownBy(() -> Require.atLeast(-1, 0, "hectograms"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("hectograms must be >= 0");
  }

  @Test
  void atLeastLongReturnsValueWhenEqualToMin() {
    assertThat(Require.atLeast(0L, 0L, "version")).isEqualTo(0L);
  }

  @Test
  void atLeastLongReturnsValueWhenAboveMin() {
    assertThat(Require.atLeast(3L, 0L, "version")).isEqualTo(3L);
  }

  @Test
  void atLeastLongRejectsBelowMin() {
    assertThatThrownBy(() -> Require.atLeast(-1L, 0L, "version"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("version must be >= 0");
  }
}
