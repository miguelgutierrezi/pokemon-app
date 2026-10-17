package com.miguel.pokemonapp.domain.pokemon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class FlavorTextTest {

  @Test
  void normalisesPokeApiNewlinesAndFormFeed() {
    String raw =
        "Its genetic code\nis irregular.\nIt may mutate if\fit is exposed to\nradiation from";
    FlavorText result = FlavorText.from(List.of(new FlavorTextEntry("en", raw, "red")));

    assertThat(result)
        .isEqualTo(
            new FlavorText(
                "Its genetic code is irregular. It may mutate if it is exposed to radiation from",
                "red"));
  }

  @Test
  void normalisesCarriageReturnNewlineDoubleSpacesAndTrims() {
    String raw = "  A seed is\r\nplanted  on its back.  ";
    FlavorText result = FlavorText.from(List.of(new FlavorTextEntry("en", raw, "blue")));

    assertThat(result).isEqualTo(new FlavorText("A seed is planted on its back.", "blue"));
  }

  @Test
  void emptyAfterTrimYieldsNullFlavorText() {
    assertThat(FlavorText.from(List.of(new FlavorTextEntry("en", "  \n\f\r  ", "red")))).isNull();
  }

  @Test
  void noEnglishEntryYieldsNullFlavorText() {
    assertThat(
            FlavorText.from(
                List.of(
                    new FlavorTextEntry("ja", "うまれたときから　せなかに\nタネが　うえてあって", "red"),
                    new FlavorTextEntry("fr", "Il a une graine sur le dos", "red"))))
        .isNull();
  }

  @Test
  void firstEnglishEntryWinsAndPairsItsVersionSlug() {
    FlavorText result =
        FlavorText.from(
            List.of(
                new FlavorTextEntry("ja", "うまれたときから", "red"),
                new FlavorTextEntry(
                    "en", "A strange seed was\nplanted on its\nback at birth.", "red"),
                new FlavorTextEntry(
                    "en", "It can go for days\nwithout eating a\nsingle morsel.", "yellow")));

    assertThat(result)
        .isEqualTo(new FlavorText("A strange seed was planted on its back at birth.", "red"));
  }

  @Test
  void emptyEntriesYieldNullFlavorText() {
    assertThat(FlavorText.from(List.of())).isNull();
  }

  @Test
  void constructorRejectsBlankTextOrNullVersion() {
    assertThatThrownBy(() -> new FlavorText(null, "red"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new FlavorText("  ", "red"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new FlavorText("A seed was planted on its back.", null))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
