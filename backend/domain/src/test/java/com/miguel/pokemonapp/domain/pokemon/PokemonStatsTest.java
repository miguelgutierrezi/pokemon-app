package com.miguel.pokemonapp.domain.pokemon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class PokemonStatsTest {

  @Test
  void mapsBulbasaurCatalogStatsAndComputesTotal() {
    PokemonStats stats =
        PokemonStats.from(
            List.of(
                new CatalogStat("hp", 45),
                new CatalogStat("attack", 49),
                new CatalogStat("defense", 49),
                new CatalogStat("special-attack", 65),
                new CatalogStat("special-defense", 65),
                new CatalogStat("speed", 45)));

    assertThat(stats.hp()).isEqualTo(45);
    assertThat(stats.attack()).isEqualTo(49);
    assertThat(stats.defense()).isEqualTo(49);
    assertThat(stats.specialAttack()).isEqualTo(65);
    assertThat(stats.specialDefense()).isEqualTo(65);
    assertThat(stats.speed()).isEqualTo(45);
    assertThat(stats.total()).isEqualTo(318);
  }

  @Test
  void ignoresUnknownExtraStatNames() {
    PokemonStats stats =
        PokemonStats.from(
            List.of(
                new CatalogStat("hp", 45),
                new CatalogStat("attack", 49),
                new CatalogStat("defense", 49),
                new CatalogStat("special-attack", 65),
                new CatalogStat("special-defense", 65),
                new CatalogStat("speed", 45),
                new CatalogStat("accuracy", 100)));

    assertThat(stats.total()).isEqualTo(318);
  }

  @Test
  void rejectsMissingRequiredStat() {
    assertThatThrownBy(
            () ->
                PokemonStats.from(
                    List.of(
                        new CatalogStat("hp", 45),
                        new CatalogStat("attack", 49),
                        new CatalogStat("defense", 49),
                        new CatalogStat("special-attack", 65),
                        new CatalogStat("speed", 45))))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsUnknownOnlyCatalogStats() {
    assertThatThrownBy(
            () ->
                PokemonStats.from(
                    List.of(new CatalogStat("accuracy", 100), new CatalogStat("evasion", 100))))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsNegativeBases() {
    assertThatThrownBy(() -> new PokemonStats(45, 49, 49, 65, 65, -1))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void allowsZeroBases() {
    PokemonStats stats = new PokemonStats(0, 0, 0, 0, 0, 0);
    assertThat(stats.total()).isEqualTo(0);
  }
}
