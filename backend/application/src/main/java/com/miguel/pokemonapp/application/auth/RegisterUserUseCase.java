package com.miguel.pokemonapp.application.auth;

import static com.miguel.pokemonapp.domain.Require.nonNull;

import com.miguel.pokemonapp.application.port.AccessTokenIssuer;
import com.miguel.pokemonapp.application.port.IssuedAccessToken;
import com.miguel.pokemonapp.application.port.PasswordHasher;
import com.miguel.pokemonapp.application.port.UserAccountRepository;
import com.miguel.pokemonapp.domain.user.Email;
import com.miguel.pokemonapp.domain.user.PersonName;
import com.miguel.pokemonapp.domain.user.RawPassword;
import com.miguel.pokemonapp.domain.user.UserAccount;
import java.time.Clock;

public class RegisterUserUseCase {

  private final UserAccountRepository repository;
  private final PasswordHasher hasher;
  private final AccessTokenIssuer issuer;
  private final Clock clock;

  public RegisterUserUseCase(
      UserAccountRepository repository,
      PasswordHasher hasher,
      AccessTokenIssuer issuer,
      Clock clock) {
    this.repository = nonNull(repository, "repository");
    this.hasher = nonNull(hasher, "hasher");
    this.issuer = nonNull(issuer, "issuer");
    this.clock = nonNull(clock, "clock");
  }

  public AuthSession execute(String name, String email, String password) {
    RawPassword.of(password);
    PersonName.of(name);
    Email.of(email);

    String passwordHash = hasher.hash(password);
    UserAccount pending = UserAccount.register(name, email, passwordHash, clock.instant());
    UserAccount saved = repository.save(pending);
    IssuedAccessToken token = issuer.issue(AuthSession.requirePersistedId(saved));
    return AuthSession.of(saved, token);
  }
}
