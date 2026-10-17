package com.miguel.pokemonapp.domain.pokemon;

import static com.miguel.pokemonapp.domain.Require.nonNull;

public record EvolutionChild(EvolutionLink link, EvolutionNode node) {

  public EvolutionChild {
    link = nonNull(link, "link");
    node = nonNull(node, "node");
  }
}
