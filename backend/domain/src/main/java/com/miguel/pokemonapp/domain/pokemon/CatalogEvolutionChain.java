package com.miguel.pokemonapp.domain.pokemon;

public record CatalogEvolutionChain(CatalogEvolutionNode root) {

  public CatalogEvolutionChain {
    if (root == null) {
      throw new IllegalArgumentException("root must not be null");
    }
  }
}
