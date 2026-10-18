package com.miguel.pokemonapp.domain.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class PersonNameTest {

  @Test
  void exposesMinAndMaxLengthConstants() {
    assertThat(PersonName.MIN_LENGTH).isEqualTo(1);
    assertThat(PersonName.MAX_LENGTH).isEqualTo(100);
  }

  @Test
  void ofTrimsSurroundingWhitespace() {
    assertThat(PersonName.of("  Ash Ketchum  ").value()).isEqualTo("Ash Ketchum");
  }

  @Test
  void ofAcceptsLengthAtBoundariesAfterTrim() {
    assertThat(PersonName.of("A").value()).isEqualTo("A");
    assertThat(PersonName.of("x".repeat(PersonName.MAX_LENGTH)).value())
        .hasSize(PersonName.MAX_LENGTH);
  }

  @Test
  void ofRejectsBlankAfterTrim() {
    assertThatThrownBy(() -> PersonName.of("   ")).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> PersonName.of("")).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> PersonName.of(null)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void ofRejectsLongerThanMaxAfterTrim() {
    assertThatThrownBy(() -> PersonName.of("x".repeat(PersonName.MAX_LENGTH + 1)))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void canonicalConstructorRejectsBlankAndUntrimmed() {
    assertThatThrownBy(() -> new PersonName("   ")).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new PersonName(" Ash ")).isInstanceOf(IllegalArgumentException.class);
  }
}
