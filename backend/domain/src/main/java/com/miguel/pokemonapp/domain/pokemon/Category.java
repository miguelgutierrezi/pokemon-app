package com.miguel.pokemonapp.domain.pokemon;

import java.util.List;

public final class Category {

  private Category() {}

  public static String from(List<LocalizedText> genera) {
    return genera.stream()
        .filter(g -> LanguageCodes.ENGLISH.equals(g.languageCode()))
        .map(LocalizedText::text)
        .findFirst()
        .orElse(null);
  }
}
