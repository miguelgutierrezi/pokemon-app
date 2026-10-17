package com.miguel.pokemonapp.web.pokemon;

import com.miguel.pokemonapp.application.pokemon.ListPokemonUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
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
  @Operation(summary = "List Pokémon (National Dex order)")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Page of Pokémon summaries"),
    @ApiResponse(
        responseCode = "400",
        description = "Invalid paging (validation or page beyond last)",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ProblemDetail.class))),
    @ApiResponse(
        responseCode = "502",
        description = "Upstream catalog unavailable or timed out",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ProblemDetail.class)))
  })
  public PokemonPageResponse list(@Valid @ParameterObject PokemonListRequest request) {
    return PokemonPageResponse.from(listPokemonUseCase.execute(request.toPageRequest()));
  }
}
