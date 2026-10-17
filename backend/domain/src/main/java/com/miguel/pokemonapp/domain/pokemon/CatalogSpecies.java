package com.miguel.pokemonapp.domain.pokemon;

import static com.miguel.pokemonapp.domain.Require.nonNull;

import java.util.List;

public record CatalogSpecies(List<LocalizedText> names, List<LocalizedText> genera) {

  public CatalogSpecies {
    names = List.copyOf(nonNull(names, "names"));
    genera = List.copyOf(nonNull(genera, "genera"));
  }
}
