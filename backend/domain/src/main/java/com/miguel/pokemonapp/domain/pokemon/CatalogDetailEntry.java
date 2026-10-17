package com.miguel.pokemonapp.domain.pokemon;

public record CatalogDetailEntry(CatalogPokemonDetail pokemon, CatalogSpeciesDetail species) {

  public CatalogDetailEntry {
    if (pokemon == null) {
      throw new IllegalArgumentException("pokemon must not be null");
    }
    if (species == null) {
      throw new IllegalArgumentException("species must not be null");
    }
  }
}
