package com.miguel.pokemonapp.domain.pokemon;

import static com.miguel.pokemonapp.domain.Require.atLeast;
import static com.miguel.pokemonapp.domain.Require.nonBlank;
import static com.miguel.pokemonapp.domain.Require.nonNull;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public record EvolutionNode(
    int nationalNumber,
    String name,
    String imageUrl,
    boolean isCurrent,
    List<EvolutionChild> children) {

  public EvolutionNode {
    atLeast(nationalNumber, 1, "nationalNumber");
    name = nonBlank(name, "name");
    children = List.copyOf(nonNull(children, "children"));
  }

  public static EvolutionNode from(
      CatalogEvolutionChain chain, List<CatalogEntry> entries, int requestedId) {
    chain = nonNull(chain, "chain");
    entries = nonNull(entries, "entries");
    Map<Integer, CatalogEntry> byId =
        entries.stream()
            .collect(
                Collectors.toMap(
                    e -> e.pokemon().nationalNumber(), Function.identity(), (a, b) -> a));
    return buildNode(chain.root(), byId, requestedId);
  }

  private static EvolutionNode buildNode(
      CatalogEvolutionNode catalogNode, Map<Integer, CatalogEntry> byId, int requestedId) {
    CatalogEntry entry = byId.get(catalogNode.speciesId());
    if (entry == null) {
      throw new IllegalArgumentException(
          "missing catalog entry for species id " + catalogNode.speciesId());
    }
    List<EvolutionChild> children =
        catalogNode.children().stream().map(child -> toChild(child, byId, requestedId)).toList();
    return new EvolutionNode(
        catalogNode.speciesId(),
        DisplayName.from(entry.species().names(), entry.pokemon().slug()),
        entry.pokemon().spriteUrl(),
        catalogNode.speciesId() == requestedId,
        children);
  }

  private static EvolutionChild toChild(
      CatalogEvolutionNode catalogChild, Map<Integer, CatalogEntry> byId, int requestedId) {
    return new EvolutionChild(
        linkFrom(catalogChild.evolutionDetails()), buildNode(catalogChild, byId, requestedId));
  }

  private static EvolutionLink linkFrom(List<CatalogEvolutionDetail> details) {
    if (details.isEmpty()) {
      return new EvolutionLink(null, null, null);
    }
    CatalogEvolutionDetail first = details.getFirst();
    return new EvolutionLink(first.triggerSlug(), first.minLevel(), first.itemSlug());
  }
}
