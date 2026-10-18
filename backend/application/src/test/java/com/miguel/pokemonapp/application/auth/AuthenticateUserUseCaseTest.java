package com.miguel.pokemonapp.application.auth;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.miguel.pokemonapp.application.InvalidCredentialsException;
import com.miguel.pokemonapp.application.port.AccessTokenIssuer;
import com.miguel.pokemonapp.application.port.PasswordHasher;
import com.miguel.pokemonapp.application.port.UserAccountRepository;
import com.miguel.pokemonapp.domain.user.UserAccount;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AuthenticateUserUseCaseTest {

  private static final Instant CREATED_AT = Instant.parse("2026-10-17T12:00:00Z");
  private static final String HASH = "$2a$10$abcdefghijklmnopqrstuuABCDEFGHIJKLMNOPQRSTUV";
  private static final String DUMMY_HASH = "$2a$10$dummyhashdummyhashdummyhashdummyhashdummyha";
  private static final String TOKEN = "jwt.access.token";
  private static final String CREDENTIALS_DETAIL = "Invalid email or password";

  private UserAccountRepository repository;
  private PasswordHasher hasher;
  private AccessTokenIssuer issuer;
  private AuthenticateUserUseCase useCase;

  @BeforeEach
  void setUp() {
    repository = mock(UserAccountRepository.class);
    hasher = mock(PasswordHasher.class);
    issuer = mock(AccessTokenIssuer.class);
    useCase = new AuthenticateUserUseCase(repository, hasher, issuer, DUMMY_HASH);
  }

  @Test
  void unknownEmailComparesAgainstDummyHashThenThrowsInvalidCredentials() {
    when(repository.findByEmail("ash@pokefolio.dev")).thenReturn(Optional.empty());
    when(hasher.matches("password1", DUMMY_HASH)).thenReturn(false);

    assertThatThrownBy(() -> useCase.execute(" Ash@Pokefolio.dev ", "password1"))
        .isInstanceOf(InvalidCredentialsException.class)
        .hasMessage(CREDENTIALS_DETAIL);

    verify(repository).findByEmail("ash@pokefolio.dev");
    verify(hasher).matches("password1", DUMMY_HASH);
    verify(issuer, never()).issue(anyLong());
  }

  @Test
  void wrongPasswordThrowsSameInvalidCredentialsMessage() {
    UserAccount account =
        UserAccount.restore(7L, "Ash", "ash@pokefolio.dev", HASH, CREATED_AT);
    when(repository.findByEmail("ash@pokefolio.dev")).thenReturn(Optional.of(account));
    when(hasher.matches("wrong-password", HASH)).thenReturn(false);

    assertThatThrownBy(() -> useCase.execute("ash@pokefolio.dev", "wrong-password"))
        .isInstanceOf(InvalidCredentialsException.class)
        .hasMessage(CREDENTIALS_DETAIL);

    verify(hasher).matches("wrong-password", HASH);
    verify(issuer, never()).issue(anyLong());
  }

  @Test
  void successReturnsAuthSession() {
    UserAccount account =
        UserAccount.restore(7L, "Ash", "ash@pokefolio.dev", HASH, CREATED_AT);
    when(repository.findByEmail("ash@pokefolio.dev")).thenReturn(Optional.of(account));
    when(hasher.matches("password1", HASH)).thenReturn(true);
    when(issuer.issue(7L)).thenReturn(new IssuedAccessToken(TOKEN, 3600L));

    AuthSession session = useCase.execute("ASH@POKEFOLIO.DEV", "password1");

    assertThat(session.accessToken()).isEqualTo(TOKEN);
    assertThat(session.tokenType()).isEqualTo("Bearer");
    assertThat(session.expiresIn()).isEqualTo(3600L);
    assertThat(session.user().id()).isEqualTo(7L);
    assertThat(session.user().name()).isEqualTo("Ash");
    assertThat(session.user().email()).isEqualTo("ash@pokefolio.dev");
  }

  @Test
  void passwordOverSeventyTwoUtf8BytesThrowsWithoutCallingHasher() {
    String oversize = "ñ".repeat(40);
    assertThat(oversize.getBytes(UTF_8)).hasSize(80);

    assertThatThrownBy(() -> useCase.execute("ash@pokefolio.dev", oversize))
        .isInstanceOf(InvalidCredentialsException.class)
        .hasMessage(CREDENTIALS_DETAIL);

    verifyNoInteractions(hasher);
    verify(repository, never()).findByEmail(anyString());
    verifyNoInteractions(issuer);
  }

  @Test
  void loginDoesNotApplyRawPasswordPolicyShortPasswordStillReachesMatches() {
    when(repository.findByEmail("ash@pokefolio.dev")).thenReturn(Optional.empty());
    when(hasher.matches("abc", DUMMY_HASH)).thenReturn(false);

    assertThatThrownBy(() -> useCase.execute("ash@pokefolio.dev", "abc"))
        .isInstanceOf(InvalidCredentialsException.class)
        .hasMessage(CREDENTIALS_DETAIL);

    verify(hasher).matches("abc", DUMMY_HASH);
  }

  @Test
  void malformedEmailDoesNotThrowDomainIllegalArgumentException() {
    when(repository.findByEmail("not-an-email")).thenReturn(Optional.empty());
    when(hasher.matches("password1", DUMMY_HASH)).thenReturn(false);

    assertThatThrownBy(() -> useCase.execute("not-an-email", "password1"))
        .isInstanceOf(InvalidCredentialsException.class)
        .hasMessage(CREDENTIALS_DETAIL)
        .isNotInstanceOf(IllegalArgumentException.class);

    verify(repository).findByEmail("not-an-email");
    verify(hasher).matches("password1", DUMMY_HASH);
    verify(issuer, never()).issue(anyLong());
  }

  @Test
  void malformedPaddedEmailIsLookedUpAfterTrimAndLocaleRootLowercase() {
    when(repository.findByEmail("not an email")).thenReturn(Optional.empty());
    when(hasher.matches("password1", DUMMY_HASH)).thenReturn(false);

    assertThatThrownBy(() -> useCase.execute(" Not An Email ", "password1"))
        .isInstanceOf(InvalidCredentialsException.class)
        .hasMessage(CREDENTIALS_DETAIL)
        .isNotInstanceOf(IllegalArgumentException.class);

    verify(repository).findByEmail("not an email");
    verify(hasher).matches("password1", DUMMY_HASH);
    verify(issuer, never()).issue(anyLong());
  }
}
