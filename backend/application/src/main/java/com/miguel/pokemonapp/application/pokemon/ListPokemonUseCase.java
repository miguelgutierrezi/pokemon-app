package com.miguel.pokemonapp.application.pokemon;

import com.miguel.pokemonapp.application.port.PokemonCatalogPort;
import com.miguel.pokemonapp.application.port.SpeciesPage;
import com.miguel.pokemonapp.domain.page.Page;
import com.miguel.pokemonapp.domain.page.PageRequest;
import com.miguel.pokemonapp.domain.pokemon.CatalogEntry;
import com.miguel.pokemonapp.domain.pokemon.PokemonSummary;
import java.util.List;

public class ListPokemonUseCase {

  private final PokemonCatalogPort catalog;

  public ListPokemonUseCase(PokemonCatalogPort catalog) {
    this.catalog = catalog;
  }

  public Page<PokemonSummary> execute(PageRequest request) {
    SpeciesPage speciesPage = catalog.speciesPage(request.offset(), request.size());
    request.requireWithin(speciesPage.totalCount());

    List<CatalogEntry> entries = catalog.fetchEntries(speciesPage.nationalIds());
    List<PokemonSummary> content =
        entries.stream()
            .map(entry -> PokemonSummary.assemble(entry.pokemon(), entry.species()))
            .toList();
    return Page.of(content, request, speciesPage.totalCount());
  }
}
