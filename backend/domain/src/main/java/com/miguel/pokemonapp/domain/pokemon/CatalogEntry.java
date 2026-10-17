package com.miguel.pokemonapp.domain.pokemon;

public record CatalogEntry(CatalogPokemon pokemon, CatalogSpecies species) {

  public CatalogEntry {
    if (pokemon == null) {
      throw new IllegalArgumentException("pokemon must not be null");
    }
    if (species == null) {
      throw new IllegalArgumentException("species must not be null");
    }
  }
}
