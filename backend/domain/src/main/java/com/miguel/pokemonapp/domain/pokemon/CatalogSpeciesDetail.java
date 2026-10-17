package com.miguel.pokemonapp.domain.pokemon;

import static com.miguel.pokemonapp.domain.Require.atLeast;
import static com.miguel.pokemonapp.domain.Require.nonBlank;
import static com.miguel.pokemonapp.domain.Require.nonNull;

import java.util.List;

public record CatalogSpeciesDetail(
    List<LocalizedText> names,
    List<LocalizedText> genera,
    List<FlavorTextEntry> flavorEntries,
    String generationSlug,
    int evolutionChainId) {

  public CatalogSpeciesDetail {
    names = List.copyOf(nonNull(names, "names"));
    genera = List.copyOf(nonNull(genera, "genera"));
    flavorEntries = List.copyOf(nonNull(flavorEntries, "flavorEntries"));
    generationSlug = nonBlank(generationSlug, "generationSlug");
    atLeast(evolutionChainId, 1, "evolutionChainId");
  }

  public CatalogSpecies toSummary() {
    return new CatalogSpecies(names, genera);
  }
}
