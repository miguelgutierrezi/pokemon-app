package com.miguel.pokemonapp.application.pokemon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.miguel.pokemonapp.application.port.LocalPokemonRepository;
import com.miguel.pokemonapp.domain.page.Page;
import com.miguel.pokemonapp.domain.page.PageOutOfRangeException;
import com.miguel.pokemonapp.domain.page.PageRequest;
import com.miguel.pokemonapp.domain.pokemon.LocalPokemon;
import com.miguel.pokemonapp.domain.pokemon.Tags;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ListLocalPokemonUseCaseTest {

  private LocalPokemonRepository repository;
  private ListLocalPokemonUseCase useCase;

  @BeforeEach
  void setUp() {
    repository = mock(LocalPokemonRepository.class);
    useCase = new ListLocalPokemonUseCase(repository);
  }

  @Test
  void returnsPageFromRepository() {
    PageRequest request = new PageRequest(1, 9);
    LocalPokemon bulbasaur = sample(1, "Bulbasaur");
    Page<LocalPokemon> expected = Page.of(List.of(bulbasaur), request, 3L);
    when(repository.findPage(request)).thenReturn(expected);

    Page<LocalPokemon> page = useCase.execute(request);

    assertThat(page.content()).containsExactly(bulbasaur);
    assertThat(page.totalElements()).isEqualTo(3L);
    assertThat(page.totalPages()).isEqualTo(1);
  }

  @Test
  void pageBeyondLastPropagatesPageOutOfRangeException() {
    PageRequest request = new PageRequest(115, 9);
    when(repository.findPage(request))
        .thenThrow(new PageOutOfRangeException("page 115 is beyond last page 1"));

    assertThatThrownBy(() -> useCase.execute(request))
        .isInstanceOf(PageOutOfRangeException.class)
        .hasMessageContaining("beyond last page");
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
