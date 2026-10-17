package com.miguel.pokemonapp.domain.pokemon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class EvolutionNodeTest {

  @Test
  void buildsLinearBulbasaurChainWithLevelUpEdges() {
    CatalogEvolutionChain chain =
        new CatalogEvolutionChain(
            node(
                1,
                List.of(),
                List.of(
                    node(
                        2,
                        List.of(detail("level-up", 16, null)),
                        List.of(node(3, List.of(detail("level-up", 32, null)), List.of()))))));

    EvolutionNode root = EvolutionNode.from(chain, bulbasaurLineEntries(), 1);

    assertThat(root.nationalNumber()).isEqualTo(1);
    assertThat(root.name()).isEqualTo("Bulbasaur");
    assertThat(root.imageUrl()).isEqualTo(sprite(1));
    assertThat(root.isCurrent()).isTrue();
    assertThat(root.children()).hasSize(1);

    EvolutionChild toIvysaur = root.children().get(0);
    assertThat(toIvysaur.link()).isEqualTo(new EvolutionLink("level-up", 16, null));
    assertThat(toIvysaur.node().nationalNumber()).isEqualTo(2);
    assertThat(toIvysaur.node().name()).isEqualTo("Ivysaur");
    assertThat(toIvysaur.node().isCurrent()).isFalse();
    assertThat(toIvysaur.node().children()).hasSize(1);

    EvolutionChild toVenusaur = toIvysaur.node().children().get(0);
    assertThat(toVenusaur.link()).isEqualTo(new EvolutionLink("level-up", 32, null));
    assertThat(toVenusaur.node().nationalNumber()).isEqualTo(3);
    assertThat(toVenusaur.node().name()).isEqualTo("Venusaur");
    assertThat(toVenusaur.node().children()).isEmpty();
  }

  @Test
  void buildsBranchingEeveeChainPreservingChildOrderAndFirstDetailOnly() {
    CatalogEvolutionChain chain = eeveeChain();
    List<CatalogEntry> entries = eeveeLineEntries();

    EvolutionNode root = EvolutionNode.from(chain, entries, 133);

    assertThat(root.nationalNumber()).isEqualTo(133);
    assertThat(root.name()).isEqualTo("Eevee");
    assertThat(root.isCurrent()).isTrue();
    assertThat(root.children()).hasSize(8);

    assertChild(root.children().get(0), 134, "Vaporeon", "use-item", null, "water-stone");
    assertChild(root.children().get(1), 135, "Jolteon", "use-item", null, "thunder-stone");
    assertChild(root.children().get(2), 136, "Flareon", "use-item", null, "fire-stone");
    assertChild(root.children().get(3), 196, "Espeon", "level-up", null, null);
    assertChild(root.children().get(4), 197, "Umbreon", "level-up", null, null);
    assertChild(root.children().get(5), 470, "Leafeon", "level-up", null, null);
    assertChild(root.children().get(6), 471, "Glaceon", "level-up", null, null);
    assertChild(root.children().get(7), 700, "Sylveon", "level-up", null, null);

    assertThat(root.children()).allMatch(c -> !c.node().isCurrent());
    assertThat(root.children()).allMatch(c -> c.node().children().isEmpty());
  }

  @Test
  void marksOnlyRequestedNodeAsCurrentWhenUmbreonRequested() {
    EvolutionNode root = EvolutionNode.from(eeveeChain(), eeveeLineEntries(), 197);

    assertThat(root.isCurrent()).isFalse();
    assertThat(root.children().get(4).node().nationalNumber()).isEqualTo(197);
    assertThat(root.children().get(4).node().isCurrent()).isTrue();
    assertThat(root.children().stream().filter(c -> c.node().isCurrent())).hasSize(1);
  }

  @Test
  void buildsSingleStageTaurosWithNoChildren() {
    CatalogEvolutionChain chain = new CatalogEvolutionChain(node(128, List.of(), List.of()));

    EvolutionNode root = EvolutionNode.from(chain, List.of(entry(128, "tauros", "Tauros")), 128);

    assertThat(root.nationalNumber()).isEqualTo(128);
    assertThat(root.name()).isEqualTo("Tauros");
    assertThat(root.isCurrent()).isTrue();
    assertThat(root.children()).isEmpty();
  }

  @Test
  void nodeImageUrlIsCatalogFrontSpriteNotArtwork() {
    String frontSprite = sprite(1);
    CatalogEntry bulbasaur =
        new CatalogEntry(
            new CatalogPokemon(1, "bulbasaur", frontSprite, 69, List.of()),
            new CatalogSpecies(List.of(new LocalizedText("en", "Bulbasaur")), List.of()));

    EvolutionNode root =
        EvolutionNode.from(
            new CatalogEvolutionChain(node(1, List.of(), List.of())), List.of(bulbasaur), 1);

    assertThat(root.imageUrl()).isEqualTo(frontSprite);
    assertThat(root.imageUrl()).isEqualTo(bulbasaur.pokemon().spriteUrl());
    assertThat(root.imageUrl()).doesNotContain("official-artwork");
  }

  @Test
  void emptyEvolutionDetailsYieldNullLinkFields() {
    CatalogEvolutionChain chain =
        new CatalogEvolutionChain(node(1, List.of(), List.of(node(2, List.of(), List.of()))));

    EvolutionNode root =
        EvolutionNode.from(
            chain, List.of(entry(1, "bulbasaur", "Bulbasaur"), entry(2, "ivysaur", "Ivysaur")), 2);

    EvolutionLink link = root.children().get(0).link();
    assertThat(link.trigger()).isNull();
    assertThat(link.minLevel()).isNull();
    assertThat(link.itemSlug()).isNull();
    assertThat(root.isCurrent()).isFalse();
    assertThat(root.children().get(0).node().isCurrent()).isTrue();
  }

  @Test
  void usesOnlyFirstEvolutionDetailWhenSeveralExist() {
    CatalogEvolutionChain chain =
        new CatalogEvolutionChain(
            node(
                133,
                List.of(),
                List.of(
                    node(
                        470,
                        List.of(
                            detail("level-up", null, null),
                            detail("level-up", null, null),
                            detail("use-item", null, "leaf-stone"),
                            detail("level-up", null, null),
                            detail("level-up", null, null),
                            detail("level-up", null, null)),
                        List.of()))));

    EvolutionNode root =
        EvolutionNode.from(
            chain, List.of(entry(133, "eevee", "Eevee"), entry(470, "leafeon", "Leafeon")), 133);

    assertThat(root.children().get(0).link()).isEqualTo(new EvolutionLink("level-up", null, null));
  }

  @Test
  void rejectsChainNodeWithoutMatchingCatalogEntry() {
    CatalogEvolutionChain chain = new CatalogEvolutionChain(node(128, List.of(), List.of()));

    assertThatThrownBy(() -> EvolutionNode.from(chain, List.of(), 128))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void childrenListIsImmutable() {
    EvolutionNode root =
        EvolutionNode.from(
            new CatalogEvolutionChain(node(128, List.of(), List.of())),
            List.of(entry(128, "tauros", "Tauros")),
            128);

    assertThatThrownBy(() -> root.children().add(null))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  private static void assertChild(
      EvolutionChild child,
      int nationalNumber,
      String name,
      String trigger,
      Integer minLevel,
      String itemSlug) {
    assertThat(child.link()).isEqualTo(new EvolutionLink(trigger, minLevel, itemSlug));
    assertThat(child.node().nationalNumber()).isEqualTo(nationalNumber);
    assertThat(child.node().name()).isEqualTo(name);
    assertThat(child.node().imageUrl()).isEqualTo(sprite(nationalNumber));
  }

  private static CatalogEvolutionChain eeveeChain() {
    return new CatalogEvolutionChain(
        node(
            133,
            List.of(),
            List.of(
                node(134, List.of(detail("use-item", null, "water-stone")), List.of()),
                node(135, List.of(detail("use-item", null, "thunder-stone")), List.of()),
                node(136, List.of(detail("use-item", null, "fire-stone")), List.of()),
                node(196, List.of(detail("level-up", null, null)), List.of()),
                node(197, List.of(detail("level-up", null, null)), List.of()),
                node(
                    470,
                    List.of(
                        detail("level-up", null, null),
                        detail("level-up", null, null),
                        detail("use-item", null, "leaf-stone"),
                        detail("level-up", null, null),
                        detail("level-up", null, null),
                        detail("level-up", null, null)),
                    List.of()),
                node(
                    471,
                    List.of(
                        detail("level-up", null, null),
                        detail("level-up", null, null),
                        detail("use-item", null, "ice-stone"),
                        detail("level-up", null, null),
                        detail("level-up", null, null),
                        detail("level-up", null, null)),
                    List.of()),
                node(
                    700,
                    List.of(detail("level-up", null, null), detail("level-up", null, null)),
                    List.of()))));
  }

  private static List<CatalogEntry> bulbasaurLineEntries() {
    return List.of(
        entry(1, "bulbasaur", "Bulbasaur"),
        entry(2, "ivysaur", "Ivysaur"),
        entry(3, "venusaur", "Venusaur"));
  }

  private static List<CatalogEntry> eeveeLineEntries() {
    return List.of(
        entry(133, "eevee", "Eevee"),
        entry(134, "vaporeon", "Vaporeon"),
        entry(135, "jolteon", "Jolteon"),
        entry(136, "flareon", "Flareon"),
        entry(196, "espeon", "Espeon"),
        entry(197, "umbreon", "Umbreon"),
        entry(470, "leafeon", "Leafeon"),
        entry(471, "glaceon", "Glaceon"),
        entry(700, "sylveon", "Sylveon"));
  }

  private static CatalogEvolutionNode node(
      int speciesId, List<CatalogEvolutionDetail> details, List<CatalogEvolutionNode> children) {
    return new CatalogEvolutionNode(speciesId, details, children);
  }

  private static CatalogEvolutionDetail detail(String trigger, Integer minLevel, String item) {
    return new CatalogEvolutionDetail(trigger, minLevel, item);
  }

  private static CatalogEntry entry(int id, String slug, String englishName) {
    return new CatalogEntry(
        new CatalogPokemon(id, slug, sprite(id), 100, List.of()),
        new CatalogSpecies(List.of(new LocalizedText("en", englishName)), List.of()));
  }

  private static String sprite(int id) {
    return "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/"
        + id
        + ".png";
  }
}
