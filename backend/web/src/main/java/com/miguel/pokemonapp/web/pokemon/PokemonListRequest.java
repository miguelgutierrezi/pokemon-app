package com.miguel.pokemonapp.web.pokemon;

import com.miguel.pokemonapp.domain.page.PageRequest;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record PokemonListRequest(
    @Min(1) Integer page, @Min(1) @Max(PageRequest.MAX_SIZE) Integer size) {

  PageRequest toPageRequest() {
    return new PageRequest(page != null ? page : 1, size != null ? size : PageRequest.DEFAULT_SIZE);
  }
}
