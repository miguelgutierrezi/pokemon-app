package com.miguel.pokemonapp.infrastructure.persistence.user;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAccountSpringDataRepository extends JpaRepository<UserAccountEntity, Long> {

  Optional<UserAccountEntity> findByEmail(String email);
}
