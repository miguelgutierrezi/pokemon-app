package com.miguel.pokemonapp.domain.pokemon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class CatalogSpeciesDetailTest {

  @Test
  void toSummaryKeepsOnlyNamesAndGenera() {
    CatalogSpeciesDetail detail = CatalogPokemonDetailTest.bulbasaurSpeciesDetail();
    CatalogSpecies summary = detail.toSummary();

    assertThat(summary.names()).isEqualTo(detail.names());
    assertThat(summary.genera()).isEqualTo(detail.genera());
  }

  @Test
  void rejectsEvolutionChainIdLessThanOne() {
    assertThatThrownBy(
            () -> new CatalogSpeciesDetail(List.of(), List.of(), List.of(), "generation-i", 0))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void listsAreImmutableCopies() {
    List<LocalizedText> names = new ArrayList<>();
    names.add(new LocalizedText("en", "Bulbasaur"));
    List<LocalizedText> genera = new ArrayList<>();
    genera.add(new LocalizedText("en", "Seed Pokémon"));
    List<FlavorTextEntry> flavor = new ArrayList<>();
    flavor.add(new FlavorTextEntry("en", "text", "red"));

    CatalogSpeciesDetail detail =
        new CatalogSpeciesDetail(names, genera, flavor, "generation-i", 1);

    names.clear();
    genera.clear();
    flavor.clear();

    assertThat(detail.names()).hasSize(1);
    assertThat(detail.genera()).hasSize(1);
    assertThat(detail.flavorEntries()).hasSize(1);
    assertThatThrownBy(() -> detail.flavorEntries().add(new FlavorTextEntry("en", "x", "blue")))
        .isInstanceOf(UnsupportedOperationException.class);
  }
}
