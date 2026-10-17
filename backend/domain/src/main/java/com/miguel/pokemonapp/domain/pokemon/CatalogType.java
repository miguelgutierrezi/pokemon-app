package com.miguel.pokemonapp.domain.pokemon;

public record CatalogType(int slot, String name) {

  public CatalogType {
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("name must be non-blank");
    }
  }
}
