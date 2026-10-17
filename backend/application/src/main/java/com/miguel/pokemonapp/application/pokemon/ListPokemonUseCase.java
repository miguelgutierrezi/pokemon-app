package com.miguel.pokemonapp.application.pokemon;

import com.miguel.pokemonapp.application.port.PokemonCatalogPort;
import com.miguel.pokemonapp.application.port.SpeciesPage;
import com.miguel.pokemonapp.domain.page.Page;
import com.miguel.pokemonapp.domain.page.PageOutOfRangeException;
import com.miguel.pokemonapp.domain.page.PageRequest;
import com.miguel.pokemonapp.domain.pokemon.PokemonSummary;
import java.util.ArrayList;
import java.util.List;

public class ListPokemonUseCase {

  private final PokemonCatalogPort catalog;

  public ListPokemonUseCase(PokemonCatalogPort catalog) {
    this.catalog = catalog;
  }

  public Page<PokemonSummary> execute(PageRequest request) {
    SpeciesPage speciesPage = catalog.speciesPage(request.offset(), request.size());
    rejectIfPageBeyondLast(request, speciesPage.totalCount());

    List<PokemonSummary> content = new ArrayList<>(speciesPage.nationalIds().size());
    for (int nationalId : speciesPage.nationalIds()) {
      content.add(
          PokemonSummary.assemble(
              catalog.fetchPokemon(nationalId), catalog.fetchSpecies(nationalId)));
    }
    return Page.of(content, request, speciesPage.totalCount());
  }

  private static void rejectIfPageBeyondLast(PageRequest request, long totalElements) {
    if (totalElements == 0) {
      return;
    }
    int totalPages = Math.toIntExact(Math.ceilDiv(totalElements, request.size()));
    if (request.page() > totalPages) {
      throw new PageOutOfRangeException(
          "page " + request.page() + " is beyond last page " + totalPages);
    }
  }
}
