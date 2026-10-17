package com.miguel.pokemonapp.web.pokemon;

import com.miguel.pokemonapp.domain.pokemon.PokemonStats;

public record PokemonStatsResponse(
    int hp, int attack, int defense, int specialAttack, int specialDefense, int speed, int total) {

  static PokemonStatsResponse from(PokemonStats stats) {
    return new PokemonStatsResponse(
        stats.hp(),
        stats.attack(),
        stats.defense(),
        stats.specialAttack(),
        stats.specialDefense(),
        stats.speed(),
        stats.total());
  }
}
