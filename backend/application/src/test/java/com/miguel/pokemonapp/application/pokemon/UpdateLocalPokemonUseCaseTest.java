package com.miguel.pokemonapp.application.pokemon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.miguel.pokemonapp.application.LocalPokemonNotFoundException;
import com.miguel.pokemonapp.application.StaleLocalPokemonException;
import com.miguel.pokemonapp.application.port.LocalPokemonRepository;
import com.miguel.pokemonapp.domain.pokemon.LocalPokemon;
import com.miguel.pokemonapp.domain.pokemon.Tags;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class UpdateLocalPokemonUseCaseTest {

  private static final Instant SYNCED_AT = Instant.parse("2026-01-01T00:00:00Z");

  private LocalPokemonRepository repository;
  private UpdateLocalPokemonUseCase useCase;

  @BeforeEach
  void setUp() {
    repository = mock(LocalPokemonRepository.class);
    useCase = new UpdateLocalPokemonUseCase(repository);
  }

  @Test
  void absentThrowsLocalPokemonNotFoundAndDoesNotSave() {
    when(repository.findByNationalNumber(9999)).thenReturn(Optional.empty());

    ProprietaryUpdate update = new ProprietaryUpdate("n", "l", List.of("t"), 0L);

    assertThatThrownBy(() -> useCase.execute(9999, update))
        .isInstanceOf(LocalPokemonNotFoundException.class)
        .extracting(ex -> ((LocalPokemonNotFoundException) ex).nationalNumber())
        .isEqualTo(9999);

    verify(repository, never()).save(any());
  }

  @Test
  void staleVersionThrowsWithoutCallingSave() {
    LocalPokemon current = sample(1, "Bulbasaur", 2L);
    when(repository.findByNationalNumber(1)).thenReturn(Optional.of(current));

    ProprietaryUpdate update = new ProprietaryUpdate("new-name", "new-loc", List.of("tag"), 1L);

    assertThatThrownBy(() -> useCase.execute(1, update))
        .isInstanceOf(StaleLocalPokemonException.class)
        .extracting(ex -> ((StaleLocalPokemonException) ex).nationalNumber())
        .isEqualTo(1);

    verify(repository, never()).save(any());
  }

  @Test
  void matchingVersionSavesWithProprietaryFieldsAndSameSnapshot() {
    LocalPokemon current = sample(1, "Bulbasaur", 2L);
    when(repository.findByNationalNumber(1)).thenReturn(Optional.of(current));
    when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    ProprietaryUpdate update =
        new ProprietaryUpdate("  My Name  ", "  Route 1  ", List.of(" Forest ", "FOREST"), 2L);

    LocalPokemon result = useCase.execute(1, update);

    ArgumentCaptor<LocalPokemon> captor = ArgumentCaptor.forClass(LocalPokemon.class);
    verify(repository).save(captor.capture());
    LocalPokemon saved = captor.getValue();

    assertThat(saved.localizedName()).isEqualTo("My Name");
    assertThat(saved.location()).isEqualTo("Route 1");
    assertThat(saved.tags().values()).containsExactly("forest");
    assertThat(saved.version()).isEqualTo(2L);
    assertThat(saved.nationalNumber()).isEqualTo(current.nationalNumber());
    assertThat(saved.name()).isEqualTo(current.name());
    assertThat(saved.category()).isEqualTo(current.category());
    assertThat(saved.types()).isEqualTo(current.types());
    assertThat(saved.heightM()).isEqualTo(current.heightM());
    assertThat(saved.weightKg()).isEqualTo(current.weightKg());
    assertThat(saved.imageUrl()).isEqualTo(current.imageUrl());
    assertThat(saved.generation()).isEqualTo(current.generation());
    assertThat(saved.syncedAt()).isEqualTo(current.syncedAt());
    assertThat(result).isEqualTo(saved);
  }

  @Test
  void invalidTagsPropagateIllegalArgumentExceptionWithoutCatalogUnavailable() {
    LocalPokemon current = sample(1, "Bulbasaur", 0L);
    when(repository.findByNationalNumber(1)).thenReturn(Optional.of(current));

    ProprietaryUpdate update = new ProprietaryUpdate(null, null, List.of("  "), 0L);

    assertThatThrownBy(() -> useCase.execute(1, update))
        .isInstanceOf(IllegalArgumentException.class);

    verify(repository, never()).save(any());
  }

  private static LocalPokemon sample(int nationalNumber, String name, long version) {
    return new LocalPokemon(
        nationalNumber,
        name,
        "Seed Pokémon",
        List.of("grass", "poison"),
        new BigDecimal("0.7"),
        new BigDecimal("6.9"),
        null,
        "generation-i",
        SYNCED_AT,
        null,
        null,
        Tags.empty(),
        version);
  }
}
