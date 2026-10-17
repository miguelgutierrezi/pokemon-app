package com.miguel.pokemonapp.domain.pokemon;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class DisplayNameTest {

  @Test
  void prefersEnglishNameFromNamesList() {
    List<LocalizedText> names =
        List.of(
            new LocalizedText("ja", "バリアー"),
            new LocalizedText("en", "Mr. Mime"),
            new LocalizedText("fr", "M. Mime"));

    assertThat(DisplayName.from(names, "mr-mime")).isEqualTo("Mr. Mime");
  }

  @Test
  void fallsBackToResourceSlugWhenEnglishMissing() {
    List<LocalizedText> names =
        List.of(new LocalizedText("ja", "バリアー"), new LocalizedText("fr", "M. Mime"));

    assertThat(DisplayName.from(names, "mr-mime")).isEqualTo("mr-mime");
  }

  @Test
  void fallsBackToResourceSlugWhenNamesEmpty() {
    assertThat(DisplayName.from(List.of(), "farfetchd")).isEqualTo("farfetchd");
  }

  @Test
  void ignoresNonEnglishEvenWhenFirst() {
    List<LocalizedText> names =
        List.of(new LocalizedText("roomaji", "Barrierd"), new LocalizedText("en", "Mr. Mime"));

    assertThat(DisplayName.from(names, "mr-mime")).isEqualTo("Mr. Mime");
  }
}
