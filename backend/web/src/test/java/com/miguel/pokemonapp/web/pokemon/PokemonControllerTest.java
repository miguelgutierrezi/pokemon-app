package com.miguel.pokemonapp.web.pokemon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.miguel.pokemonapp.application.pokemon.ListPokemonUseCase;
import com.miguel.pokemonapp.domain.page.Page;
import com.miguel.pokemonapp.domain.page.PageRequest;
import com.miguel.pokemonapp.domain.pokemon.AbilityRef;
import com.miguel.pokemonapp.domain.pokemon.PokemonSummary;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PokemonController.class)
class PokemonControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private ListPokemonUseCase listPokemonUseCase;

  @Test
  void rejectsPageZero() throws Exception {
    mockMvc.perform(get("/api/v1/pokemon").param("page", "0")).andExpect(status().isBadRequest());
  }

  @Test
  void rejectsSizeAboveMax() throws Exception {
    mockMvc.perform(get("/api/v1/pokemon").param("size", "21")).andExpect(status().isBadRequest());
  }

  @Test
  void defaultsSizeToNineWhenOmitted() throws Exception {
    when(listPokemonUseCase.execute(any()))
        .thenReturn(Page.of(List.of(), new PageRequest(1, PageRequest.DEFAULT_SIZE), 0L));

    mockMvc.perform(get("/api/v1/pokemon")).andExpect(status().isOk());

    ArgumentCaptor<PageRequest> captor = ArgumentCaptor.forClass(PageRequest.class);
    verify(listPokemonUseCase).execute(captor.capture());
    assertThat(captor.getValue().page()).isEqualTo(1);
    assertThat(captor.getValue().size()).isEqualTo(PageRequest.DEFAULT_SIZE);
  }

  @Test
  void returnsPageJsonShape() throws Exception {
    PokemonSummary summary =
        new PokemonSummary(
            1,
            "Bulbasaur",
            "https://example.com/1.png",
            "Seed Pokémon",
            new BigDecimal("6.9"),
            List.of(new AbilityRef("overgrow", false), new AbilityRef("chlorophyll", true)));
    when(listPokemonUseCase.execute(any()))
        .thenReturn(Page.of(List.of(summary), new PageRequest(1, 9), 1025L));

    mockMvc
        .perform(get("/api/v1/pokemon").param("page", "1").param("size", "9"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].nationalNumber").value(1))
        .andExpect(jsonPath("$.content[0].name").value("Bulbasaur"))
        .andExpect(jsonPath("$.content[0].spriteUrl").value("https://example.com/1.png"))
        .andExpect(jsonPath("$.content[0].category").value("Seed Pokémon"))
        .andExpect(jsonPath("$.content[0].weightKg").value(6.9))
        .andExpect(jsonPath("$.content[0].abilities[0].name").value("overgrow"))
        .andExpect(jsonPath("$.content[0].abilities[0].hidden").value(false))
        .andExpect(jsonPath("$.content[0].abilities[1].name").value("chlorophyll"))
        .andExpect(jsonPath("$.content[0].abilities[1].hidden").value(true))
        .andExpect(jsonPath("$.page").value(1))
        .andExpect(jsonPath("$.size").value(9))
        .andExpect(jsonPath("$.totalElements").value(1025))
        .andExpect(jsonPath("$.totalPages").value(114));
  }

  @Test
  void serializesWeightKgWithOneDecimalScale() throws Exception {
    PokemonSummary summary =
        new PokemonSummary(
            25,
            "Pikachu",
            "https://example.com/25.png",
            "Mouse Pokémon",
            new BigDecimal("10.0"),
            List.of(new AbilityRef("static", false)));
    when(listPokemonUseCase.execute(any()))
        .thenReturn(Page.of(List.of(summary), new PageRequest(1, PageRequest.DEFAULT_SIZE), 1L));

    String body =
        mockMvc
            .perform(get("/api/v1/pokemon").param("page", "1").param("size", "9"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    assertThat(body).contains("\"weightKg\":10.0");
  }
}
