package com.miguel.pokemonapp.domain.pokemon;

import static com.miguel.pokemonapp.domain.Require.nonNull;

public record CatalogEntry(CatalogPokemon pokemon, CatalogSpecies species) {

  public CatalogEntry {
    pokemon = nonNull(pokemon, "pokemon");
    species = nonNull(species, "species");
  }
}
