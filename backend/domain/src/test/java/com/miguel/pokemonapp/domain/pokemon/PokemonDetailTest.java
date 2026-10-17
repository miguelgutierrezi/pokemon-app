package com.miguel.pokemonapp.domain.pokemon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class PokemonDetailTest {

  @Test
  void assemblesBulbasaurDetailFromCatalogFixtures() {
    CatalogPokemonDetail pokemon = CatalogPokemonDetailTest.bulbasaurPokemonDetail();
    CatalogSpeciesDetail species = CatalogPokemonDetailTest.bulbasaurSpeciesDetail();
    CatalogDetailEntry entry = new CatalogDetailEntry(pokemon, species);

    CatalogEvolutionChain chain =
        new CatalogEvolutionChain(
            new CatalogEvolutionNode(
                1,
                List.of(),
                List.of(
                    new CatalogEvolutionNode(
                        2,
                        List.of(new CatalogEvolutionDetail("level-up", 16, null)),
                        List.of(
                            new CatalogEvolutionNode(
                                3,
                                List.of(new CatalogEvolutionDetail("level-up", 32, null)),
                                List.of()))))));

    List<CatalogEntry> chainEntries =
        List.of(
            new CatalogEntry(pokemon.toSummary(), species.toSummary()),
            entry(2, "ivysaur", "Ivysaur"),
            entry(3, "venusaur", "Venusaur"));

    PokemonDetail detail = PokemonDetail.assemble(entry, chain, chainEntries, 1);

    assertThat(detail.nationalNumber()).isEqualTo(1);
    assertThat(detail.name()).isEqualTo("Bulbasaur");
    assertThat(detail.category()).isEqualTo("Seed Pokémon");
    assertThat(detail.types()).containsExactly("grass", "poison");
    assertThat(detail.heightM()).isEqualTo(new BigDecimal("0.7"));
    assertThat(detail.weightKg()).isEqualTo(new BigDecimal("6.9"));
    assertThat(detail.abilities())
        .containsExactly(new AbilityRef("overgrow", false), new AbilityRef("chlorophyll", true));
    assertThat(detail.generation()).isEqualTo("generation-i");
    assertThat(detail.imageUrl()).isEqualTo(pokemon.officialArtworkSprite());
    assertThat(detail.stats().total()).isEqualTo(318);
    assertThat(detail.flavorText())
        .isEqualTo(new FlavorText("A strange seed was planted on its back at birth.", "red"));
    assertThat(detail.evolution().isCurrent()).isTrue();
    assertThat(detail.evolution().nationalNumber()).isEqualTo(1);
    assertThat(detail.evolution().imageUrl()).isEqualTo(pokemon.frontDefaultSprite());
    assertThat(detail.evolution().children()).hasSize(1);
    assertThat(detail.evolution().children().get(0).node().nationalNumber()).isEqualTo(2);
  }

  @Test
  void imageUrlFallsBackToFrontSpriteThenNull() {
    CatalogPokemonDetail withSpriteOnly =
        new CatalogPokemonDetail(
            1,
            "bulbasaur",
            sprite(1),
            null,
            7,
            69,
            List.of(new CatalogAbility(1, "overgrow", false)),
            List.of(new CatalogType(1, "grass")),
            bulbasaurStats());
    CatalogSpeciesDetail species = CatalogPokemonDetailTest.bulbasaurSpeciesDetail();
    CatalogDetailEntry entry = new CatalogDetailEntry(withSpriteOnly, species);
    CatalogEvolutionChain chain =
        new CatalogEvolutionChain(new CatalogEvolutionNode(1, List.of(), List.of()));

    assertThat(
            PokemonDetail.assemble(
                    entry,
                    chain,
                    List.of(new CatalogEntry(withSpriteOnly.toSummary(), species.toSummary())),
                    1)
                .imageUrl())
        .isEqualTo(sprite(1));

    CatalogPokemonDetail withNeither =
        new CatalogPokemonDetail(
            1,
            "bulbasaur",
            null,
            null,
            7,
            69,
            List.of(new CatalogAbility(1, "overgrow", false)),
            List.of(new CatalogType(1, "grass")),
            bulbasaurStats());
    assertThat(
            PokemonDetail.assemble(
                    new CatalogDetailEntry(withNeither, species),
                    chain,
                    List.of(new CatalogEntry(withNeither.toSummary(), species.toSummary())),
                    1)
                .imageUrl())
        .isNull();
  }

  @Test
  void flavorTextMayBeNull() {
    CatalogSpeciesDetail noEnglish =
        new CatalogSpeciesDetail(
            List.of(new LocalizedText("en", "Bulbasaur")),
            List.of(new LocalizedText("en", "Seed Pokémon")),
            List.of(new FlavorTextEntry("ja", "うまれたときから", "red")),
            "generation-i",
            1);
    CatalogPokemonDetail pokemon = CatalogPokemonDetailTest.bulbasaurPokemonDetail();
    CatalogDetailEntry entry = new CatalogDetailEntry(pokemon, noEnglish);
    CatalogEvolutionChain chain =
        new CatalogEvolutionChain(new CatalogEvolutionNode(1, List.of(), List.of()));

    PokemonDetail detail =
        PokemonDetail.assemble(
            entry, chain, List.of(new CatalogEntry(pokemon.toSummary(), noEnglish.toSummary())), 1);

    assertThat(detail.flavorText()).isNull();
  }

  @Test
  void rejectsRequestedIdDifferentFromDetailNationalNumber() {
    CatalogDetailEntry entry =
        new CatalogDetailEntry(
            CatalogPokemonDetailTest.bulbasaurPokemonDetail(),
            CatalogPokemonDetailTest.bulbasaurSpeciesDetail());
    CatalogEvolutionChain chain =
        new CatalogEvolutionChain(new CatalogEvolutionNode(1, List.of(), List.of()));

    assertThatThrownBy(() -> PokemonDetail.assemble(entry, chain, List.of(), 2))
        .isInstanceOf(IllegalArgumentException.class);
  }

  private static List<CatalogStat> bulbasaurStats() {
    return List.of(
        new CatalogStat("hp", 45),
        new CatalogStat("attack", 49),
        new CatalogStat("defense", 49),
        new CatalogStat("special-attack", 65),
        new CatalogStat("special-defense", 65),
        new CatalogStat("speed", 45));
  }

  private static CatalogEntry entry(int id, String slug, String englishName) {
    return new CatalogEntry(
        new CatalogPokemon(id, slug, sprite(id), 100, List.of()),
        new CatalogSpecies(List.of(new LocalizedText("en", englishName)), List.of()));
  }

  private static String sprite(int id) {
    return "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/"
        + id
        + ".png";
  }
}
