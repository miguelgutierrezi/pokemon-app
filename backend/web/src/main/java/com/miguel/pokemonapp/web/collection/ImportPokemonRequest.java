package com.miguel.pokemonapp.web.collection;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ImportPokemonRequest(
    @NotNull
        @Min(1)
        @Schema(description = "National Dex number to import from the catalog", example = "25")
        Integer nationalNumber) {}
