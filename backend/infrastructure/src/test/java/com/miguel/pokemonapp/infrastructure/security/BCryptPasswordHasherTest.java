package com.miguel.pokemonapp.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BCryptPasswordHasherTest {

  private static final String DEMO_PASSWORD = "Demo-Passw0rd!";
  private static final Pattern SEEDED_HASH =
      Pattern.compile("'(\\$2[aby]\\$[0-9]{2}\\$[A-Za-z0-9./]{53})'");

  private BCryptPasswordHasher hasher;

  @BeforeEach
  void setUp() {
    hasher = new BCryptPasswordHasher();
  }

  @Test
  void hashThenMatchesReturnsTrue() {
    String hash = hasher.hash("password1");

    assertThat(hasher.matches("password1", hash)).isTrue();
  }

  @Test
  void matchesWrongPasswordReturnsFalse() {
    String hash = hasher.hash("password1");

    assertThat(hasher.matches("wrong-pass", hash)).isFalse();
  }

  @Test
  void twoHashesOfSamePlaintextDifferBecauseOfSalt() {
    String first = hasher.hash("password1");
    String second = hasher.hash("password1");

    assertThat(first).isNotEqualTo(second);
    assertThat(hasher.matches("password1", first)).isTrue();
    assertThat(hasher.matches("password1", second)).isTrue();
  }

  @Test
  void hashHasBcryptPrefix() {
    String hash = hasher.hash("password1");

    assertThat(hash).matches("^\\$2[ab]\\$.+");
  }

  @Test
  void matchesDemoPasswordAgainstV5SeededHash() {
    String seededHash = readSeededHashFromV5();

    assertThat(hasher.matches(DEMO_PASSWORD, seededHash)).isTrue();
  }

  /**
   * Observed behaviour with {@code BCryptPasswordEncoder} (default strength 10): {@code matches}
   * does not throw for passwords longer than 72 UTF-8 bytes; it returns {@code false}. Login still
   * short-circuits in {@code AuthenticateUserUseCase} before calling the hasher.
   */
  @Test
  void matchesOnPasswordOverSeventyTwoUtf8BytesDoesNotThrow() {
    String oversize = "ñ".repeat(40); // 80 UTF-8 bytes
    assertThat(oversize.getBytes(StandardCharsets.UTF_8).length).isGreaterThan(72);

    String hash = hasher.hash("password1");

    assertThatCode(() -> hasher.matches(oversize, hash)).doesNotThrowAnyException();
    assertThat(hasher.matches(oversize, hash)).isFalse();
  }

  /**
   * Hash literal from {@code classpath:db/migration/V5__app_user.sql} so the test cannot drift from
   * the Flyway seed.
   */
  private static String readSeededHashFromV5() {
    try (InputStream in =
        BCryptPasswordHasherTest.class.getResourceAsStream("/db/migration/V5__app_user.sql")) {
      assertThat(in).as("V5__app_user.sql on test classpath").isNotNull();
      String sql = new String(in.readAllBytes(), StandardCharsets.UTF_8);
      Matcher matcher = SEEDED_HASH.matcher(sql);
      assertThat(matcher.find()).as("BCrypt hash in V5 INSERT").isTrue();
      return matcher.group(1);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
