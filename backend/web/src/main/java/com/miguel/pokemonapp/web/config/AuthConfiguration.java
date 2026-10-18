package com.miguel.pokemonapp.web.config;

import com.miguel.pokemonapp.application.auth.AuthenticateUserUseCase;
import com.miguel.pokemonapp.application.auth.RegisterUserUseCase;
import com.miguel.pokemonapp.application.port.AccessTokenIssuer;
import com.miguel.pokemonapp.application.port.PasswordHasher;
import com.miguel.pokemonapp.application.port.UserAccountRepository;
import com.miguel.pokemonapp.infrastructure.persistence.user.UserAccountJpaAdapter;
import com.miguel.pokemonapp.infrastructure.persistence.user.UserAccountSpringDataRepository;
import com.miguel.pokemonapp.infrastructure.security.BCryptPasswordHasher;
import java.time.Clock;
import java.util.UUID;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class AuthConfiguration {

  @Bean
  BCryptPasswordHasher passwordHasher() {
    return new BCryptPasswordHasher();
  }

  @Bean
  UserAccountJpaAdapter userAccountJpaAdapter(
      UserAccountSpringDataRepository springDataRepository,
      PlatformTransactionManager transactionManager) {
    return new UserAccountJpaAdapter(springDataRepository, transactionManager);
  }

  @Bean
  RegisterUserUseCase registerUserUseCase(
      UserAccountRepository repository,
      PasswordHasher hasher,
      AccessTokenIssuer issuer,
      Clock clock) {
    return new RegisterUserUseCase(repository, hasher, issuer, clock);
  }

  @Bean
  AuthenticateUserUseCase authenticateUserUseCase(
      UserAccountRepository repository, PasswordHasher hasher, AccessTokenIssuer issuer) {
    String dummyHash = hasher.hash(UUID.randomUUID().toString());
    return new AuthenticateUserUseCase(repository, hasher, issuer, dummyHash);
  }
}
