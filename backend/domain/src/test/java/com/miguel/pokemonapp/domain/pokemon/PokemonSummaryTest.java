package com.miguel.pokemonapp.domain.pokemon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class PokemonSummaryTest {

  @Test
  void assemblesSummaryWithSortedAbilitiesAndDerivedFields() {
    PokemonSummary summary =
        PokemonSummary.assemble(
            new CatalogPokemon(
                1,
                "bulbasaur",
                "https://example.com/sprite.png",
                69,
                List.of(
                    new CatalogAbility(2, "chlorophyll", true),
                    new CatalogAbility(1, "overgrow", false))),
            new CatalogSpecies(
                List.of(new LocalizedText("ja", "フシギダネ"), new LocalizedText("en", "Bulbasaur")),
                List.of(
                    new LocalizedText("en", "Seed Pokémon"),
                    new LocalizedText("fr", "Pokémon Graine"))));

    assertThat(summary.nationalNumber()).isEqualTo(1);
    assertThat(summary.name()).isEqualTo("Bulbasaur");
    assertThat(summary.spriteUrl()).isEqualTo("https://example.com/sprite.png");
    assertThat(summary.category()).isEqualTo("Seed Pokémon");
    assertThat(summary.weightKg()).isEqualTo(new BigDecimal("6.9"));
    assertThat(summary.abilities())
        .containsExactly(new AbilityRef("overgrow", false), new AbilityRef("chlorophyll", true));
  }

  @Test
  void allowsNullSpriteAndNullCategoryWhenEnglishGenusMissing() {
    PokemonSummary summary =
        PokemonSummary.assemble(
            new CatalogPokemon(
                25, "pikachu", null, 60, List.of(new CatalogAbility(1, "static", false))),
            new CatalogSpecies(
                List.of(new LocalizedText("en", "Pikachu")),
                List.of(new LocalizedText("ja", "ねずみポケモン"))));

    assertThat(summary.spriteUrl()).isNull();
    assertThat(summary.category()).isNull();
    assertThat(summary.name()).isEqualTo("Pikachu");
    assertThat(summary.weightKg()).isEqualTo(new BigDecimal("6.0"));
  }

  @Test
  void fallsBackToSlugWhenEnglishNameMissing() {
    PokemonSummary summary =
        PokemonSummary.assemble(
            new CatalogPokemon(122, "mr-mime", null, 545, List.of()),
            new CatalogSpecies(List.of(new LocalizedText("ja", "バリアー")), List.of()));

    assertThat(summary.name()).isEqualTo("mr-mime");
    assertThat(summary.abilities()).isEmpty();
  }

  @Test
  void compactConstructorRejectsNationalNumberLessThanOne() {
    assertThatThrownBy(
            () -> new PokemonSummary(0, "Bulbasaur", null, null, new BigDecimal("6.9"), List.of()))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void compactConstructorRejectsBlankName() {
    assertThatThrownBy(
            () -> new PokemonSummary(1, "  ", null, null, new BigDecimal("6.9"), List.of()))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void compactConstructorRejectsNullWeightKg() {
    assertThatThrownBy(() -> new PokemonSummary(1, "Bulbasaur", null, null, null, List.of()))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void compactConstructorRejectsNullAbilities() {
    assertThatThrownBy(
            () -> new PokemonSummary(1, "Bulbasaur", null, null, new BigDecimal("6.9"), null))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void assembleRejectsNationalNumberLessThanOne() {
    assertThatThrownBy(
            () ->
                PokemonSummary.assemble(
                    new CatalogPokemon(0, "bulbasaur", null, 69, List.of()),
                    new CatalogSpecies(List.of(), List.of())))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void catalogPokemonRejectsNullSlug() {
    assertThatThrownBy(() -> new CatalogPokemon(1, null, null, 69, List.of()))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void catalogPokemonRejectsBlankSlug() {
    assertThatThrownBy(() -> new CatalogPokemon(1, "  ", null, 69, List.of()))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void catalogPokemonRejectsNullAbilitiesList() {
    assertThatThrownBy(() -> new CatalogPokemon(1, "bulbasaur", null, 69, null))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void catalogSpeciesRejectsNullNames() {
    assertThatThrownBy(() -> new CatalogSpecies(null, List.of()))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void catalogSpeciesRejectsNullGenera() {
    assertThatThrownBy(() -> new CatalogSpecies(List.of(), null))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
