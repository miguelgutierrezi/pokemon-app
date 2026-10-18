package com.miguel.pokemonapp.infrastructure.persistence;

import java.time.Instant;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PokemonRecordSpringDataRepository
    extends JpaRepository<PokemonRecordEntity, Long> {

  Optional<PokemonRecordEntity> findByNationalNumber(int nationalNumber);

  /**
   * Optimistic compare-and-set: writes the proprietary scalars and bumps the version only if the
   * stored version still equals {@code expectedVersion}. Returns the number of rows updated (0 when
   * the record changed or was deleted since the client read it). The version bump does not depend
   * on Hibernate dirty checking, so tag-only updates (inverse collection) are versioned too.
   */
  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query(
      "update PokemonRecordEntity p set p.localizedName = :localizedName,"
          + " p.location = :location, p.updatedAt = :updatedAt, p.version = p.version + 1"
          + " where p.nationalNumber = :nationalNumber and p.version = :expectedVersion")
  int updateProprietaryIfVersion(
      @Param("nationalNumber") int nationalNumber,
      @Param("expectedVersion") long expectedVersion,
      @Param("localizedName") String localizedName,
      @Param("location") String location,
      @Param("updatedAt") Instant updatedAt);

  boolean existsByNationalNumber(int nationalNumber);

  long countByNationalNumber(int nationalNumber);

  long deleteByNationalNumber(int nationalNumber);

  Page<PokemonRecordEntity> findAllByOrderByNationalNumberAsc(Pageable pageable);
}
