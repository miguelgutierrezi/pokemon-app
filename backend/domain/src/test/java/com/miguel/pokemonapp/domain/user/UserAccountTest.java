package com.miguel.pokemonapp.domain.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class UserAccountTest {

  private static final Instant CREATED_AT = Instant.parse("2026-10-17T12:00:00Z");
  private static final String OPAQUE_HASH = "$2a$10$abcdefghijklmnopqrstuuABCDEFGHIJKLMNOPQRSTUV";

  @Test
  void registerReturnsAccountWithoutId() {
    UserAccount account =
        UserAccount.register("  Ash  ", "Ash@Pokefolio.dev", OPAQUE_HASH, CREATED_AT);

    assertThat(account.id()).isEqualTo(Optional.empty());
    assertThat(account.name().value()).isEqualTo("Ash");
    assertThat(account.email().value()).isEqualTo("ash@pokefolio.dev");
    assertThat(account.passwordHash()).isEqualTo(OPAQUE_HASH);
    assertThat(account.createdAt()).isEqualTo(CREATED_AT);
  }

  @Test
  void restoreExposesPresentIdWhenAtLeastOne() {
    UserAccount account =
        UserAccount.restore(42L, "  Misty  ", "Misty@Pokefolio.dev", OPAQUE_HASH, CREATED_AT);

    assertThat(account.id()).isEqualTo(Optional.of(42L));
    assertThat(account.name().value()).isEqualTo("Misty");
    assertThat(account.email().value()).isEqualTo("misty@pokefolio.dev");
    assertThat(account.passwordHash()).isEqualTo(OPAQUE_HASH);
    assertThat(account.createdAt()).isEqualTo(CREATED_AT);
  }

  @Test
  void restoreRejectsIdLessThanOne() {
    assertThatThrownBy(
            () -> UserAccount.restore(0L, "Ash", "ash@pokefolio.dev", OPAQUE_HASH, CREATED_AT))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void registerRejectsInvalidEmail() {
    assertThatThrownBy(() -> UserAccount.register("Ash", "not-an-email", OPAQUE_HASH, CREATED_AT))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void registerRejectsBlankName() {
    assertThatThrownBy(
            () -> UserAccount.register("   ", "ash@pokefolio.dev", OPAQUE_HASH, CREATED_AT))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void registerRejectsBlankPasswordHash() {
    assertThatThrownBy(() -> UserAccount.register("Ash", "ash@pokefolio.dev", "  ", CREATED_AT))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> UserAccount.register("Ash", "ash@pokefolio.dev", null, CREATED_AT))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
