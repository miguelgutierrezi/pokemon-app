package com.miguel.pokemonapp.web.pokemon;

import com.miguel.pokemonapp.domain.pokemon.PokemonDetail;
import java.math.BigDecimal;
import java.util.List;

public record PokemonDetailResponse(
    int nationalNumber,
    String name,
    String category,
    List<String> types,
    BigDecimal heightM,
    BigDecimal weightKg,
    List<AbilityResponse> abilities,
    String generation,
    String imageUrl,
    PokemonStatsResponse stats,
    FlavorTextResponse flavorText,
    EvolutionNodeResponse evolution) {

  static PokemonDetailResponse from(PokemonDetail detail) {
    return new PokemonDetailResponse(
        detail.nationalNumber(),
        detail.name(),
        detail.category(),
        detail.types(),
        detail.heightM(),
        detail.weightKg(),
        detail.abilities().stream().map(AbilityResponse::from).toList(),
        detail.generation(),
        detail.imageUrl(),
        PokemonStatsResponse.from(detail.stats()),
        FlavorTextResponse.from(detail.flavorText()),
        EvolutionNodeResponse.from(detail.evolution()));
  }
}
