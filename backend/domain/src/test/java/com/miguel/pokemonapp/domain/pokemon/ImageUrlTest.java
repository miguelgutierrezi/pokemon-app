package com.miguel.pokemonapp.domain.pokemon;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ImageUrlTest {

  @Test
  void prefersOfficialArtworkOverFrontDefault() {
    assertThat(
            ImageUrl.resolve(
                "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/1.png",
                "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/1.png"))
        .isEqualTo(
            "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/1.png");
  }

  @Test
  void fallsBackToFrontDefaultWhenArtworkAbsent() {
    assertThat(
            ImageUrl.resolve(
                null,
                "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/1.png"))
        .isEqualTo(
            "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/1.png");
  }

  @Test
  void returnsNullWhenBothAbsent() {
    assertThat(ImageUrl.resolve(null, null)).isNull();
  }

  @Test
  void treatsBlankStringsAsAbsent() {
    assertThat(ImageUrl.resolve("  ", "\t")).isNull();
    assertThat(
            ImageUrl.resolve(
                "   ",
                "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/1.png"))
        .isEqualTo(
            "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/1.png");
    assertThat(
            ImageUrl.resolve(
                "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/1.png",
                "  "))
        .isEqualTo(
            "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/1.png");
  }
}
