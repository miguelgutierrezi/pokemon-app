package com.miguel.pokemonapp.domain.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class EmailTest {

  private static final String EXPECTED_PATTERN =
      "^[a-z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?(?:\\.[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?)+$";

  @Test
  void exposesMaxLengthAndConcretePatternFromDataModel() {
    assertThat(Email.MAX_LENGTH).isEqualTo(254);
    assertThat(Email.PATTERN).isEqualTo(EXPECTED_PATTERN);
  }

  @Test
  void isValidAcceptsMixedCaseAndNormalisesViaOf() {
    assertThat(Email.isValid("Ash@Pokefolio.dev")).isTrue();
    assertThat(Email.of("Ash@Pokefolio.dev").value()).isEqualTo("ash@pokefolio.dev");
  }

  @Test
  void isValidAcceptsPaddedInputAndNormalisesViaOf() {
    assertThat(Email.isValid(" ash@pokefolio.dev ")).isTrue();
    assertThat(Email.of(" ash@pokefolio.dev ").value()).isEqualTo("ash@pokefolio.dev");
  }

  @Test
  void isValidRejectsBlankNullAndInvalidShape() {
    assertThat(Email.isValid(null)).isFalse();
    assertThat(Email.isValid("")).isFalse();
    assertThat(Email.isValid("   ")).isFalse();
    assertThat(Email.isValid("not-an-email")).isFalse();
    assertThat(Email.isValid("missing-tld@localhost")).isFalse();
    assertThat(Email.isValid("@pokefolio.dev")).isFalse();
  }

  @Test
  void isValidRejectsNormalisedLengthOverMax() {
    String tooLong = "a".repeat(250) + "@b.co"; // 255 after normalisation
    assertThat(tooLong.length()).isEqualTo(255);
    assertThat(Email.isValid(tooLong)).isFalse();
  }

  @Test
  void isValidAcceptsNormalisedLengthAtMax() {
    String atMax = "a".repeat(249) + "@b.co"; // 254
    assertThat(atMax.length()).isEqualTo(Email.MAX_LENGTH);
    assertThat(Email.isValid(atMax)).isTrue();
    assertThat(Email.of(atMax).value()).isEqualTo(atMax);
  }

  @Test
  void isValidAcceptsPaddedInputWhoseTrimmedLengthIsExactlyMax() {
    String trimmed = "a".repeat(249) + "@b.co"; // 254 after trim
    String padded = "  " + trimmed + "  ";
    assertThat(trimmed.length()).isEqualTo(Email.MAX_LENGTH);
    assertThat(padded.length()).isGreaterThan(Email.MAX_LENGTH);
    assertThat(Email.isValid(padded)).isTrue();
    assertThat(Email.of(padded).value()).isEqualTo(trimmed);
  }

  @Test
  void ofRejectsInvalidWithIllegalArgumentException() {
    assertThatThrownBy(() -> Email.of("not-an-email")).isInstanceOf(IllegalArgumentException.class);
  }
}
