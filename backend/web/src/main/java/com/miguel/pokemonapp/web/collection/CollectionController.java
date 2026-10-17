package com.miguel.pokemonapp.web.collection;

import com.miguel.pokemonapp.application.pokemon.GetLocalPokemonUseCase;
import com.miguel.pokemonapp.application.pokemon.ImportPokemonUseCase;
import com.miguel.pokemonapp.application.pokemon.ListLocalPokemonUseCase;
import com.miguel.pokemonapp.domain.pokemon.LocalPokemon;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.net.URI;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/collection")
public class CollectionController {

  private final ImportPokemonUseCase importPokemonUseCase;
  private final ListLocalPokemonUseCase listLocalPokemonUseCase;
  private final GetLocalPokemonUseCase getLocalPokemonUseCase;

  public CollectionController(
      ImportPokemonUseCase importPokemonUseCase,
      ListLocalPokemonUseCase listLocalPokemonUseCase,
      GetLocalPokemonUseCase getLocalPokemonUseCase) {
    this.importPokemonUseCase = importPokemonUseCase;
    this.listLocalPokemonUseCase = listLocalPokemonUseCase;
    this.getLocalPokemonUseCase = getLocalPokemonUseCase;
  }

  @PostMapping(
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(summary = "Import (sync) a Pokémon from the catalog into the local store")
  @ApiResponses({
    @ApiResponse(
        responseCode = "201",
        description = "Local record created",
        headers =
            @Header(
                name = "Location",
                description = "URL of the new local record",
                schema = @Schema(type = "string", example = "/api/v1/collection/25")),
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = LocalPokemonResponse.class))),
    @ApiResponse(
        responseCode = "400",
        description = "Invalid body (missing/invalid nationalNumber)",
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
        responseCode = "409",
        description = "National number already in the local collection",
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
  public ResponseEntity<LocalPokemonResponse> importPokemon(
      @Valid @RequestBody ImportPokemonRequest request) {
    LocalPokemon created = importPokemonUseCase.execute(request.nationalNumber());
    URI location = URI.create("/api/v1/collection/" + created.nationalNumber());
    return ResponseEntity.created(location).body(LocalPokemonResponse.from(created));
  }

  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(summary = "List local collection (national number ascending)")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Page of local Pokémon records",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = LocalPokemonPageResponse.class))),
    @ApiResponse(
        responseCode = "400",
        description = "Invalid paging (validation or page beyond last)",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ProblemDetail.class)))
  })
  public LocalPokemonPageResponse list(@Valid @ParameterObject CollectionListRequest request) {
    return LocalPokemonPageResponse.from(listLocalPokemonUseCase.execute(request.toPageRequest()));
  }

  @GetMapping(value = "/{nationalNumber}", produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(summary = "Get one local record by National Dex number")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Local Pokémon record",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = LocalPokemonResponse.class))),
    @ApiResponse(
        responseCode = "400",
        description = "Invalid nationalNumber (non-numeric or < 1)",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ProblemDetail.class))),
    @ApiResponse(
        responseCode = "404",
        description = "Not in the local collection (distinct detail from catalog 404)",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ProblemDetail.class)))
  })
  public LocalPokemonResponse getByNationalNumber(
      @Parameter(description = "National Dex number", required = true) @PathVariable @Min(1)
          int nationalNumber) {
    return LocalPokemonResponse.from(getLocalPokemonUseCase.execute(nationalNumber));
  }
}
