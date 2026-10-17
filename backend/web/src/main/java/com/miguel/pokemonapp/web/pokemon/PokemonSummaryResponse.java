package com.miguel.pokemonapp.web.pokemon;

import com.miguel.pokemonapp.domain.pokemon.PokemonSummary;
import java.math.BigDecimal;
import java.util.List;

public record PokemonSummaryResponse(
    int nationalNumber,
    String name,
    String spriteUrl,
    String category,
    BigDecimal weightKg,
    List<AbilityResponse> abilities) {

  static PokemonSummaryResponse from(PokemonSummary summary) {
    return new PokemonSummaryResponse(
        summary.nationalNumber(),
        summary.name(),
        summary.spriteUrl(),
        summary.category(),
        summary.weightKg(),
        summary.abilities().stream().map(AbilityResponse::from).toList());
  }
}
