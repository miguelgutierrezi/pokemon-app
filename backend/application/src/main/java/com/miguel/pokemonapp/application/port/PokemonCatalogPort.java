package com.miguel.pokemonapp.application.port;

import com.miguel.pokemonapp.domain.pokemon.CatalogDetailEntry;
import com.miguel.pokemonapp.domain.pokemon.CatalogEntry;
import com.miguel.pokemonapp.domain.pokemon.CatalogEvolutionChain;
import java.util.List;

public interface PokemonCatalogPort {

  SpeciesPage speciesPage(int offset, int limit);

  List<CatalogEntry> fetchEntries(List<Integer> nationalIds);

  CatalogDetailEntry fetchDetail(int nationalId);

  CatalogEvolutionChain fetchEvolutionChain(int chainId);
}
