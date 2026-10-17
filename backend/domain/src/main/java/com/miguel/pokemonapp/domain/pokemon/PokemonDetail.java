package com.miguel.pokemonapp.domain.pokemon;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

public record PokemonDetail(
    int nationalNumber,
    String name,
    String category,
    List<String> types,
    BigDecimal heightM,
    BigDecimal weightKg,
    List<AbilityRef> abilities,
    String generation,
    String imageUrl,
    PokemonStats stats,
    FlavorText flavorText,
    EvolutionNode evolution) {

  public PokemonDetail {
    if (nationalNumber < 1) {
      throw new IllegalArgumentException("nationalNumber must be >= 1");
    }
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("name must be non-blank");
    }
    if (types == null) {
      throw new IllegalArgumentException("types must not be null");
    }
    if (heightM == null) {
      throw new IllegalArgumentException("heightM must not be null");
    }
    if (weightKg == null) {
      throw new IllegalArgumentException("weightKg must not be null");
    }
    if (abilities == null) {
      throw new IllegalArgumentException("abilities must not be null");
    }
    if (generation == null || generation.isBlank()) {
      throw new IllegalArgumentException("generation must be non-blank");
    }
    if (stats == null) {
      throw new IllegalArgumentException("stats must not be null");
    }
    if (evolution == null) {
      throw new IllegalArgumentException("evolution must not be null");
    }
    types = List.copyOf(types);
    abilities = List.copyOf(abilities);
  }

  public static PokemonDetail assemble(
      CatalogDetailEntry entry,
      CatalogEvolutionChain chain,
      List<CatalogEntry> chainEntries,
      int requestedId) {
    if (entry == null) {
      throw new IllegalArgumentException("entry must not be null");
    }
    if (chain == null) {
      throw new IllegalArgumentException("chain must not be null");
    }
    if (chainEntries == null) {
      throw new IllegalArgumentException("chainEntries must not be null");
    }
    CatalogPokemonDetail pokemon = entry.pokemon();
    CatalogSpeciesDetail species = entry.species();
    if (requestedId != pokemon.nationalNumber()) {
      throw new IllegalArgumentException(
          "requestedId must match detail national number " + pokemon.nationalNumber());
    }

    List<AbilityRef> orderedAbilities =
        pokemon.abilities().stream()
            .sorted(Comparator.comparingInt(CatalogAbility::slot))
            .map(a -> new AbilityRef(a.name(), a.hidden()))
            .toList();

    List<String> orderedTypes =
        pokemon.types().stream()
            .sorted(Comparator.comparingInt(CatalogType::slot))
            .map(CatalogType::name)
            .toList();

    return new PokemonDetail(
        pokemon.nationalNumber(),
        DisplayName.from(species.names(), pokemon.slug()),
        Category.from(species.genera()),
        orderedTypes,
        HeightM.fromDecimetres(pokemon.heightDecimetres()),
        WeightKg.fromHectograms(pokemon.weightHectograms()),
        orderedAbilities,
        species.generationSlug(),
        ImageUrl.resolve(pokemon.officialArtworkSprite(), pokemon.frontDefaultSprite()),
        PokemonStats.from(pokemon.stats()),
        FlavorText.from(species.flavorEntries()),
        EvolutionNode.from(chain, chainEntries, requestedId));
  }
}
