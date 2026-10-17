package com.miguel.pokemonapp.domain.pokemon;

import static com.miguel.pokemonapp.domain.Require.nonBlank;

import java.util.Comparator;
import java.util.List;

public record CatalogType(int slot, String name) {

  public CatalogType {
    name = nonBlank(name, "name");
  }

  public static List<String> namesOrderedBySlot(List<CatalogType> types) {
    return types.stream()
        .sorted(Comparator.comparingInt(CatalogType::slot))
        .map(CatalogType::name)
        .toList();
  }
}
