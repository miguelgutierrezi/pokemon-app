package com.miguel.pokemonapp.application.pokemon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.miguel.pokemonapp.application.LocalPokemonNotFoundException;
import com.miguel.pokemonapp.application.port.LocalPokemonRepository;
import com.miguel.pokemonapp.domain.pokemon.LocalPokemon;
import com.miguel.pokemonapp.domain.pokemon.Tags;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GetLocalPokemonUseCaseTest {

  private LocalPokemonRepository repository;
  private GetLocalPokemonUseCase useCase;

  @BeforeEach
  void setUp() {
    repository = mock(LocalPokemonRepository.class);
    useCase = new GetLocalPokemonUseCase(repository);
  }

  @Test
  void returnsLocalPokemonWhenPresent() {
    LocalPokemon local = sample(1, "Bulbasaur");
    when(repository.findByNationalNumber(1)).thenReturn(Optional.of(local));

    assertThat(useCase.execute(1)).isEqualTo(local);
  }

  @Test
  void absentThrowsLocalPokemonNotFoundException() {
    when(repository.findByNationalNumber(9999)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> useCase.execute(9999))
        .isInstanceOf(LocalPokemonNotFoundException.class)
        .extracting(ex -> ((LocalPokemonNotFoundException) ex).nationalNumber())
        .isEqualTo(9999);
  }

  private static LocalPokemon sample(int nationalNumber, String name) {
    return new LocalPokemon(
        nationalNumber,
        name,
        "Seed Pokémon",
        List.of("grass", "poison"),
        new BigDecimal("0.7"),
        new BigDecimal("6.9"),
        null,
        "generation-i",
        Instant.parse("2026-01-01T00:00:00Z"),
        null,
        null,
        Tags.empty(),
        0L);
  }
}
