package com.miguel.pokemonapp.domain.pokemon;

import static com.miguel.pokemonapp.domain.Require.nonBlank;
import static com.miguel.pokemonapp.domain.Require.nonNull;

import java.util.List;

public record CatalogPokemon(
    int nationalNumber,
    String slug,
    String spriteUrl,
    int weightHectograms,
    List<CatalogAbility> abilities) {

  public CatalogPokemon {
    slug = nonBlank(slug, "slug");
    abilities = List.copyOf(nonNull(abilities, "abilities"));
  }
}
