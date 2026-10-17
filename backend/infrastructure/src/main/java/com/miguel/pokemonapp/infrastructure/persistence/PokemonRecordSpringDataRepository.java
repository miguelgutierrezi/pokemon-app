package com.miguel.pokemonapp.infrastructure.persistence;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PokemonRecordSpringDataRepository
    extends JpaRepository<PokemonRecordEntity, Long> {

  Optional<PokemonRecordEntity> findByNationalNumber(int nationalNumber);

  boolean existsByNationalNumber(int nationalNumber);

  long countByNationalNumber(int nationalNumber);

  Page<PokemonRecordEntity> findAllByOrderByNationalNumberAsc(Pageable pageable);
}
