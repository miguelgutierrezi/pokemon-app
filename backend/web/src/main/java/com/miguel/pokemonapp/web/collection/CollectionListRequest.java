package com.miguel.pokemonapp.web.collection;

import com.miguel.pokemonapp.domain.page.PageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record CollectionListRequest(
    @Min(1)
        @Schema(
            description = "1-based page index",
            defaultValue = "1",
            minimum = "1",
            example = "1")
        Integer page,
    @Min(1)
        @Max(PageRequest.MAX_SIZE)
        @Schema(
            description = "Page size",
            defaultValue = "9",
            minimum = "1",
            maximum = "20",
            example = "9")
        Integer size) {

  PageRequest toPageRequest() {
    return new PageRequest(page != null ? page : 1, size != null ? size : PageRequest.DEFAULT_SIZE);
  }
}
