package com.miguel.pokemonapp.domain.pokemon;

import java.util.List;

public record CatalogSpeciesDetail(
    List<LocalizedText> names,
    List<LocalizedText> genera,
    List<FlavorTextEntry> flavorEntries,
    String generationSlug,
    int evolutionChainId) {

  public CatalogSpeciesDetail {
    if (names == null) {
      throw new IllegalArgumentException("names must not be null");
    }
    if (genera == null) {
      throw new IllegalArgumentException("genera must not be null");
    }
    if (flavorEntries == null) {
      throw new IllegalArgumentException("flavorEntries must not be null");
    }
    if (generationSlug == null || generationSlug.isBlank()) {
      throw new IllegalArgumentException("generationSlug must be non-blank");
    }
    if (evolutionChainId < 1) {
      throw new IllegalArgumentException("evolutionChainId must be >= 1");
    }
    names = List.copyOf(names);
    genera = List.copyOf(genera);
    flavorEntries = List.copyOf(flavorEntries);
  }

  public CatalogSpecies toSummary() {
    return new CatalogSpecies(names, genera);
  }
}
