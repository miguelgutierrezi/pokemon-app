package com.miguel.pokemonapp.domain.user;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class RawPasswordTest {

  @Test
  void exposesMinLengthAndMaxUtf8ByteConstants() {
    assertThat(RawPassword.MIN_LENGTH).isEqualTo(8);
    assertThat(RawPassword.MAX_UTF8_BYTES).isEqualTo(72);
  }

  @Test
  void ofAcceptsPasswordAtMinCharacterLength() {
    assertThat(RawPassword.of("password").value()).isEqualTo("password");
  }

  @Test
  void ofDoesNotTrimPassword() {
    assertThat(RawPassword.of("  password  ").value()).isEqualTo("  password  ");
  }

  @Test
  void ofRejectsShorterThanMinCharacters() {
    assertThatThrownBy(() -> RawPassword.of("short")).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void ofRejectsFortyEnYeAsEightyUtf8Bytes() {
    String eightyBytes = "ñ".repeat(40);
    assertThat(eightyBytes.getBytes(UTF_8)).hasSize(80);
    assertThatThrownBy(() -> RawPassword.of(eightyBytes))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void ofAcceptsExactlySeventyTwoUtf8BytesWithMultibyteChars() {
    String seventyTwoBytes = "ñ".repeat(36);
    assertThat(seventyTwoBytes.getBytes(UTF_8)).hasSize(RawPassword.MAX_UTF8_BYTES);
    assertThat(RawPassword.of(seventyTwoBytes).value()).isEqualTo(seventyTwoBytes);
  }

  @Test
  void ofRejectsSeventyThreeUtf8Bytes() {
    String seventyThreeAscii = "a".repeat(73);
    assertThat(seventyThreeAscii.getBytes(UTF_8)).hasSize(73);
    assertThatThrownBy(() -> RawPassword.of(seventyThreeAscii))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void ofRejectsNull() {
    assertThatThrownBy(() -> RawPassword.of(null)).isInstanceOf(IllegalArgumentException.class);
  }
}
