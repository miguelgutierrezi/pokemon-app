package com.miguel.pokemonapp.application.port;

import com.miguel.pokemonapp.domain.pokemon.CatalogPokemon;
import com.miguel.pokemonapp.domain.pokemon.CatalogSpecies;

public interface PokemonCatalogPort {

  SpeciesPage speciesPage(int offset, int limit);

  CatalogPokemon fetchPokemon(int nationalId);

  CatalogSpecies fetchSpecies(int nationalId);
}
