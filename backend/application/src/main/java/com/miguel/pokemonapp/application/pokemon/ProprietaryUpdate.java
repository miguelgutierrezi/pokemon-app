package com.miguel.pokemonapp.application.pokemon;

import java.util.List;

public record ProprietaryUpdate(
    String localizedName, String location, List<String> tags, long expectedVersion) {}
