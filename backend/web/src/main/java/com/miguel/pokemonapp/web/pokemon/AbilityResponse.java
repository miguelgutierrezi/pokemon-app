package com.miguel.pokemonapp.web.pokemon;

import com.miguel.pokemonapp.domain.pokemon.AbilityRef;

public record AbilityResponse(String name, boolean hidden) {

  static AbilityResponse from(AbilityRef ability) {
    return new AbilityResponse(ability.name(), ability.hidden());
  }
}
