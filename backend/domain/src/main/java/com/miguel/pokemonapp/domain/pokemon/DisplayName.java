package com.miguel.pokemonapp.domain.pokemon;

import java.util.List;

public final class DisplayName {

  private DisplayName() {}

  public static String from(List<LocalizedText> names, String slug) {
    return names.stream()
        .filter(n -> LanguageCodes.ENGLISH.equals(n.languageCode()))
        .map(LocalizedText::text)
        .findFirst()
        .orElse(slug);
  }
}
