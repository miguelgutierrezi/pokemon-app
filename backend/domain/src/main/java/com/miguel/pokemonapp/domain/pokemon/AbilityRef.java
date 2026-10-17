package com.miguel.pokemonapp.domain.pokemon;

import java.util.Comparator;
import java.util.List;

public record AbilityRef(String name, boolean hidden) {

  public static List<AbilityRef> orderedFrom(List<CatalogAbility> abilities) {
    return abilities.stream()
        .sorted(Comparator.comparingInt(CatalogAbility::slot))
        .map(a -> new AbilityRef(a.name(), a.hidden()))
        .toList();
  }
}
