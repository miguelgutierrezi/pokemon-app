package com.miguel.pokemonapp.web.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.miguel.pokemonapp.domain.user.PersonName;
import com.miguel.pokemonapp.domain.user.RawPassword;
import com.miguel.pokemonapp.web.auth.validation.MaxUtf8Bytes;
import com.miguel.pokemonapp.web.auth.validation.ValidEmail;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @JsonProperty(required = true) @NotBlank @Size(max = PersonName.MAX_LENGTH) String name,
    @JsonProperty(required = true) @NotBlank @ValidEmail String email,
    @JsonProperty(required = true)
        @NotBlank
        @Size(min = RawPassword.MIN_LENGTH)
        @MaxUtf8Bytes(RawPassword.MAX_UTF8_BYTES)
        String password) {}
