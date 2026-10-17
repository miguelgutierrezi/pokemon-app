package com.miguel.pokemonapp.application.port;

import java.util.List;

public record SpeciesPage(List<Integer> nationalIds, long totalCount) {

  public SpeciesPage {
    if (nationalIds == null) {
      throw new IllegalArgumentException("nationalIds must not be null");
    }
    if (totalCount < 0) {
      throw new IllegalArgumentException("totalCount must be >= 0");
    }
    nationalIds = List.copyOf(nationalIds);
  }
}
