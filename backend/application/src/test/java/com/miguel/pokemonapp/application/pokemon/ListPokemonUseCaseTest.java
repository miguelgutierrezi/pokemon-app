package com.miguel.pokemonapp.application.pokemon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.miguel.pokemonapp.application.CatalogUnavailableException;
import com.miguel.pokemonapp.application.port.PokemonCatalogPort;
import com.miguel.pokemonapp.application.port.SpeciesPage;
import com.miguel.pokemonapp.domain.page.Page;
import com.miguel.pokemonapp.domain.page.PageOutOfRangeException;
import com.miguel.pokemonapp.domain.page.PageRequest;
import com.miguel.pokemonapp.domain.pokemon.CatalogAbility;
import com.miguel.pokemonapp.domain.pokemon.CatalogPokemon;
import com.miguel.pokemonapp.domain.pokemon.CatalogSpecies;
import com.miguel.pokemonapp.domain.pokemon.LocalizedText;
import com.miguel.pokemonapp.domain.pokemon.PokemonSummary;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ListPokemonUseCaseTest {

  private PokemonCatalogPort catalog;
  private ListPokemonUseCase useCase;

  @BeforeEach
  void setUp() {
    catalog = mock(PokemonCatalogPort.class);
    useCase = new ListPokemonUseCase(catalog);
  }

  @Test
  void returnsPageWithSummariesInSpeciesPageOrder() {
    when(catalog.speciesPage(0, 9)).thenReturn(new SpeciesPage(List.of(2, 1), 1025L));
    when(catalog.fetchPokemon(2)).thenReturn(pokemon(2, "ivysaur", 130));
    when(catalog.fetchSpecies(2)).thenReturn(species("Ivysaur", "Seed Pokémon"));
    when(catalog.fetchPokemon(1)).thenReturn(pokemon(1, "bulbasaur", 69));
    when(catalog.fetchSpecies(1)).thenReturn(species("Bulbasaur", "Seed Pokémon"));

    Page<PokemonSummary> page = useCase.execute(new PageRequest(1, 9));

    assertThat(page.content()).extracting(PokemonSummary::nationalNumber).containsExactly(2, 1);
    assertThat(page.content())
        .extracting(PokemonSummary::name)
        .containsExactly("Ivysaur", "Bulbasaur");
    assertThat(page.totalElements()).isEqualTo(1025L);
    assertThat(page.totalPages()).isEqualTo(114);
    assertThat(page.page()).isEqualTo(1);
    assertThat(page.size()).isEqualTo(9);
  }

  @Test
  void pageBeyondLastThrowsWithoutFetchingDetails() {
    when(catalog.speciesPage(1026, 9)).thenReturn(new SpeciesPage(List.of(), 1025L));

    assertThatThrownBy(() -> useCase.execute(new PageRequest(115, 9)))
        .isInstanceOf(PageOutOfRangeException.class);

    verify(catalog, never()).fetchPokemon(anyInt());
    verify(catalog, never()).fetchSpecies(anyInt());
  }

  @Test
  void propagatesCatalogUnavailableExceptionFromPort() {
    when(catalog.speciesPage(0, 9)).thenThrow(new CatalogUnavailableException("down"));

    assertThatThrownBy(() -> useCase.execute(new PageRequest(1, 9)))
        .isInstanceOf(CatalogUnavailableException.class)
        .hasMessage("down");

    verify(catalog, never()).fetchPokemon(anyInt());
    verify(catalog, never()).fetchSpecies(anyInt());
  }

  private static CatalogPokemon pokemon(int id, String slug, int hectograms) {
    return new CatalogPokemon(
        id, slug, null, hectograms, List.of(new CatalogAbility(1, "overgrow", false)));
  }

  private static CatalogSpecies species(String englishName, String englishGenus) {
    return new CatalogSpecies(
        List.of(new LocalizedText("en", englishName)),
        List.of(new LocalizedText("en", englishGenus)));
  }
}
