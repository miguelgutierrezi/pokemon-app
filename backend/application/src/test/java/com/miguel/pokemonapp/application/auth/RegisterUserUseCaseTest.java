package com.miguel.pokemonapp.application.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.miguel.pokemonapp.application.DuplicateEmailException;
import com.miguel.pokemonapp.application.port.AccessTokenIssuer;
import com.miguel.pokemonapp.application.port.PasswordHasher;
import com.miguel.pokemonapp.application.port.UserAccountRepository;
import com.miguel.pokemonapp.domain.user.UserAccount;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class RegisterUserUseCaseTest {

  private static final Instant CREATED_AT = Instant.parse("2026-10-17T12:00:00Z");
  private static final String HASH = "$2a$10$abcdefghijklmnopqrstuuABCDEFGHIJKLMNOPQRSTUV";
  private static final String TOKEN = "jwt.access.token";

  private UserAccountRepository repository;
  private PasswordHasher hasher;
  private AccessTokenIssuer issuer;
  private RegisterUserUseCase useCase;

  @BeforeEach
  void setUp() {
    repository = mock(UserAccountRepository.class);
    hasher = mock(PasswordHasher.class);
    issuer = mock(AccessTokenIssuer.class);
    Clock clock = Clock.fixed(CREATED_AT, ZoneOffset.UTC);
    useCase = new RegisterUserUseCase(repository, hasher, issuer, clock);
  }

  @Test
  void registersHashesSavesIssuesTokenAndReturnsAuthSession() {
    when(hasher.hash("password1")).thenReturn(HASH);
    when(repository.save(any(UserAccount.class)))
        .thenAnswer(
            invocation -> {
              UserAccount pending = invocation.getArgument(0);
              return UserAccount.restore(
                  7L,
                  pending.name().value(),
                  pending.email().value(),
                  pending.passwordHash(),
                  pending.createdAt());
            });
    when(issuer.issue(7L)).thenReturn(new IssuedAccessToken(TOKEN, 3600L));

    AuthSession session = useCase.execute("  Ash  ", "Ash@Pokefolio.dev", "password1");

    assertThat(session.accessToken()).isEqualTo(TOKEN);
    assertThat(session.tokenType()).isEqualTo("Bearer");
    assertThat(session.expiresIn()).isEqualTo(3600L);
    assertThat(session.user().id()).isEqualTo(7L);
    assertThat(session.user().name()).isEqualTo("Ash");
    assertThat(session.user().email()).isEqualTo("ash@pokefolio.dev");

    ArgumentCaptor<UserAccount> saved = ArgumentCaptor.forClass(UserAccount.class);
    verify(hasher).hash("password1");
    verify(repository).save(saved.capture());
    assertThat(saved.getValue().id()).isEmpty();
    assertThat(saved.getValue().passwordHash()).isEqualTo(HASH);
    assertThat(saved.getValue().createdAt()).isEqualTo(CREATED_AT);
    assertThat(saved.getValue().email().value()).isEqualTo("ash@pokefolio.dev");
    verify(issuer).issue(7L);
  }

  @Test
  void propagatesDuplicateEmailExceptionFromRepository() {
    when(hasher.hash("password1")).thenReturn(HASH);
    when(repository.save(any(UserAccount.class)))
        .thenThrow(new DuplicateEmailException("ash@pokefolio.dev"));

    assertThatThrownBy(() -> useCase.execute("Ash", "ash@pokefolio.dev", "password1"))
        .isInstanceOf(DuplicateEmailException.class);

    verify(issuer, never()).issue(anyLong());
  }

  @Test
  void invalidPasswordPolicyThrowsBeforeHashing() {
    assertThatThrownBy(() -> useCase.execute("Ash", "ash@pokefolio.dev", "short"))
        .isInstanceOf(IllegalArgumentException.class);

    verifyNoInteractions(hasher);
    verifyNoInteractions(repository);
    verifyNoInteractions(issuer);
  }

  @Test
  void invalidEmailThrowsBeforeHashing() {
    assertThatThrownBy(() -> useCase.execute("Ash", "not-an-email", "password1"))
        .isInstanceOf(IllegalArgumentException.class);

    verifyNoInteractions(hasher);
    verify(repository, never()).save(any());
    verifyNoInteractions(issuer);
  }

  @Test
  void blankNameThrowsBeforeHashing() {
    assertThatThrownBy(() -> useCase.execute("   ", "ash@pokefolio.dev", "password1"))
        .isInstanceOf(IllegalArgumentException.class);

    verifyNoInteractions(hasher);
  }
}
