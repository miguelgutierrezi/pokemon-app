package com.miguel.pokemonapp.web.collection;

import com.miguel.pokemonapp.domain.pokemon.LocalPokemon;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record LocalPokemonResponse(
    int nationalNumber,
    String name,
    String category,
    List<String> types,
    BigDecimal heightM,
    BigDecimal weightKg,
    String imageUrl,
    String generation,
    Instant syncedAt,
    String localizedName,
    String location,
    List<String> tags,
    long version) {

  static LocalPokemonResponse from(LocalPokemon pokemon) {
    return new LocalPokemonResponse(
        pokemon.nationalNumber(),
        pokemon.name(),
        pokemon.category(),
        pokemon.types(),
        pokemon.heightM(),
        pokemon.weightKg(),
        pokemon.imageUrl(),
        pokemon.generation(),
        pokemon.syncedAt(),
        pokemon.localizedName(),
        pokemon.location(),
        pokemon.tags().values(),
        pokemon.version());
  }
}
