package com.miguel.pokemonapp.web.pokemon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.miguel.pokemonapp.application.pokemon.GetPokemonDetailUseCase;
import com.miguel.pokemonapp.application.pokemon.ListPokemonUseCase;
import com.miguel.pokemonapp.domain.page.Page;
import com.miguel.pokemonapp.domain.page.PageRequest;
import com.miguel.pokemonapp.domain.pokemon.AbilityRef;
import com.miguel.pokemonapp.domain.pokemon.EvolutionChild;
import com.miguel.pokemonapp.domain.pokemon.EvolutionLink;
import com.miguel.pokemonapp.domain.pokemon.EvolutionNode;
import com.miguel.pokemonapp.domain.pokemon.FlavorText;
import com.miguel.pokemonapp.domain.pokemon.PokemonDetail;
import com.miguel.pokemonapp.domain.pokemon.PokemonStats;
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

  @MockitoBean private GetPokemonDetailUseCase getPokemonDetailUseCase;

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

  @Test
  void returnsDetailJsonShape() throws Exception {
    when(getPokemonDetailUseCase.execute(1)).thenReturn(bulbasaurDetail(flavorText()));

    String body =
        mockMvc
            .perform(get("/api/v1/pokemon/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.nationalNumber").value(1))
            .andExpect(jsonPath("$.name").value("Bulbasaur"))
            .andExpect(jsonPath("$.category").value("Seed Pokémon"))
            .andExpect(jsonPath("$.types[0]").value("grass"))
            .andExpect(jsonPath("$.types[1]").value("poison"))
            .andExpect(jsonPath("$.abilities[0].name").value("overgrow"))
            .andExpect(jsonPath("$.abilities[0].hidden").value(false))
            .andExpect(jsonPath("$.abilities[1].name").value("chlorophyll"))
            .andExpect(jsonPath("$.abilities[1].hidden").value(true))
            .andExpect(jsonPath("$.generation").value("generation-i"))
            .andExpect(jsonPath("$.imageUrl").value("https://example.com/1-art.png"))
            .andExpect(jsonPath("$.stats.hp").value(45))
            .andExpect(jsonPath("$.stats.attack").value(49))
            .andExpect(jsonPath("$.stats.defense").value(49))
            .andExpect(jsonPath("$.stats.specialAttack").value(65))
            .andExpect(jsonPath("$.stats.specialDefense").value(65))
            .andExpect(jsonPath("$.stats.speed").value(45))
            .andExpect(jsonPath("$.stats.total").value(318))
            .andExpect(
                jsonPath("$.flavorText.text").value("A strange seed was planted on its back."))
            .andExpect(jsonPath("$.flavorText.version").value("red"))
            .andExpect(jsonPath("$.evolution.nationalNumber").value(1))
            .andExpect(jsonPath("$.evolution.name").value("Bulbasaur"))
            .andExpect(jsonPath("$.evolution.imageUrl").value("https://example.com/1.png"))
            .andExpect(jsonPath("$.evolution.isCurrent").value(true))
            .andExpect(jsonPath("$.evolution.children[0].trigger").value("level-up"))
            .andExpect(jsonPath("$.evolution.children[0].minLevel").value(16))
            .andExpect(jsonPath("$.evolution.children[0].itemSlug").value(nullValue()))
            .andExpect(jsonPath("$.evolution.children[0].node.nationalNumber").value(2))
            .andExpect(jsonPath("$.evolution.children[0].node.name").value("Ivysaur"))
            .andExpect(jsonPath("$.evolution.children[0].node.isCurrent").value(false))
            .andReturn()
            .getResponse()
            .getContentAsString();

    assertThat(body).contains("\"heightM\":0.7");
    assertThat(body).contains("\"weightKg\":6.9");
    assertThat(body).contains("\"isCurrent\":true");
    assertThat(body).doesNotContain("\"current\":");
  }

  @Test
  void returnsNullFlavorTextWhenAbsent() throws Exception {
    when(getPokemonDetailUseCase.execute(1)).thenReturn(bulbasaurDetail(null));

    mockMvc
        .perform(get("/api/v1/pokemon/1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.flavorText").value(nullValue()));
  }

  @Test
  void rejectsDetailIdZero() throws Exception {
    mockMvc.perform(get("/api/v1/pokemon/0")).andExpect(status().isBadRequest());
  }

  private static PokemonDetail bulbasaurDetail(FlavorText flavorText) {
    return new PokemonDetail(
        1,
        "Bulbasaur",
        "Seed Pokémon",
        List.of("grass", "poison"),
        new BigDecimal("0.7"),
        new BigDecimal("6.9"),
        List.of(new AbilityRef("overgrow", false), new AbilityRef("chlorophyll", true)),
        "generation-i",
        "https://example.com/1-art.png",
        new PokemonStats(45, 49, 49, 65, 65, 45),
        flavorText,
        new EvolutionNode(
            1,
            "Bulbasaur",
            "https://example.com/1.png",
            true,
            List.of(
                new EvolutionChild(
                    new EvolutionLink("level-up", 16, null),
                    new EvolutionNode(
                        2, "Ivysaur", "https://example.com/2.png", false, List.of())))));
  }

  private static FlavorText flavorText() {
    return new FlavorText("A strange seed was planted on its back.", "red");
  }
}
