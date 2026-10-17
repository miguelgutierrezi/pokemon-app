package com.miguel.pokemonapp.web.pokemon;

import com.miguel.pokemonapp.domain.pokemon.EvolutionChild;

public record EvolutionChildResponse(
    String trigger, Integer minLevel, String itemSlug, EvolutionNodeResponse node) {

  static EvolutionChildResponse from(EvolutionChild child) {
    return new EvolutionChildResponse(
        child.link().trigger(),
        child.link().minLevel(),
        child.link().itemSlug(),
        EvolutionNodeResponse.from(child.node()));
  }
}
