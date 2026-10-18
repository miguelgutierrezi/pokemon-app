package com.miguel.pokemonapp.application.auth;

import static com.miguel.pokemonapp.domain.Require.nonBlank;
import static com.miguel.pokemonapp.domain.Require.nonNull;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.miguel.pokemonapp.application.InvalidCredentialsException;
import com.miguel.pokemonapp.application.port.AccessTokenIssuer;
import com.miguel.pokemonapp.application.port.IssuedAccessToken;
import com.miguel.pokemonapp.application.port.PasswordHasher;
import com.miguel.pokemonapp.application.port.UserAccountRepository;
import com.miguel.pokemonapp.domain.user.RawPassword;
import com.miguel.pokemonapp.domain.user.UserAccount;
import java.util.Locale;
import java.util.Optional;

public class AuthenticateUserUseCase {

  private final UserAccountRepository repository;
  private final PasswordHasher hasher;
  private final AccessTokenIssuer issuer;
  private final String dummyHash;

  public AuthenticateUserUseCase(
      UserAccountRepository repository,
      PasswordHasher hasher,
      AccessTokenIssuer issuer,
      String dummyHash) {
    this.repository = nonNull(repository, "repository");
    this.hasher = nonNull(hasher, "hasher");
    this.issuer = nonNull(issuer, "issuer");
    this.dummyHash = nonBlank(dummyHash, "dummyHash");
  }

  public AuthSession execute(String email, String password) {
    nonNull(password, "password");
    if (password.getBytes(UTF_8).length > RawPassword.MAX_UTF8_BYTES) {
      throw new InvalidCredentialsException();
    }

    String normalisedEmail = nonNull(email, "email").trim().toLowerCase(Locale.ROOT);
    Optional<UserAccount> found = repository.findByEmail(normalisedEmail);
    if (found.isEmpty()) {
      hasher.matches(password, dummyHash);
      throw new InvalidCredentialsException();
    }

    UserAccount account = found.get();
    if (!hasher.matches(password, account.passwordHash())) {
      throw new InvalidCredentialsException();
    }

    IssuedAccessToken token = issuer.issue(AuthSession.requirePersistedId(account));
    return AuthSession.of(account, token);
  }
}
