package com.miguel.pokemonapp.web.pokemon;

import com.miguel.pokemonapp.domain.pokemon.FlavorText;

public record FlavorTextResponse(String text, String version) {

  static FlavorTextResponse from(FlavorText flavorText) {
    if (flavorText == null) {
      return null;
    }
    return new FlavorTextResponse(flavorText.text(), flavorText.version());
  }
}
