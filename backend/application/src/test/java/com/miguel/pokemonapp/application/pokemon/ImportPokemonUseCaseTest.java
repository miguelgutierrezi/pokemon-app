package com.miguel.pokemonapp.application.pokemon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.miguel.pokemonapp.application.CatalogUnavailableException;
import com.miguel.pokemonapp.application.DuplicateLocalPokemonException;
import com.miguel.pokemonapp.application.PokemonNotFoundException;
import com.miguel.pokemonapp.application.port.LocalPokemonRepository;
import com.miguel.pokemonapp.application.port.PokemonCatalogPort;
import com.miguel.pokemonapp.domain.pokemon.CatalogAbility;
import com.miguel.pokemonapp.domain.pokemon.CatalogDetailEntry;
import com.miguel.pokemonapp.domain.pokemon.CatalogPokemonDetail;
import com.miguel.pokemonapp.domain.pokemon.CatalogSpeciesDetail;
import com.miguel.pokemonapp.domain.pokemon.CatalogStat;
import com.miguel.pokemonapp.domain.pokemon.CatalogType;
import com.miguel.pokemonapp.domain.pokemon.FlavorTextEntry;
import com.miguel.pokemonapp.domain.pokemon.LocalPokemon;
import com.miguel.pokemonapp.domain.pokemon.LocalizedText;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ImportPokemonUseCaseTest {

  private static final Instant SYNCED_AT = Instant.parse("2026-06-15T12:00:00Z");

  private LocalPokemonRepository repository;
  private PokemonCatalogPort catalog;
  private ImportPokemonUseCase useCase;

  @BeforeEach
  void setUp() {
    repository = mock(LocalPokemonRepository.class);
    catalog = mock(PokemonCatalogPort.class);
    Clock clock = Clock.fixed(SYNCED_AT, ZoneOffset.UTC);
    useCase = new ImportPokemonUseCase(repository, catalog, clock);
  }

  @Test
  void whenAlreadyLocalThrowsDuplicateWithoutCallingCatalog() {
    when(repository.existsByNationalNumber(25)).thenReturn(true);

    assertThatThrownBy(() -> useCase.execute(25))
        .isInstanceOf(DuplicateLocalPokemonException.class);

    verifyNoInteractions(catalog);
    verify(repository, never()).create(any());
  }

  @Test
  void importsViaFetchDetailOnlyThenSavesSnapshotWithClockInstant() {
    CatalogDetailEntry entry = bulbasaurEntry();
    when(repository.existsByNationalNumber(1)).thenReturn(false);
    when(catalog.fetchDetail(1)).thenReturn(entry);
    when(repository.create(any(LocalPokemon.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    LocalPokemon saved = useCase.execute(1);

    assertThat(saved.nationalNumber()).isEqualTo(1);
    assertThat(saved.name()).isEqualTo("Bulbasaur");
    assertThat(saved.types()).containsExactly("grass", "poison");
    assertThat(saved.heightM()).isEqualTo(new BigDecimal("0.7"));
    assertThat(saved.weightKg()).isEqualTo(new BigDecimal("6.9"));
    assertThat(saved.syncedAt()).isEqualTo(SYNCED_AT);
    assertThat(saved.localizedName()).isNull();
    assertThat(saved.location()).isNull();
    assertThat(saved.tags().values()).isEmpty();

    verify(catalog).fetchDetail(1);
    verify(catalog, never()).fetchEvolutionChain(anyInt());
    verify(catalog, never()).fetchEntries(anyList());
    verify(repository).create(any(LocalPokemon.class));
  }

  @Test
  void propagatesPokemonNotFoundAndDoesNotSave() {
    when(repository.existsByNationalNumber(9999)).thenReturn(false);
    when(catalog.fetchDetail(9999)).thenThrow(new PokemonNotFoundException(9999, "not found"));

    assertThatThrownBy(() -> useCase.execute(9999))
        .isInstanceOf(PokemonNotFoundException.class)
        .extracting(ex -> ((PokemonNotFoundException) ex).nationalId())
        .isEqualTo(9999);

    verify(repository, never()).create(any());
  }

  @Test
  void propagatesCatalogUnavailableAndDoesNotSave() {
    when(repository.existsByNationalNumber(1)).thenReturn(false);
    when(catalog.fetchDetail(1)).thenThrow(new CatalogUnavailableException("down"));

    assertThatThrownBy(() -> useCase.execute(1))
        .isInstanceOf(CatalogUnavailableException.class)
        .hasMessage("down");

    verify(repository, never()).create(any());
  }

  @Test
  void wrapsIllegalArgumentExceptionFromSnapshotAsCatalogUnavailableAndDoesNotSave() {
    when(repository.existsByNationalNumber(1)).thenReturn(false);
    when(catalog.fetchDetail(1)).thenReturn(malformedHeightEntry());

    assertThatThrownBy(() -> useCase.execute(1))
        .isInstanceOf(CatalogUnavailableException.class)
        .hasMessage("malformed catalog data");

    verify(repository, never()).create(any());
  }

  @Test
  void propagatesIllegalArgumentExceptionFromCreateUnchanged() {
    when(repository.existsByNationalNumber(1)).thenReturn(false);
    when(catalog.fetchDetail(1)).thenReturn(bulbasaurEntry());
    when(repository.create(any(LocalPokemon.class)))
        .thenThrow(new IllegalArgumentException("persistence rejected"));

    assertThatThrownBy(() -> useCase.execute(1))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("persistence rejected")
        .isNotInstanceOf(CatalogUnavailableException.class);
  }

  private static CatalogDetailEntry bulbasaurEntry() {
    return new CatalogDetailEntry(
        new CatalogPokemonDetail(
            1,
            "bulbasaur",
            "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/1.png",
            "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/1.png",
            7,
            69,
            List.of(new CatalogAbility(1, "overgrow", false)),
            List.of(new CatalogType(1, "grass"), new CatalogType(2, "poison")),
            List.of(new CatalogStat("hp", 45))),
        new CatalogSpeciesDetail(
            List.of(new LocalizedText("en", "Bulbasaur")),
            List.of(new LocalizedText("en", "Seed Pokémon")),
            List.of(new FlavorTextEntry("en", "A seed.", "red")),
            "generation-i",
            1));
  }

  private static CatalogDetailEntry malformedHeightEntry() {
    return new CatalogDetailEntry(
        new CatalogPokemonDetail(
            1,
            "bulbasaur",
            null,
            null,
            -1,
            69,
            List.of(),
            List.of(new CatalogType(1, "grass")),
            List.of()),
        new CatalogSpeciesDetail(
            List.of(new LocalizedText("en", "Bulbasaur")),
            List.of(),
            List.of(),
            "generation-i",
            1));
  }
}
