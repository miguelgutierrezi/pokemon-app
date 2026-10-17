package com.miguel.pokemonapp.domain.page;

import static com.miguel.pokemonapp.domain.Require.atLeast;
import static com.miguel.pokemonapp.domain.Require.nonNull;

import java.util.List;

public record Page<T>(List<T> content, int page, int size, long totalElements) {

  public Page {
    content = List.copyOf(nonNull(content, "content"));
    atLeast(page, 1, "page");
    atLeast(size, 1, "size");
    if (totalElements < 0) {
      throw new IllegalArgumentException("totalElements must be >= 0");
    }
    int pages = PageCounts.pagesFor(totalElements, size);
    if (totalElements > 0 && page > pages) {
      throw new PageOutOfRangeException("page " + page + " is beyond last page " + pages);
    }
  }

  public int totalPages() {
    return PageCounts.pagesFor(totalElements, size);
  }

  public static <T> Page<T> of(List<T> content, PageRequest request, long totalElements) {
    return new Page<>(content, request.page(), request.size(), totalElements);
  }
}
