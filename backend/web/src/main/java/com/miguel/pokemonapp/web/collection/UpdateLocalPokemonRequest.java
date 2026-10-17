package com.miguel.pokemonapp.web.collection;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.miguel.pokemonapp.application.pokemon.ProprietaryUpdate;
import com.miguel.pokemonapp.domain.pokemon.LocalPokemon;
import com.miguel.pokemonapp.domain.pokemon.Tags;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record UpdateLocalPokemonRequest(
    @JsonProperty(required = true)
        @Size(max = LocalPokemon.MAX_LOCALIZED_NAME_LENGTH)
        @Schema(nullable = true, maxLength = LocalPokemon.MAX_LOCALIZED_NAME_LENGTH)
        String localizedName,
    @JsonProperty(required = true)
        @Size(max = LocalPokemon.MAX_LOCATION_LENGTH)
        @Schema(nullable = true, maxLength = LocalPokemon.MAX_LOCATION_LENGTH)
        String location,
    @JsonProperty(required = true)
        @NotNull
        @Size(max = Tags.MAX_COUNT)
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        List<@NotBlank @Size(max = Tags.MAX_TAG_LENGTH) String> tags,
    @JsonProperty(required = true)
        @NotNull
        @Min(0)
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0")
        Long version) {

  ProprietaryUpdate toProprietaryUpdate() {
    return new ProprietaryUpdate(localizedName, location, tags, version);
  }
}
