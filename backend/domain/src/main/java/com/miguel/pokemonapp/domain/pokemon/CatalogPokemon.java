package com.miguel.pokemonapp.domain.pokemon;

import java.util.List;

public record CatalogPokemon(
    int nationalNumber,
    String slug,
    String spriteUrl,
    int weightHectograms,
    List<CatalogAbility> abilities) {

  public CatalogPokemon {
    if (slug == null || slug.isBlank()) {
      throw new IllegalArgumentException("slug must be non-blank");
    }
    if (abilities == null) {
      throw new IllegalArgumentException("abilities must not be null");
    }
    abilities = List.copyOf(abilities);
  }
}
