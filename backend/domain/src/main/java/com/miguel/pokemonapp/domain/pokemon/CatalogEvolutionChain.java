package com.miguel.pokemonapp.domain.pokemon;

import static com.miguel.pokemonapp.domain.Require.nonNull;

public record CatalogEvolutionChain(CatalogEvolutionNode root) {

  public CatalogEvolutionChain {
    root = nonNull(root, "root");
  }
}
