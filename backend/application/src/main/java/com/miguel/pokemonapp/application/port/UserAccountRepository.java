package com.miguel.pokemonapp.application.port;

import com.miguel.pokemonapp.domain.user.UserAccount;
import java.util.Optional;

public interface UserAccountRepository {

  UserAccount save(UserAccount account);

  Optional<UserAccount> findByEmail(String normalisedEmail);
}
