package com.miguel.pokemonapp.web.pokemon;

import com.miguel.pokemonapp.application.pokemon.GetPokemonDetailUseCase;
import com.miguel.pokemonapp.application.pokemon.ListPokemonUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/pokemon")
public class PokemonController {

  private final ListPokemonUseCase listPokemonUseCase;
  private final GetPokemonDetailUseCase getPokemonDetailUseCase;

  public PokemonController(
      ListPokemonUseCase listPokemonUseCase, GetPokemonDetailUseCase getPokemonDetailUseCase) {
    this.listPokemonUseCase = listPokemonUseCase;
    this.getPokemonDetailUseCase = getPokemonDetailUseCase;
  }

  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
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

  @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(summary = "Get Pokémon detail (National Dex id)")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Pokémon detail"),
    @ApiResponse(
        responseCode = "400",
        description = "Invalid id (non-numeric or < 1)",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ProblemDetail.class))),
    @ApiResponse(
        responseCode = "404",
        description = "Pokémon not found in the catalog",
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
  public PokemonDetailResponse getById(
      @Parameter(description = "National Dex number", required = true) @PathVariable @Min(1)
          int id) {
    return PokemonDetailResponse.from(getPokemonDetailUseCase.execute(id));
  }
}
