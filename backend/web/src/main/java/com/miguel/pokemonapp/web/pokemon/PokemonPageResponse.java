package com.miguel.pokemonapp.web.pokemon;

import com.miguel.pokemonapp.domain.page.Page;
import com.miguel.pokemonapp.domain.pokemon.PokemonSummary;
import java.util.List;

public record PokemonPageResponse(
    List<PokemonSummaryResponse> content, int page, int size, long totalElements, int totalPages) {

  static PokemonPageResponse from(Page<PokemonSummary> page) {
    return new PokemonPageResponse(
        page.content().stream().map(PokemonSummaryResponse::from).toList(),
        page.page(),
        page.size(),
        page.totalElements(),
        page.totalPages());
  }
}
