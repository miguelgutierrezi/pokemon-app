package com.miguel.pokemonapp.domain.pokemon;

import static com.miguel.pokemonapp.domain.Require.nonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class Tags {

  public static final int MAX_TAG_LENGTH = 32;
  public static final int MAX_COUNT = 20;

  private static final Tags EMPTY = new Tags(List.of());

  private final List<String> values;

  private Tags(List<String> values) {
    this.values = List.copyOf(values);
  }

  public static Tags empty() {
    return EMPTY;
  }

  public static Tags of(Collection<String> raw) {
    nonNull(raw, "tags");
    if (raw.isEmpty()) {
      return EMPTY;
    }

    Set<String> normalised = new LinkedHashSet<>();
    for (String tag : raw) {
      nonNull(tag, "tag");
      String trimmed = tag.trim();
      if (trimmed.isEmpty()) {
        throw new IllegalArgumentException("tag must be non-blank");
      }
      String lower = trimmed.toLowerCase(Locale.ROOT);
      if (lower.length() > MAX_TAG_LENGTH) {
        throw new IllegalArgumentException(
            "tag length must be <= " + MAX_TAG_LENGTH + " after normalisation");
      }
      normalised.add(lower);
    }
    if (normalised.size() > MAX_COUNT) {
      throw new IllegalArgumentException("tags must have at most " + MAX_COUNT + " values");
    }
    return new Tags(new ArrayList<>(normalised));
  }

  public List<String> values() {
    return values;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof Tags other)) {
      return false;
    }
    return values.equals(other.values);
  }

  @Override
  public int hashCode() {
    return values.hashCode();
  }

  @Override
  public String toString() {
    return "Tags" + values;
  }
}
