package com.miguel.pokemonapp.application.pokemon;

import static com.miguel.pokemonapp.domain.Require.atLeast;
import static com.miguel.pokemonapp.domain.Require.nonNull;

import com.miguel.pokemonapp.application.LocalPokemonNotFoundException;
import com.miguel.pokemonapp.application.port.LocalPokemonRepository;

public class DeleteLocalPokemonUseCase {

  private final LocalPokemonRepository repository;

  public DeleteLocalPokemonUseCase(LocalPokemonRepository repository) {
    this.repository = nonNull(repository, "repository");
  }

  public void execute(int nationalNumber) {
    atLeast(nationalNumber, 1, "nationalNumber");

    if (!repository.deleteByNationalNumber(nationalNumber)) {
      throw new LocalPokemonNotFoundException(nationalNumber);
    }
  }
}
