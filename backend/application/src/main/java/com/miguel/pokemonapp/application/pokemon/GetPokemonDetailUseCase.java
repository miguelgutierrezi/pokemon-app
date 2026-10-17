package com.miguel.pokemonapp.application.pokemon;

import static com.miguel.pokemonapp.domain.Require.atLeast;

import com.miguel.pokemonapp.application.CatalogUnavailableException;
import com.miguel.pokemonapp.application.port.PokemonCatalogPort;
import com.miguel.pokemonapp.domain.pokemon.CatalogDetailEntry;
import com.miguel.pokemonapp.domain.pokemon.CatalogEntry;
import com.miguel.pokemonapp.domain.pokemon.CatalogEvolutionChain;
import com.miguel.pokemonapp.domain.pokemon.CatalogEvolutionNode;
import com.miguel.pokemonapp.domain.pokemon.PokemonDetail;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GetPokemonDetailUseCase {

  private final PokemonCatalogPort catalog;

  public GetPokemonDetailUseCase(PokemonCatalogPort catalog) {
    this.catalog = catalog;
  }

  public PokemonDetail execute(int nationalId) {
    atLeast(nationalId, 1, "nationalId");

    CatalogDetailEntry detail = catalog.fetchDetail(nationalId);
    CatalogEvolutionChain chain = catalog.fetchEvolutionChain(detail.species().evolutionChainId());
    List<CatalogEntry> entries = catalog.fetchEntries(speciesIdsDepthFirst(chain.root()));

    try {
      return PokemonDetail.assemble(detail, chain, entries, nationalId);
    } catch (IllegalArgumentException e) {
      throw new CatalogUnavailableException("malformed catalog data", e);
    }
  }

  private static List<Integer> speciesIdsDepthFirst(CatalogEvolutionNode root) {
    List<Integer> ids = new ArrayList<>();
    Set<Integer> seen = new HashSet<>();
    collectDepthFirst(root, ids, seen);
    return List.copyOf(ids);
  }

  private static void collectDepthFirst(
      CatalogEvolutionNode node, List<Integer> ids, Set<Integer> seen) {
    if (seen.add(node.speciesId())) {
      ids.add(node.speciesId());
    }
    for (CatalogEvolutionNode child : node.children()) {
      collectDepthFirst(child, ids, seen);
    }
  }
}
