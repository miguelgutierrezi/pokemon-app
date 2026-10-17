package com.miguel.pokemonapp.domain.pokemon;

import static com.miguel.pokemonapp.domain.Require.atLeast;
import static com.miguel.pokemonapp.domain.Require.nonBlank;
import static com.miguel.pokemonapp.domain.Require.nonNull;

import java.math.BigDecimal;
import java.util.List;

public record PokemonSummary(
    int nationalNumber,
    String name,
    String spriteUrl,
    String category,
    BigDecimal weightKg,
    List<AbilityRef> abilities) {

  public PokemonSummary {
    atLeast(nationalNumber, 1, "nationalNumber");
    name = nonBlank(name, "name");
    weightKg = nonNull(weightKg, "weightKg");
    abilities = List.copyOf(nonNull(abilities, "abilities"));
  }

  public static PokemonSummary assemble(CatalogPokemon pokemon, CatalogSpecies species) {
    return new PokemonSummary(
        pokemon.nationalNumber(),
        DisplayName.from(species.names(), pokemon.slug()),
        pokemon.spriteUrl(),
        Category.from(species.genera()),
        WeightKg.fromHectograms(pokemon.weightHectograms()),
        AbilityRef.orderedFrom(pokemon.abilities()));
  }
}
