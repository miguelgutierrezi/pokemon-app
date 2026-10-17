package com.miguel.pokemonapp.domain.pokemon;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

public record PokemonSummary(
    int nationalNumber,
    String name,
    String spriteUrl,
    String category,
    BigDecimal weightKg,
    List<AbilityRef> abilities) {

  public PokemonSummary {
    if (nationalNumber < 1) {
      throw new IllegalArgumentException("nationalNumber must be >= 1");
    }
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("name must be non-blank");
    }
    if (weightKg == null) {
      throw new IllegalArgumentException("weightKg must not be null");
    }
    if (abilities == null) {
      throw new IllegalArgumentException("abilities must not be null");
    }
    abilities = List.copyOf(abilities);
  }

  public static PokemonSummary assemble(CatalogPokemon pokemon, CatalogSpecies species) {
    List<AbilityRef> ordered =
        pokemon.abilities().stream()
            .sorted(Comparator.comparingInt(CatalogAbility::slot))
            .map(a -> new AbilityRef(a.name(), a.hidden()))
            .toList();

    return new PokemonSummary(
        pokemon.nationalNumber(),
        DisplayName.from(species.names(), pokemon.slug()),
        pokemon.spriteUrl(),
        Category.from(species.genera()),
        WeightKg.fromHectograms(pokemon.weightHectograms()),
        ordered);
  }
}
