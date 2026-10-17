package com.miguel.pokemonapp.domain.pokemon;

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
    if (nationalNumber < 1) {
      throw new IllegalArgumentException("nationalNumber must be >= 1");
    }
    if (slug == null || slug.isBlank()) {
      throw new IllegalArgumentException("slug must be non-blank");
    }
    if (abilities == null) {
      throw new IllegalArgumentException("abilities must not be null");
    }
    if (types == null) {
      throw new IllegalArgumentException("types must not be null");
    }
    if (stats == null) {
      throw new IllegalArgumentException("stats must not be null");
    }
    abilities = List.copyOf(abilities);
    types = List.copyOf(types);
    stats = List.copyOf(stats);
  }

  public CatalogPokemon toSummary() {
    return new CatalogPokemon(
        nationalNumber, slug, frontDefaultSprite, weightHectograms, abilities);
  }
}
