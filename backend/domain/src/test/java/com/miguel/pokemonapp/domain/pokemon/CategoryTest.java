package com.miguel.pokemonapp.domain.pokemon;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class CategoryTest {

  @Test
  void prefersEnglishGenusFromGeneraList() {
    List<LocalizedText> genera =
        List.of(
            new LocalizedText("ja", "たねポケモン"),
            new LocalizedText("en", "Seed Pokémon"),
            new LocalizedText("fr", "Pokémon Graine"));

    assertThat(Category.from(genera)).isEqualTo("Seed Pokémon");
  }

  @Test
  void returnsNullWhenEnglishGenusMissing() {
    List<LocalizedText> genera =
        List.of(new LocalizedText("ja", "たねポケモン"), new LocalizedText("fr", "Pokémon Graine"));

    assertThat(Category.from(genera)).isNull();
  }

  @Test
  void returnsNullWhenGeneraEmpty() {
    assertThat(Category.from(List.of())).isNull();
  }

  @Test
  void doesNotGuessFromNonEnglishGenus() {
    List<LocalizedText> genera = List.of(new LocalizedText("roomaji", "Tane Pokémon"));

    assertThat(Category.from(genera)).isNull();
  }
}
