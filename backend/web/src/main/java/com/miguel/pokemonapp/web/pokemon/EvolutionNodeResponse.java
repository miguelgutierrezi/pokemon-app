package com.miguel.pokemonapp.web.pokemon;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.miguel.pokemonapp.domain.pokemon.EvolutionNode;
import java.util.List;

public record EvolutionNodeResponse(
    int nationalNumber,
    String name,
    String imageUrl,
    @JsonProperty("isCurrent") boolean isCurrent,
    List<EvolutionChildResponse> children) {

  static EvolutionNodeResponse from(EvolutionNode node) {
    return new EvolutionNodeResponse(
        node.nationalNumber(),
        node.name(),
        node.imageUrl(),
        node.isCurrent(),
        node.children().stream().map(EvolutionChildResponse::from).toList());
  }
}
