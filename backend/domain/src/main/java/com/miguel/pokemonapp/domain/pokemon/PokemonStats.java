package com.miguel.pokemonapp.domain.pokemon;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record PokemonStats(
    int hp, int attack, int defense, int specialAttack, int specialDefense, int speed) {

  private static final String HP = "hp";
  private static final String ATTACK = "attack";
  private static final String DEFENSE = "defense";
  private static final String SPECIAL_ATTACK = "special-attack";
  private static final String SPECIAL_DEFENSE = "special-defense";
  private static final String SPEED = "speed";
  private static final Set<String> REQUIRED =
      Set.of(HP, ATTACK, DEFENSE, SPECIAL_ATTACK, SPECIAL_DEFENSE, SPEED);

  public PokemonStats {
    requireNonNegative(hp, "hp");
    requireNonNegative(attack, "attack");
    requireNonNegative(defense, "defense");
    requireNonNegative(specialAttack, "specialAttack");
    requireNonNegative(specialDefense, "specialDefense");
    requireNonNegative(speed, "speed");
  }

  public int total() {
    return hp + attack + defense + specialAttack + specialDefense + speed;
  }

  public static PokemonStats from(List<CatalogStat> stats) {
    if (stats == null) {
      throw new IllegalArgumentException("stats must not be null");
    }
    Map<String, Integer> byName = new HashMap<>();
    for (CatalogStat stat : stats) {
      if (stat == null || stat.name() == null) {
        continue;
      }
      if (REQUIRED.contains(stat.name())) {
        byName.putIfAbsent(stat.name(), stat.baseStat());
      }
    }
    if (!byName.keySet().containsAll(REQUIRED)) {
      throw new IllegalArgumentException("catalog stats must include all six base stats");
    }
    return new PokemonStats(
        byName.get(HP),
        byName.get(ATTACK),
        byName.get(DEFENSE),
        byName.get(SPECIAL_ATTACK),
        byName.get(SPECIAL_DEFENSE),
        byName.get(SPEED));
  }

  private static void requireNonNegative(int value, String field) {
    if (value < 0) {
      throw new IllegalArgumentException(field + " must be non-negative");
    }
  }
}
