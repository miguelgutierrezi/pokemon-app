package com.miguel.pokemonapp.domain.pokemon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class CatalogPokemonDetailTest {

  @Test
  void toSummaryKeepsFrontDefaultSpriteOnlyAndFeedsPokemonSummaryAssemble() {
    CatalogPokemonDetail detail = bulbasaurPokemonDetail();
    CatalogSpeciesDetail species = bulbasaurSpeciesDetail();

    CatalogPokemon summaryPokemon = detail.toSummary();
    CatalogSpecies summarySpecies = species.toSummary();

    assertThat(summaryPokemon.spriteUrl()).isEqualTo(detail.frontDefaultSprite());
    assertThat(summaryPokemon.spriteUrl()).doesNotContain("official-artwork");

    PokemonSummary summary = PokemonSummary.assemble(summaryPokemon, summarySpecies);
    assertThat(summary.nationalNumber()).isEqualTo(1);
    assertThat(summary.name()).isEqualTo("Bulbasaur");
    assertThat(summary.category()).isEqualTo("Seed Pokémon");
    assertThat(summary.weightKg()).isEqualTo(new BigDecimal("6.9"));
    assertThat(summary.abilities())
        .containsExactly(new AbilityRef("overgrow", false), new AbilityRef("chlorophyll", true));
  }

  @Test
  void abilitiesAndTypesAndStatsListsAreImmutableCopies() {
    List<CatalogAbility> abilities = new ArrayList<>();
    abilities.add(new CatalogAbility(1, "overgrow", false));
    List<CatalogType> types = new ArrayList<>();
    types.add(new CatalogType(1, "grass"));
    List<CatalogStat> stats = new ArrayList<>();
    stats.add(new CatalogStat("hp", 45));

    CatalogPokemonDetail detail =
        new CatalogPokemonDetail(
            1, "bulbasaur", sprite(1), artwork(1), 7, 69, abilities, types, stats);

    abilities.clear();
    types.clear();
    stats.clear();

    assertThat(detail.abilities()).hasSize(1);
    assertThat(detail.types()).hasSize(1);
    assertThat(detail.stats()).hasSize(1);
    assertThatThrownBy(() -> detail.abilities().add(new CatalogAbility(2, "x", false)))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  static CatalogPokemonDetail bulbasaurPokemonDetail() {
    return new CatalogPokemonDetail(
        1,
        "bulbasaur",
        sprite(1),
        artwork(1),
        7,
        69,
        List.of(
            new CatalogAbility(2, "chlorophyll", true), new CatalogAbility(1, "overgrow", false)),
        List.of(new CatalogType(2, "poison"), new CatalogType(1, "grass")),
        List.of(
            new CatalogStat("hp", 45),
            new CatalogStat("attack", 49),
            new CatalogStat("defense", 49),
            new CatalogStat("special-attack", 65),
            new CatalogStat("special-defense", 65),
            new CatalogStat("speed", 45)));
  }

  static CatalogSpeciesDetail bulbasaurSpeciesDetail() {
    return new CatalogSpeciesDetail(
        List.of(new LocalizedText("ja", "フシギダネ"), new LocalizedText("en", "Bulbasaur")),
        List.of(new LocalizedText("en", "Seed Pokémon"), new LocalizedText("fr", "Pokémon Graine")),
        List.of(
            new FlavorTextEntry("en", "A strange seed was\nplanted on its\nback at birth.", "red"),
            new FlavorTextEntry(
                "en", "It can go for days\nwithout eating a\nsingle morsel.", "yellow")),
        "generation-i",
        1);
  }

  private static String sprite(int id) {
    return "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/"
        + id
        + ".png";
  }

  private static String artwork(int id) {
    return "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/"
        + id
        + ".png";
  }
}
