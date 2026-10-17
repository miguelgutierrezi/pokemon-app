package com.miguel.pokemonapp.application.pokemon;

import static com.miguel.pokemonapp.domain.Require.atLeast;
import static com.miguel.pokemonapp.domain.Require.nonNull;

import com.miguel.pokemonapp.application.CatalogUnavailableException;
import com.miguel.pokemonapp.application.DuplicateLocalPokemonException;
import com.miguel.pokemonapp.application.port.LocalPokemonRepository;
import com.miguel.pokemonapp.application.port.PokemonCatalogPort;
import com.miguel.pokemonapp.domain.pokemon.CatalogDetailEntry;
import com.miguel.pokemonapp.domain.pokemon.LocalPokemon;
import java.time.Clock;

public class ImportPokemonUseCase {

  private final LocalPokemonRepository repository;
  private final PokemonCatalogPort catalog;
  private final Clock clock;

  public ImportPokemonUseCase(
      LocalPokemonRepository repository, PokemonCatalogPort catalog, Clock clock) {
    this.repository = nonNull(repository, "repository");
    this.catalog = nonNull(catalog, "catalog");
    this.clock = nonNull(clock, "clock");
  }

  public LocalPokemon execute(int nationalNumber) {
    atLeast(nationalNumber, 1, "nationalNumber");

    if (repository.existsByNationalNumber(nationalNumber)) {
      throw new DuplicateLocalPokemonException(nationalNumber);
    }

    CatalogDetailEntry entry = catalog.fetchDetail(nationalNumber);
    LocalPokemon snapshot;
    try {
      snapshot = LocalPokemon.snapshotOf(entry, clock.instant());
    } catch (IllegalArgumentException e) {
      throw new CatalogUnavailableException("malformed catalog data", e);
    }
    return repository.save(snapshot);
  }
}
