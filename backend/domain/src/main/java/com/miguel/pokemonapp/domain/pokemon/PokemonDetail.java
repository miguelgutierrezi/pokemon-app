package com.miguel.pokemonapp.domain.pokemon;

import static com.miguel.pokemonapp.domain.Require.atLeast;
import static com.miguel.pokemonapp.domain.Require.nonBlank;
import static com.miguel.pokemonapp.domain.Require.nonNull;

import java.math.BigDecimal;
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
    atLeast(nationalNumber, 1, "nationalNumber");
    name = nonBlank(name, "name");
    types = List.copyOf(nonNull(types, "types"));
    heightM = nonNull(heightM, "heightM");
    weightKg = nonNull(weightKg, "weightKg");
    abilities = List.copyOf(nonNull(abilities, "abilities"));
    generation = nonBlank(generation, "generation");
    stats = nonNull(stats, "stats");
    evolution = nonNull(evolution, "evolution");
  }

  public static PokemonDetail assemble(
      CatalogDetailEntry entry,
      CatalogEvolutionChain chain,
      List<CatalogEntry> chainEntries,
      int requestedId) {
    entry = nonNull(entry, "entry");
    chain = nonNull(chain, "chain");
    chainEntries = nonNull(chainEntries, "chainEntries");
    CatalogPokemonDetail pokemon = entry.pokemon();
    CatalogSpeciesDetail species = entry.species();
    if (requestedId != pokemon.nationalNumber()) {
      throw new IllegalArgumentException(
          "requestedId must match detail national number " + pokemon.nationalNumber());
    }

    return new PokemonDetail(
        pokemon.nationalNumber(),
        DisplayName.from(species.names(), pokemon.slug()),
        Category.from(species.genera()),
        CatalogType.namesOrderedBySlot(pokemon.types()),
        HeightM.fromDecimetres(pokemon.heightDecimetres()),
        WeightKg.fromHectograms(pokemon.weightHectograms()),
        AbilityRef.orderedFrom(pokemon.abilities()),
        species.generationSlug(),
        ImageUrl.resolve(pokemon.officialArtworkSprite(), pokemon.frontDefaultSprite()),
        PokemonStats.from(pokemon.stats()),
        FlavorText.from(species.flavorEntries()),
        EvolutionNode.from(chain, chainEntries, requestedId));
  }
}
