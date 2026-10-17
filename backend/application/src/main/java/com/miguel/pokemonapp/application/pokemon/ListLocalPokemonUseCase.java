package com.miguel.pokemonapp.application.pokemon;

import static com.miguel.pokemonapp.domain.Require.nonNull;

import com.miguel.pokemonapp.application.port.LocalPokemonRepository;
import com.miguel.pokemonapp.domain.page.Page;
import com.miguel.pokemonapp.domain.page.PageRequest;
import com.miguel.pokemonapp.domain.pokemon.LocalPokemon;

public class ListLocalPokemonUseCase {

  private final LocalPokemonRepository repository;

  public ListLocalPokemonUseCase(LocalPokemonRepository repository) {
    this.repository = nonNull(repository, "repository");
  }

  public Page<LocalPokemon> execute(PageRequest request) {
    return repository.findPage(nonNull(request, "request"));
  }
}
