package com.miguel.pokemonapp.domain.pokemon;

public record EvolutionChild(EvolutionLink link, EvolutionNode node) {

  public EvolutionChild {
    if (link == null) {
      throw new IllegalArgumentException("link must not be null");
    }
    if (node == null) {
      throw new IllegalArgumentException("node must not be null");
    }
  }
}
