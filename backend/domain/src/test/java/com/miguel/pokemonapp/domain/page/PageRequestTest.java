package com.miguel.pokemonapp.domain.page;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class PageRequestTest {

  @Test
  void computesOffsetFromOneBasedPage() {
    assertThat(new PageRequest(1, 9).offset()).isEqualTo(0);
    assertThat(new PageRequest(2, 9).offset()).isEqualTo(9);
    assertThat(new PageRequest(114, 9).offset()).isEqualTo(1017);
  }

  @Test
  void rejectsPageLessThanOne() {
    assertThatThrownBy(() -> new PageRequest(0, 9)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsSizeBelowOne() {
    assertThatThrownBy(() -> new PageRequest(1, 0)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsSizeAboveTwenty() {
    assertThatThrownBy(() -> new PageRequest(1, 21)).isInstanceOf(IllegalArgumentException.class);
  }
}
