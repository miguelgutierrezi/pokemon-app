package com.miguel.pokemonapp.domain.pokemon;

import static com.miguel.pokemonapp.domain.Require.atLeast;
import static com.miguel.pokemonapp.domain.Require.nonNull;

import java.util.List;

public record CatalogEvolutionNode(
    int speciesId,
    List<CatalogEvolutionDetail> evolutionDetails,
    List<CatalogEvolutionNode> children) {

  public CatalogEvolutionNode {
    atLeast(speciesId, 1, "speciesId");
    evolutionDetails = List.copyOf(nonNull(evolutionDetails, "evolutionDetails"));
    children = List.copyOf(nonNull(children, "children"));
  }
}
