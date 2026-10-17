package com.miguel.pokemonapp.domain.pokemon;

import static com.miguel.pokemonapp.domain.Require.atLeast;
import static com.miguel.pokemonapp.domain.Require.nonBlank;
import static com.miguel.pokemonapp.domain.Require.nonNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record LocalPokemon(
    int nationalNumber,
    String name,
    String category,
    List<String> types,
    BigDecimal heightM,
    BigDecimal weightKg,
    String imageUrl,
    String generation,
    Instant syncedAt,
    String localizedName,
    String location,
    Tags tags) {

  public static final int MAX_LOCALIZED_NAME_LENGTH = 100;
  public static final int MAX_LOCATION_LENGTH = 200;

  public LocalPokemon {
    atLeast(nationalNumber, 1, "nationalNumber");
    name = nonBlank(name, "name");
    types = List.copyOf(nonNull(types, "types"));
    heightM = nonNull(heightM, "heightM");
    weightKg = nonNull(weightKg, "weightKg");
    generation = nonBlank(generation, "generation");
    syncedAt = nonNull(syncedAt, "syncedAt");
    localizedName = optionalText(localizedName, MAX_LOCALIZED_NAME_LENGTH, "localizedName");
    location = optionalText(location, MAX_LOCATION_LENGTH, "location");
    tags = nonNull(tags, "tags");
  }

  public static LocalPokemon snapshotOf(CatalogDetailEntry entry, Instant syncedAt) {
    entry = nonNull(entry, "entry");
    syncedAt = nonNull(syncedAt, "syncedAt");
    CatalogPokemonDetail pokemon = entry.pokemon();
    CatalogSpeciesDetail species = entry.species();

    return new LocalPokemon(
        pokemon.nationalNumber(),
        DisplayName.from(species.names(), pokemon.slug()),
        Category.from(species.genera()),
        CatalogType.namesOrderedBySlot(pokemon.types()),
        HeightM.fromDecimetres(pokemon.heightDecimetres()),
        WeightKg.fromHectograms(pokemon.weightHectograms()),
        ImageUrl.resolve(pokemon.officialArtworkSprite(), pokemon.frontDefaultSprite()),
        species.generationSlug(),
        syncedAt,
        null,
        null,
        Tags.empty());
  }

  private static String optionalText(String value, int maxLength, String field) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    if (trimmed.isEmpty()) {
      return null;
    }
    if (trimmed.length() > maxLength) {
      throw new IllegalArgumentException(field + " length must be <= " + maxLength);
    }
    return trimmed;
  }
}
