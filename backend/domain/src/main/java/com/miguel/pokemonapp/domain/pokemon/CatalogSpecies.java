package com.miguel.pokemonapp.domain.pokemon;

import java.util.List;

public record CatalogSpecies(List<LocalizedText> names, List<LocalizedText> genera) {

  public CatalogSpecies {
    if (names == null) {
      throw new IllegalArgumentException("names must not be null");
    }
    if (genera == null) {
      throw new IllegalArgumentException("genera must not be null");
    }
    names = List.copyOf(names);
    genera = List.copyOf(genera);
  }
}
