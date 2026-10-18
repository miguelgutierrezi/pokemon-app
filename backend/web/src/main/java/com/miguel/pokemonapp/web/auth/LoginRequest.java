package com.miguel.pokemonapp.web.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
    @JsonProperty(required = true) @NotBlank String email,
    @JsonProperty(required = true) @NotBlank String password) {}
