package com.miguel.pokemonapp.domain.pokemon;

import static com.miguel.pokemonapp.domain.Require.nonBlank;
import static com.miguel.pokemonapp.domain.Require.nonNull;

import java.util.List;
import java.util.regex.Pattern;

public record FlavorText(String text, String version) {

  private static final Pattern WHITESPACE_RUNS = Pattern.compile("\\s+");

  public FlavorText {
    text = nonBlank(text, "text");
    version = nonNull(version, "version");
  }

  public static FlavorText from(List<FlavorTextEntry> entries) {
    return entries.stream()
        .filter(e -> LanguageCodes.ENGLISH.equals(e.languageCode()))
        .findFirst()
        .map(FlavorText::fromEntry)
        .orElse(null);
  }

  private static FlavorText fromEntry(FlavorTextEntry entry) {
    String normalised = normalise(entry.text());
    if (normalised == null || entry.versionSlug() == null) {
      return null;
    }
    return new FlavorText(normalised, entry.versionSlug());
  }

  private static String normalise(String raw) {
    if (raw == null) {
      return null;
    }
    String collapsed = WHITESPACE_RUNS.matcher(raw).replaceAll(" ").trim();
    return collapsed.isEmpty() ? null : collapsed;
  }
}
