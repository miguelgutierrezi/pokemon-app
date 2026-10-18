package com.miguel.pokemonapp.application.pokemon;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.miguel.pokemonapp.application.LocalPokemonNotFoundException;
import com.miguel.pokemonapp.application.port.LocalPokemonRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DeleteLocalPokemonUseCaseTest {

  private LocalPokemonRepository repository;
  private DeleteLocalPokemonUseCase useCase;

  @BeforeEach
  void setUp() {
    repository = mock(LocalPokemonRepository.class);
    useCase = new DeleteLocalPokemonUseCase(repository);
  }

  @Test
  void absentThrowsLocalPokemonNotFoundWhenDeleteReturnsFalse() {
    when(repository.deleteByNationalNumber(9999)).thenReturn(false);

    assertThatThrownBy(() -> useCase.execute(9999))
        .isInstanceOf(LocalPokemonNotFoundException.class)
        .extracting(ex -> ((LocalPokemonNotFoundException) ex).nationalNumber())
        .isEqualTo(9999);

    verify(repository).deleteByNationalNumber(9999);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void presentDeletesByNationalNumberOnce() {
    when(repository.deleteByNationalNumber(1)).thenReturn(true);

    useCase.execute(1);

    verify(repository).deleteByNationalNumber(1);
    verifyNoMoreInteractions(repository);
  }
}
