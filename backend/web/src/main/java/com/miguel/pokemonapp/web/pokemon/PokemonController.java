package com.miguel.pokemonapp.web.pokemon;

import com.miguel.pokemonapp.application.pokemon.ListPokemonUseCase;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/pokemon")
public class PokemonController {

  private final ListPokemonUseCase listPokemonUseCase;

  public PokemonController(ListPokemonUseCase listPokemonUseCase) {
    this.listPokemonUseCase = listPokemonUseCase;
  }

  @GetMapping
  public PokemonPageResponse list(@Valid PokemonListRequest request) {
    return PokemonPageResponse.from(listPokemonUseCase.execute(request.toPageRequest()));
  }
}
