package com.miguel.pokemonapp.domain.pokemon;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class CatalogEntryTest {

  @Test
  void rejectsNullPokemon() {
    assertThatThrownBy(() -> new CatalogEntry(null, new CatalogSpecies(List.of(), List.of())))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsNullSpecies() {
    assertThatThrownBy(
            () -> new CatalogEntry(new CatalogPokemon(1, "bulbasaur", null, 69, List.of()), null))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
