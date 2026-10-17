package com.miguel.pokemonapp.domain.pokemon;

import static com.miguel.pokemonapp.domain.Require.atLeast;
import static com.miguel.pokemonapp.domain.Require.nonBlank;
import static com.miguel.pokemonapp.domain.Require.nonNull;

import java.util.List;

public record CatalogPokemonDetail(
    int nationalNumber,
    String slug,
    String frontDefaultSprite,
    String officialArtworkSprite,
    int heightDecimetres,
    int weightHectograms,
    List<CatalogAbility> abilities,
    List<CatalogType> types,
    List<CatalogStat> stats) {

  public CatalogPokemonDetail {
    atLeast(nationalNumber, 1, "nationalNumber");
    slug = nonBlank(slug, "slug");
    abilities = List.copyOf(nonNull(abilities, "abilities"));
    types = List.copyOf(nonNull(types, "types"));
    stats = List.copyOf(nonNull(stats, "stats"));
  }

  public CatalogPokemon toSummary() {
    return new CatalogPokemon(
        nationalNumber, slug, frontDefaultSprite, weightHectograms, abilities);
  }
}
