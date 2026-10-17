package com.miguel.pokemonapp.domain.pokemon;

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
    if (nationalNumber < 1) {
      throw new IllegalArgumentException("nationalNumber must be >= 1");
    }
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("name must be non-blank");
    }
    if (children == null) {
      throw new IllegalArgumentException("children must not be null");
    }
    children = List.copyOf(children);
  }

  public static EvolutionNode from(
      CatalogEvolutionChain chain, List<CatalogEntry> entries, int requestedId) {
    if (chain == null) {
      throw new IllegalArgumentException("chain must not be null");
    }
    if (entries == null) {
      throw new IllegalArgumentException("entries must not be null");
    }
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
