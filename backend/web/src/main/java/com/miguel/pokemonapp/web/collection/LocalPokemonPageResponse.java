package com.miguel.pokemonapp.web.collection;

import com.miguel.pokemonapp.domain.page.Page;
import com.miguel.pokemonapp.domain.pokemon.LocalPokemon;
import java.util.List;

public record LocalPokemonPageResponse(
    List<LocalPokemonResponse> content, int page, int size, long totalElements, int totalPages) {

  static LocalPokemonPageResponse from(Page<LocalPokemon> page) {
    return new LocalPokemonPageResponse(
        page.content().stream().map(LocalPokemonResponse::from).toList(),
        page.page(),
        page.size(),
        page.totalElements(),
        page.totalPages());
  }
}
