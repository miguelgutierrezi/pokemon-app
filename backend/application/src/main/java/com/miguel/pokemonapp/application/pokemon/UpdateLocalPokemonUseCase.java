package com.miguel.pokemonapp.application.pokemon;

import static com.miguel.pokemonapp.domain.Require.atLeast;
import static com.miguel.pokemonapp.domain.Require.nonNull;

import com.miguel.pokemonapp.application.LocalPokemonNotFoundException;
import com.miguel.pokemonapp.application.StaleLocalPokemonException;
import com.miguel.pokemonapp.application.port.LocalPokemonRepository;
import com.miguel.pokemonapp.domain.pokemon.LocalPokemon;
import com.miguel.pokemonapp.domain.pokemon.Tags;

public class UpdateLocalPokemonUseCase {

  private final LocalPokemonRepository repository;

  public UpdateLocalPokemonUseCase(LocalPokemonRepository repository) {
    this.repository = nonNull(repository, "repository");
  }

  public LocalPokemon execute(int nationalNumber, ProprietaryUpdate update) {
    atLeast(nationalNumber, 1, "nationalNumber");
    update = nonNull(update, "update");

    LocalPokemon current =
        repository
            .findByNationalNumber(nationalNumber)
            .orElseThrow(() -> new LocalPokemonNotFoundException(nationalNumber));

    if (update.expectedVersion() != current.version()) {
      throw new StaleLocalPokemonException(nationalNumber);
    }

    return repository.save(
        current.withProprietary(update.localizedName(), update.location(), Tags.of(update.tags())));
  }
}
