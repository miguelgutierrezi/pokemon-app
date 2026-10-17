package com.miguel.pokemonapp.infrastructure.persistence;

import com.miguel.pokemonapp.application.DuplicateLocalPokemonException;
import com.miguel.pokemonapp.application.port.LocalPokemonRepository;
import com.miguel.pokemonapp.domain.page.Page;
import com.miguel.pokemonapp.domain.page.PageRequest;
import com.miguel.pokemonapp.domain.pokemon.LocalPokemon;
import com.miguel.pokemonapp.domain.pokemon.Tags;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

public class LocalPokemonJpaAdapter implements LocalPokemonRepository {

  static final String NATIONAL_NUMBER_UNIQUE_CONSTRAINT = "uq_pokemon_record_national_number";

  private final PokemonRecordSpringDataRepository repository;
  private final Clock clock;
  private final TransactionTemplate writeTx;
  private final TransactionTemplate readTx;

  public LocalPokemonJpaAdapter(
      PokemonRecordSpringDataRepository repository,
      Clock clock,
      PlatformTransactionManager transactionManager) {
    this.repository = repository;
    this.clock = clock;
    this.writeTx = new TransactionTemplate(transactionManager);
    this.readTx = new TransactionTemplate(transactionManager);
    this.readTx.setReadOnly(true);
  }

  @Override
  public LocalPokemon save(LocalPokemon pokemon) {
    return writeTx.execute(status -> doSave(pokemon));
  }

  private LocalPokemon doSave(LocalPokemon pokemon) {
    try {
      PokemonRecordEntity entity = new PokemonRecordEntity();
      Instant now = clock.instant();
      entity.setNationalNumber(pokemon.nationalNumber());
      entity.setName(pokemon.name());
      entity.setCategory(pokemon.category());
      entity.setHeightM(pokemon.heightM());
      entity.setWeightKg(pokemon.weightKg());
      entity.setImageUrl(pokemon.imageUrl());
      entity.setGeneration(pokemon.generation());
      entity.setSyncedAt(pokemon.syncedAt());
      entity.setLocalizedName(pokemon.localizedName());
      entity.setLocation(pokemon.location());
      entity.setCreatedAt(now);
      entity.setUpdatedAt(now);
      // Flush parent first so IDENTITY id is assigned before child rows cascade;
      // also keeps column-limit failures from entering child ID generation.
      PokemonRecordEntity persisted = repository.saveAndFlush(entity);
      persisted.replaceTypes(pokemon.types());
      persisted.replaceTags(pokemon.tags().values());
      return toDomain(repository.saveAndFlush(persisted));
    } catch (DataIntegrityViolationException ex) {
      if (isNationalNumberUniqueViolation(ex)) {
        throw new DuplicateLocalPokemonException(pokemon.nationalNumber());
      }
      throw ex;
    }
  }

  @Override
  public Optional<LocalPokemon> findByNationalNumber(int nationalNumber) {
    return readTx.execute(
        status -> repository.findByNationalNumber(nationalNumber).map(this::toDomain));
  }

  @Override
  public Page<LocalPokemon> findPage(PageRequest request) {
    return readTx.execute(
        status -> {
          Pageable pageable =
              org.springframework.data.domain.PageRequest.of(request.page() - 1, request.size());
          org.springframework.data.domain.Page<PokemonRecordEntity> springPage =
              repository.findAllByOrderByNationalNumberAsc(pageable);
          List<LocalPokemon> content =
              springPage.getContent().stream().map(this::toDomain).toList();
          return Page.of(content, request, springPage.getTotalElements());
        });
  }

  @Override
  public boolean existsByNationalNumber(int nationalNumber) {
    return Boolean.TRUE.equals(
        readTx.execute(status -> repository.existsByNationalNumber(nationalNumber)));
  }

  @Override
  public boolean deleteByNationalNumber(int nationalNumber) {
    throw new UnsupportedOperationException("deleteByNationalNumber not implemented yet");
  }

  private static boolean isNationalNumberUniqueViolation(DataIntegrityViolationException ex) {
    for (Throwable cursor = ex; cursor != null; cursor = cursor.getCause()) {
      if (cursor instanceof ConstraintViolationException constraintViolation) {
        String name = constraintViolation.getConstraintName();
        if (name != null && NATIONAL_NUMBER_UNIQUE_CONSTRAINT.equalsIgnoreCase(name)) {
          return true;
        }
      }
    }
    return false;
  }

  private LocalPokemon toDomain(PokemonRecordEntity entity) {
    List<String> types =
        entity.getTypes().stream()
            .sorted(Comparator.comparingInt(PokemonRecordTypeEntity::getSlot))
            .map(PokemonRecordTypeEntity::getType)
            .toList();
    List<String> tags = entity.getTags().stream().map(PokemonRecordTagEntity::getTag).toList();
    return new LocalPokemon(
        entity.getNationalNumber(),
        entity.getName(),
        entity.getCategory(),
        types,
        entity.getHeightM(),
        entity.getWeightKg(),
        entity.getImageUrl(),
        entity.getGeneration(),
        entity.getSyncedAt(),
        entity.getLocalizedName(),
        entity.getLocation(),
        Tags.of(tags),
        0L);
  }
}
