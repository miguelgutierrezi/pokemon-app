package com.miguel.pokemonapp.application.pokemon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.miguel.pokemonapp.application.CatalogUnavailableException;
import com.miguel.pokemonapp.application.PokemonNotFoundException;
import com.miguel.pokemonapp.application.port.PokemonCatalogPort;
import com.miguel.pokemonapp.domain.pokemon.CatalogAbility;
import com.miguel.pokemonapp.domain.pokemon.CatalogDetailEntry;
import com.miguel.pokemonapp.domain.pokemon.CatalogEntry;
import com.miguel.pokemonapp.domain.pokemon.CatalogEvolutionChain;
import com.miguel.pokemonapp.domain.pokemon.CatalogEvolutionDetail;
import com.miguel.pokemonapp.domain.pokemon.CatalogEvolutionNode;
import com.miguel.pokemonapp.domain.pokemon.CatalogPokemon;
import com.miguel.pokemonapp.domain.pokemon.CatalogPokemonDetail;
import com.miguel.pokemonapp.domain.pokemon.CatalogSpecies;
import com.miguel.pokemonapp.domain.pokemon.CatalogSpeciesDetail;
import com.miguel.pokemonapp.domain.pokemon.CatalogStat;
import com.miguel.pokemonapp.domain.pokemon.CatalogType;
import com.miguel.pokemonapp.domain.pokemon.FlavorTextEntry;
import com.miguel.pokemonapp.domain.pokemon.LocalizedText;
import com.miguel.pokemonapp.domain.pokemon.PokemonDetail;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

class GetPokemonDetailUseCaseTest {

  private PokemonCatalogPort catalog;
  private GetPokemonDetailUseCase useCase;

  @BeforeEach
  void setUp() {
    catalog = mock(PokemonCatalogPort.class);
    useCase = new GetPokemonDetailUseCase(catalog);
  }

  @Test
  void executesThreePortCallsInOrderAndAssemblesDetail() {
    CatalogDetailEntry detailEntry = bulbasaurDetailEntry();
    CatalogEvolutionChain chain = bulbasaurChain();
    List<Integer> expectedIds = List.of(1, 2, 3);
    List<CatalogEntry> entries =
        List.of(
            entry(1, "bulbasaur", "Bulbasaur"),
            entry(2, "ivysaur", "Ivysaur"),
            entry(3, "venusaur", "Venusaur"));

    when(catalog.fetchDetail(1)).thenReturn(detailEntry);
    when(catalog.fetchEvolutionChain(1)).thenReturn(chain);
    when(catalog.fetchEntries(expectedIds)).thenReturn(entries);

    PokemonDetail detail = useCase.execute(1);

    assertThat(detail.nationalNumber()).isEqualTo(1);
    assertThat(detail.name()).isEqualTo("Bulbasaur");
    assertThat(detail.evolution().children()).hasSize(1);

    InOrder order = inOrder(catalog);
    order.verify(catalog).fetchDetail(1);
    order.verify(catalog).fetchEvolutionChain(1);
    order.verify(catalog).fetchEntries(expectedIds);
  }

  @Test
  void passesDepthFirstDeduplicatedSpeciesIdsToFetchEntries() {
    CatalogDetailEntry detailEntry = bulbasaurDetailEntry();
    CatalogEvolutionChain chainWithDup =
        new CatalogEvolutionChain(
            new CatalogEvolutionNode(
                1,
                List.of(),
                List.of(
                    new CatalogEvolutionNode(
                        2,
                        List.of(new CatalogEvolutionDetail("level-up", 16, null)),
                        List.of(
                            new CatalogEvolutionNode(
                                1,
                                List.of(new CatalogEvolutionDetail("level-up", 1, null)),
                                List.of()),
                            new CatalogEvolutionNode(
                                3,
                                List.of(new CatalogEvolutionDetail("level-up", 32, null)),
                                List.of()))))));

    when(catalog.fetchDetail(1)).thenReturn(detailEntry);
    when(catalog.fetchEvolutionChain(1)).thenReturn(chainWithDup);
    when(catalog.fetchEntries(List.of(1, 2, 3)))
        .thenReturn(
            List.of(
                entry(1, "bulbasaur", "Bulbasaur"),
                entry(2, "ivysaur", "Ivysaur"),
                entry(3, "venusaur", "Venusaur")));

    useCase.execute(1);

    verify(catalog).fetchEntries(List.of(1, 2, 3));
  }

  @Test
  void propagatesPokemonNotFoundExceptionFromFetchDetail() {
    when(catalog.fetchDetail(9999)).thenThrow(new PokemonNotFoundException("missing"));

    assertThatThrownBy(() -> useCase.execute(9999))
        .isInstanceOf(PokemonNotFoundException.class)
        .hasMessage("missing");

    verify(catalog, never()).fetchEvolutionChain(anyInt());
    verify(catalog, never()).fetchEntries(anyList());
  }

  @Test
  void propagatesCatalogUnavailableExceptionFromPort() {
    when(catalog.fetchDetail(1)).thenThrow(new CatalogUnavailableException("down"));

    assertThatThrownBy(() -> useCase.execute(1))
        .isInstanceOf(CatalogUnavailableException.class)
        .hasMessage("down");

    verify(catalog, never()).fetchEvolutionChain(anyInt());
  }

  @Test
  void translatesIllegalArgumentExceptionDuringAssembleToCatalogUnavailable() {
    CatalogPokemonDetail pokemon = bulbasaurPokemonDetail();
    CatalogSpeciesDetail species = bulbasaurSpeciesDetail();
    when(catalog.fetchDetail(1)).thenReturn(new CatalogDetailEntry(pokemon, species));
    when(catalog.fetchEvolutionChain(1))
        .thenReturn(new CatalogEvolutionChain(new CatalogEvolutionNode(1, List.of(), List.of())));
    when(catalog.fetchEntries(List.of(1))).thenReturn(List.of());

    assertThatThrownBy(() -> useCase.execute(1))
        .isInstanceOf(CatalogUnavailableException.class)
        .hasMessage("malformed catalog data")
        .hasCauseInstanceOf(IllegalArgumentException.class);

    verify(catalog).fetchEntries(List.of(1));
  }

  @Test
  void rejectsIdLessThanOneBeforeAnyPortCall() {
    assertThatThrownBy(() -> useCase.execute(0)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> useCase.execute(-1)).isInstanceOf(IllegalArgumentException.class);

    verify(catalog, never()).fetchDetail(anyInt());
    verify(catalog, never()).fetchEvolutionChain(anyInt());
    verify(catalog, never()).fetchEntries(anyList());
  }

  private static CatalogDetailEntry bulbasaurDetailEntry() {
    return new CatalogDetailEntry(bulbasaurPokemonDetail(), bulbasaurSpeciesDetail());
  }

  private static CatalogPokemonDetail bulbasaurPokemonDetail() {
    return new CatalogPokemonDetail(
        1,
        "bulbasaur",
        "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/1.png",
        "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/1.png",
        7,
        69,
        List.of(
            new CatalogAbility(2, "chlorophyll", true), new CatalogAbility(1, "overgrow", false)),
        List.of(new CatalogType(2, "poison"), new CatalogType(1, "grass")),
        List.of(
            new CatalogStat("hp", 45),
            new CatalogStat("attack", 49),
            new CatalogStat("defense", 49),
            new CatalogStat("special-attack", 65),
            new CatalogStat("special-defense", 65),
            new CatalogStat("speed", 45)));
  }

  private static CatalogSpeciesDetail bulbasaurSpeciesDetail() {
    return new CatalogSpeciesDetail(
        List.of(new LocalizedText("en", "Bulbasaur")),
        List.of(new LocalizedText("en", "Seed Pokémon")),
        List.of(
            new FlavorTextEntry("en", "A strange seed was planted on its back at birth.", "red")),
        "generation-i",
        1);
  }

  private static CatalogEvolutionChain bulbasaurChain() {
    return new CatalogEvolutionChain(
        new CatalogEvolutionNode(
            1,
            List.of(),
            List.of(
                new CatalogEvolutionNode(
                    2,
                    List.of(new CatalogEvolutionDetail("level-up", 16, null)),
                    List.of(
                        new CatalogEvolutionNode(
                            3,
                            List.of(new CatalogEvolutionDetail("level-up", 32, null)),
                            List.of()))))));
  }

  private static CatalogEntry entry(int id, String slug, String englishName) {
    return new CatalogEntry(
        new CatalogPokemon(id, slug, null, 100, List.of()),
        new CatalogSpecies(List.of(new LocalizedText("en", englishName)), List.of()));
  }
}
