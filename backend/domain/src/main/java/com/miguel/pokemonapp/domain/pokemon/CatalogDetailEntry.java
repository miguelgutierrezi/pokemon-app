package com.miguel.pokemonapp.domain.pokemon;

import static com.miguel.pokemonapp.domain.Require.nonNull;

public record CatalogDetailEntry(CatalogPokemonDetail pokemon, CatalogSpeciesDetail species) {

  public CatalogDetailEntry {
    pokemon = nonNull(pokemon, "pokemon");
    species = nonNull(species, "species");
  }
}
