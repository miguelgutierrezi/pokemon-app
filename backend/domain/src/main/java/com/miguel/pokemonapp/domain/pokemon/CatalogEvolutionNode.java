package com.miguel.pokemonapp.domain.pokemon;

import java.util.List;

public record CatalogEvolutionNode(
    int speciesId,
    List<CatalogEvolutionDetail> evolutionDetails,
    List<CatalogEvolutionNode> children) {

  public CatalogEvolutionNode {
    if (speciesId < 1) {
      throw new IllegalArgumentException("speciesId must be >= 1");
    }
    if (evolutionDetails == null) {
      throw new IllegalArgumentException("evolutionDetails must not be null");
    }
    if (children == null) {
      throw new IllegalArgumentException("children must not be null");
    }
    evolutionDetails = List.copyOf(evolutionDetails);
    children = List.copyOf(children);
  }
}
