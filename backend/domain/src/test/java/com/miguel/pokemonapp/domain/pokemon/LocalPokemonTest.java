package com.miguel.pokemonapp.domain.pokemon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class LocalPokemonTest {

  private static final Instant SYNCED_AT = Instant.parse("2026-01-01T00:00:00Z");
  private static final String ARTWORK_1 =
      "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/1.png";

  @Test
  void snapshotOfMapsBulbasaurCatalogDetailAndLeavesProprietaryEmpty() {
    CatalogDetailEntry entry =
        new CatalogDetailEntry(
            CatalogPokemonDetailTest.bulbasaurPokemonDetail(),
            CatalogPokemonDetailTest.bulbasaurSpeciesDetail());

    LocalPokemon local = LocalPokemon.snapshotOf(entry, SYNCED_AT);

    assertThat(local.nationalNumber()).isEqualTo(1);
    assertThat(local.name()).isEqualTo("Bulbasaur");
    assertThat(local.category()).isEqualTo("Seed Pokémon");
    assertThat(local.types()).containsExactly("grass", "poison");
    assertThat(local.heightM()).isEqualTo(new BigDecimal("0.7"));
    assertThat(local.weightKg()).isEqualTo(new BigDecimal("6.9"));
    assertThat(local.imageUrl()).isEqualTo(ARTWORK_1);
    assertThat(local.generation()).isEqualTo("generation-i");
    assertThat(local.syncedAt()).isEqualTo(SYNCED_AT);
    assertThat(local.localizedName()).isNull();
    assertThat(local.location()).isNull();
    assertThat(local.tags().values()).isEmpty();
    assertThat(local.version()).isZero();
  }

  @Test
  void snapshotOfOrdersTypesBySlotNotListOrder() {
    CatalogPokemonDetail pokemon =
        new CatalogPokemonDetail(
            1,
            "bulbasaur",
            "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/1.png",
            ARTWORK_1,
            7,
            69,
            List.of(new CatalogAbility(1, "overgrow", false)),
            List.of(new CatalogType(2, "poison"), new CatalogType(1, "grass")),
            List.of(new CatalogStat("hp", 45)));
    CatalogDetailEntry entry =
        new CatalogDetailEntry(pokemon, CatalogPokemonDetailTest.bulbasaurSpeciesDetail());

    assertThat(LocalPokemon.snapshotOf(entry, SYNCED_AT).types())
        .containsExactly("grass", "poison");
  }

  @Test
  void snapshotOfPrefersOfficialArtworkThenFrontSpriteThenNull() {
    CatalogSpeciesDetail species = CatalogPokemonDetailTest.bulbasaurSpeciesDetail();

    CatalogPokemonDetail artwork =
        new CatalogPokemonDetail(
            1,
            "bulbasaur",
            "https://example.com/front.png",
            ARTWORK_1,
            7,
            69,
            List.of(),
            List.of(new CatalogType(1, "grass")),
            List.of());
    assertThat(
            LocalPokemon.snapshotOf(new CatalogDetailEntry(artwork, species), SYNCED_AT).imageUrl())
        .isEqualTo(ARTWORK_1);

    CatalogPokemonDetail frontOnly =
        new CatalogPokemonDetail(
            1,
            "bulbasaur",
            "https://example.com/front.png",
            null,
            7,
            69,
            List.of(),
            List.of(new CatalogType(1, "grass")),
            List.of());
    assertThat(
            LocalPokemon.snapshotOf(new CatalogDetailEntry(frontOnly, species), SYNCED_AT)
                .imageUrl())
        .isEqualTo("https://example.com/front.png");

    CatalogPokemonDetail neither =
        new CatalogPokemonDetail(
            1,
            "bulbasaur",
            null,
            null,
            7,
            69,
            List.of(),
            List.of(new CatalogType(1, "grass")),
            List.of());
    assertThat(
            LocalPokemon.snapshotOf(new CatalogDetailEntry(neither, species), SYNCED_AT).imageUrl())
        .isNull();
  }

  @Test
  void blankLocalizedNameAndLocationBecomeNull() {
    LocalPokemon local =
        new LocalPokemon(
            1,
            "Bulbasaur",
            "Seed Pokémon",
            List.of("grass", "poison"),
            new BigDecimal("0.7"),
            new BigDecimal("6.9"),
            ARTWORK_1,
            "generation-i",
            SYNCED_AT,
            "   ",
            "  ",
            Tags.empty(),
            0L);

    assertThat(local.localizedName()).isNull();
    assertThat(local.location()).isNull();
  }

  @Test
  void trimsProprietaryFieldsAndEnforcesLengthLimits() {
    String name100 = "n".repeat(100);
    String location200 = "l".repeat(200);

    LocalPokemon ok =
        new LocalPokemon(
            1,
            "Bulbasaur",
            null,
            List.of("grass"),
            new BigDecimal("0.7"),
            new BigDecimal("6.9"),
            null,
            "generation-i",
            SYNCED_AT,
            "  " + name100 + "  ",
            "  " + location200 + "  ",
            Tags.of(List.of("forest")),
            0L);

    assertThat(ok.localizedName()).isEqualTo(name100);
    assertThat(ok.location()).isEqualTo(location200);
    assertThat(ok.category()).isNull();

    assertThatThrownBy(
            () ->
                new LocalPokemon(
                    1,
                    "Bulbasaur",
                    null,
                    List.of("grass"),
                    new BigDecimal("0.7"),
                    new BigDecimal("6.9"),
                    null,
                    "generation-i",
                    SYNCED_AT,
                    "x".repeat(101),
                    null,
                    Tags.empty(),
                    0L))
        .isInstanceOf(IllegalArgumentException.class);

    assertThatThrownBy(
            () ->
                new LocalPokemon(
                    1,
                    "Bulbasaur",
                    null,
                    List.of("grass"),
                    new BigDecimal("0.7"),
                    new BigDecimal("6.9"),
                    null,
                    "generation-i",
                    SYNCED_AT,
                    null,
                    "y".repeat(201),
                    Tags.empty(),
                    0L))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void withProprietaryUpdatesProprietaryFieldsPreservesSnapshotAndVersion() {
    LocalPokemon original =
        new LocalPokemon(
            1,
            "Bulbasaur",
            "Seed Pokémon",
            List.of("grass", "poison"),
            new BigDecimal("0.7"),
            new BigDecimal("6.9"),
            ARTWORK_1,
            "generation-i",
            SYNCED_AT,
            "old",
            "pallet",
            Tags.of(List.of("starter")),
            3L);

    LocalPokemon updated =
        original.withProprietary(
            "  My Name  ", "  Route 1  ", Tags.of(List.of(" Forest ", "FOREST", "grass")));

    assertThat(updated).isNotSameAs(original);
    assertThat(updated.localizedName()).isEqualTo("My Name");
    assertThat(updated.location()).isEqualTo("Route 1");
    assertThat(updated.tags().values()).containsExactly("forest", "grass");
    assertThat(updated.version()).isEqualTo(3L);
    assertThat(updated.nationalNumber()).isEqualTo(original.nationalNumber());
    assertThat(updated.name()).isEqualTo(original.name());
    assertThat(updated.category()).isEqualTo(original.category());
    assertThat(updated.types()).isEqualTo(original.types());
    assertThat(updated.heightM()).isEqualTo(original.heightM());
    assertThat(updated.weightKg()).isEqualTo(original.weightKg());
    assertThat(updated.imageUrl()).isEqualTo(original.imageUrl());
    assertThat(updated.generation()).isEqualTo(original.generation());
    assertThat(updated.syncedAt()).isEqualTo(original.syncedAt());
  }

  @Test
  void withProprietaryBlankTextClearsToNullAndKeepsVersion() {
    LocalPokemon original =
        new LocalPokemon(
            1,
            "Bulbasaur",
            "Seed Pokémon",
            List.of("grass"),
            new BigDecimal("0.7"),
            new BigDecimal("6.9"),
            ARTWORK_1,
            "generation-i",
            SYNCED_AT,
            "named",
            "somewhere",
            Tags.of(List.of("a")),
            1L);

    LocalPokemon cleared = original.withProprietary("   ", "  ", Tags.empty());

    assertThat(cleared.localizedName()).isNull();
    assertThat(cleared.location()).isNull();
    assertThat(cleared.tags().values()).isEmpty();
    assertThat(cleared.version()).isEqualTo(1L);
    assertThat(cleared.name()).isEqualTo("Bulbasaur");
  }

  @Test
  void rejectsNegativeVersion() {
    assertThatThrownBy(
            () ->
                new LocalPokemon(
                    1,
                    "Bulbasaur",
                    null,
                    List.of("grass"),
                    new BigDecimal("0.7"),
                    new BigDecimal("6.9"),
                    null,
                    "generation-i",
                    SYNCED_AT,
                    null,
                    null,
                    Tags.empty(),
                    -1L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("version");
  }

  @Test
  void rejectsInvalidCoreInvariants() {
    assertThatThrownBy(
            () ->
                new LocalPokemon(
                    0,
                    "Bulbasaur",
                    null,
                    List.of("grass"),
                    new BigDecimal("0.7"),
                    new BigDecimal("6.9"),
                    null,
                    "generation-i",
                    SYNCED_AT,
                    null,
                    null,
                    Tags.empty(),
                    0L))
        .isInstanceOf(IllegalArgumentException.class);

    assertThatThrownBy(
            () ->
                LocalPokemon.snapshotOf(
                    new CatalogDetailEntry(
                        CatalogPokemonDetailTest.bulbasaurPokemonDetail(),
                        CatalogPokemonDetailTest.bulbasaurSpeciesDetail()),
                    null))
        .isInstanceOf(IllegalArgumentException.class);

    assertThatThrownBy(
            () ->
                new LocalPokemon(
                    1,
                    "Bulbasaur",
                    null,
                    null,
                    new BigDecimal("0.7"),
                    new BigDecimal("6.9"),
                    null,
                    "generation-i",
                    SYNCED_AT,
                    null,
                    null,
                    Tags.empty(),
                    0L))
        .isInstanceOf(IllegalArgumentException.class);

    assertThatThrownBy(
            () ->
                new LocalPokemon(
                    1,
                    "Bulbasaur",
                    null,
                    List.of("grass"),
                    new BigDecimal("0.7"),
                    new BigDecimal("6.9"),
                    null,
                    "generation-i",
                    SYNCED_AT,
                    null,
                    null,
                    null,
                    0L))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
