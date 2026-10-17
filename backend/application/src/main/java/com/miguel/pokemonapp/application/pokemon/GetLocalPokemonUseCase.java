package com.miguel.pokemonapp.application.pokemon;

import static com.miguel.pokemonapp.domain.Require.atLeast;
import static com.miguel.pokemonapp.domain.Require.nonNull;

import com.miguel.pokemonapp.application.LocalPokemonNotFoundException;
import com.miguel.pokemonapp.application.port.LocalPokemonRepository;
import com.miguel.pokemonapp.domain.pokemon.LocalPokemon;

public class GetLocalPokemonUseCase {

  private final LocalPokemonRepository repository;

  public GetLocalPokemonUseCase(LocalPokemonRepository repository) {
    this.repository = nonNull(repository, "repository");
  }

  public LocalPokemon execute(int nationalNumber) {
    atLeast(nationalNumber, 1, "nationalNumber");
    return repository
        .findByNationalNumber(nationalNumber)
        .orElseThrow(() -> new LocalPokemonNotFoundException(nationalNumber));
  }
}
