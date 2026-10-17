package com.miguel.pokemonapp.infrastructure.pokeapi;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.miguel.pokemonapp.application.CatalogUnavailableException;
import com.miguel.pokemonapp.application.PokemonNotFoundException;
import com.miguel.pokemonapp.domain.pokemon.CatalogDetailEntry;
import com.miguel.pokemonapp.domain.pokemon.CatalogEvolutionChain;
import com.miguel.pokemonapp.domain.pokemon.CatalogEvolutionDetail;
import com.miguel.pokemonapp.domain.pokemon.CatalogEvolutionNode;
import com.miguel.pokemonapp.domain.pokemon.CatalogStat;
import com.miguel.pokemonapp.domain.pokemon.CatalogType;
import com.miguel.pokemonapp.domain.pokemon.FlavorTextEntry;
import com.miguel.pokemonapp.domain.pokemon.LocalizedText;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PokeApiCatalogAdapterDetailTest {

  private WireMockServer wireMock;
  private PokeApiCatalogAdapter adapter;

  @BeforeEach
  void setUp() {
    wireMock = new WireMockServer(wireMockConfig().dynamicPort());
    wireMock.start();
    adapter =
        new PokeApiCatalogAdapter(
            wireMock.baseUrl(), Duration.ofMillis(200), Duration.ofSeconds(5));
  }

  @AfterEach
  void tearDown() {
    if (adapter != null) {
      adapter.close();
    }
    if (wireMock != null) {
      wireMock.stop();
    }
  }

  @Test
  void fetchDetailMapsPokemonAndSpeciesFields() {
    stubPokemon(
        1,
        """
        {
          "id": 1,
          "name": "bulbasaur",
          "height": 7,
          "weight": 69,
          "sprites": {
            "front_default": "https://example.com/1.png",
            "other": {
              "official-artwork": {
                "front_default": "https://example.com/1-art.png"
              }
            }
          },
          "abilities": [
            { "ability": { "name": "overgrow" }, "is_hidden": false, "slot": 1 }
          ],
          "types": [
            { "slot": 1, "type": { "name": "grass" } },
            { "slot": 2, "type": { "name": "poison" } }
          ],
          "stats": [
            { "base_stat": 45, "stat": { "name": "hp" } },
            { "base_stat": 49, "stat": { "name": "attack" } }
          ]
        }
        """);
    stubSpecies(
        1,
        """
        {
          "names": [ { "name": "Bulbasaur", "language": { "name": "en" } } ],
          "genera": [ { "genus": "Seed Pokémon", "language": { "name": "en" } } ],
          "flavor_text_entries": [
            {
              "flavor_text": "A strange seed was\\nplanted on its back.",
              "language": { "name": "en" },
              "version": { "name": "red" }
            }
          ],
          "generation": { "name": "generation-i" },
          "evolution_chain": { "url": "https://pokeapi.co/api/v2/evolution-chain/1/" }
        }
        """);

    CatalogDetailEntry detail = adapter.fetchDetail(1);

    assertThat(detail.pokemon().nationalNumber()).isEqualTo(1);
    assertThat(detail.pokemon().slug()).isEqualTo("bulbasaur");
    assertThat(detail.pokemon().frontDefaultSprite()).isEqualTo("https://example.com/1.png");
    assertThat(detail.pokemon().officialArtworkSprite()).isEqualTo("https://example.com/1-art.png");
    assertThat(detail.pokemon().heightDecimetres()).isEqualTo(7);
    assertThat(detail.pokemon().weightHectograms()).isEqualTo(69);
    assertThat(detail.pokemon().types())
        .containsExactly(new CatalogType(1, "grass"), new CatalogType(2, "poison"));
    assertThat(detail.pokemon().stats())
        .containsExactly(new CatalogStat("hp", 45), new CatalogStat("attack", 49));
    assertThat(detail.species().names()).containsExactly(new LocalizedText("en", "Bulbasaur"));
    assertThat(detail.species().flavorEntries())
        .containsExactly(
            new FlavorTextEntry("en", "A strange seed was\nplanted on its back.", "red"));
    assertThat(detail.species().generationSlug()).isEqualTo("generation-i");
    assertThat(detail.species().evolutionChainId()).isEqualTo(1);
  }

  @Test
  void fetchDetailMapsPokemon404ToPokemonNotFoundException() {
    wireMock.stubFor(get(urlEqualTo("/pokemon/9999")).willReturn(aResponse().withStatus(404)));
    stubSpecies(9999, minimalSpeciesBody(1));

    assertThatThrownBy(() -> adapter.fetchDetail(9999))
        .isInstanceOf(PokemonNotFoundException.class)
        .extracting(ex -> ((PokemonNotFoundException) ex).nationalId())
        .isEqualTo(9999);
  }

  @Test
  void fetchDetailMapsSpecies404ToPokemonNotFoundException() {
    stubPokemon(9999, minimalPokemonBody(9999, "missing"));
    wireMock.stubFor(
        get(urlEqualTo("/pokemon-species/9999")).willReturn(aResponse().withStatus(404)));

    assertThatThrownBy(() -> adapter.fetchDetail(9999))
        .isInstanceOf(PokemonNotFoundException.class)
        .extracting(ex -> ((PokemonNotFoundException) ex).nationalId())
        .isEqualTo(9999);
  }

  @Test
  void fetchDetailMapsUpstream500ToCatalogUnavailableException() {
    wireMock.stubFor(
        get(urlEqualTo("/pokemon/1")).willReturn(aResponse().withStatus(500).withBody("error")));
    stubSpecies(1, minimalSpeciesBody(1));

    assertThatThrownBy(() -> adapter.fetchDetail(1))
        .isInstanceOf(CatalogUnavailableException.class);
  }

  @Test
  void fetchDetailThenFetchEntriesHitsIdentityResourcesExactlyOnce() {
    stubPokemon(1, minimalPokemonBody(1, "bulbasaur"));
    stubSpecies(1, minimalSpeciesBody(1));

    adapter.fetchDetail(1);
    assertThat(adapter.fetchEntries(List.of(1))).hasSize(1);

    wireMock.verify(exactly(1), getRequestedFor(urlEqualTo("/pokemon/1")));
    wireMock.verify(exactly(1), getRequestedFor(urlEqualTo("/pokemon-species/1")));
  }

  @Test
  void fetchEvolutionChainParsesBulbasaurLinearChain() {
    stubEvolutionChain(1, resource("evolution-chain-1.json"));

    CatalogEvolutionChain chain = adapter.fetchEvolutionChain(1);

    assertThat(chain.root().speciesId()).isEqualTo(1);
    assertThat(chain.root().evolutionDetails()).isEmpty();
    assertThat(chain.root().children()).hasSize(1);
    CatalogEvolutionNode ivysaur = chain.root().children().getFirst();
    assertThat(ivysaur.speciesId()).isEqualTo(2);
    assertThat(ivysaur.evolutionDetails())
        .containsExactly(new CatalogEvolutionDetail("level-up", 16, null));
    CatalogEvolutionNode venusaur = ivysaur.children().getFirst();
    assertThat(venusaur.speciesId()).isEqualTo(3);
    assertThat(venusaur.evolutionDetails())
        .containsExactly(new CatalogEvolutionDetail("level-up", 32, null));
    assertThat(venusaur.children()).isEmpty();
  }

  @Test
  void fetchEvolutionChainParsesTaurosSingleNodeChain() {
    stubEvolutionChain(63, resource("evolution-chain-63.json"));

    CatalogEvolutionChain chain = adapter.fetchEvolutionChain(63);

    assertThat(chain.root().speciesId()).isEqualTo(128);
    assertThat(chain.root().evolutionDetails()).isEmpty();
    assertThat(chain.root().children()).isEmpty();
  }

  @Test
  void fetchEvolutionChainParsesEeveeBranchingChainFromRealJson() {
    stubEvolutionChain(67, resource("evolution-chain-67.json"));

    CatalogEvolutionChain chain = adapter.fetchEvolutionChain(67);

    assertThat(chain.root().speciesId()).isEqualTo(133);
    assertThat(chain.root().children()).hasSize(8);
    assertThat(chain.root().children())
        .extracting(CatalogEvolutionNode::speciesId)
        .containsExactly(134, 135, 136, 196, 197, 470, 471, 700);
    assertThat(child(chain, 134).evolutionDetails())
        .containsExactly(new CatalogEvolutionDetail("use-item", null, "water-stone"));
    assertThat(child(chain, 135).evolutionDetails())
        .containsExactly(new CatalogEvolutionDetail("use-item", null, "thunder-stone"));
    assertThat(child(chain, 136).evolutionDetails())
        .containsExactly(new CatalogEvolutionDetail("use-item", null, "fire-stone"));
    assertThat(child(chain, 196).evolutionDetails())
        .containsExactly(new CatalogEvolutionDetail("level-up", null, null));
    assertThat(child(chain, 197).evolutionDetails())
        .containsExactly(new CatalogEvolutionDetail("level-up", null, null));
    assertThat(child(chain, 470).evolutionDetails())
        .containsExactly(
            new CatalogEvolutionDetail("level-up", null, null),
            new CatalogEvolutionDetail("level-up", null, null),
            new CatalogEvolutionDetail("level-up", null, null),
            new CatalogEvolutionDetail("use-item", null, "leaf-stone"),
            new CatalogEvolutionDetail("level-up", null, null),
            new CatalogEvolutionDetail("level-up", null, null));
    assertThat(child(chain, 471).evolutionDetails())
        .containsExactly(
            new CatalogEvolutionDetail("level-up", null, null),
            new CatalogEvolutionDetail("level-up", null, null),
            new CatalogEvolutionDetail("level-up", null, null),
            new CatalogEvolutionDetail("use-item", null, "ice-stone"),
            new CatalogEvolutionDetail("level-up", null, null),
            new CatalogEvolutionDetail("level-up", null, null));
    assertThat(child(chain, 700).evolutionDetails())
        .containsExactly(
            new CatalogEvolutionDetail("level-up", null, null),
            new CatalogEvolutionDetail("level-up", null, null));
  }

  @Test
  void identicalFetchEvolutionChainCallsHitUpstreamOnce() {
    stubEvolutionChain(67, resource("evolution-chain-67.json"));

    CatalogEvolutionChain first = adapter.fetchEvolutionChain(67);
    CatalogEvolutionChain second = adapter.fetchEvolutionChain(67);

    assertThat(first.root().speciesId()).isEqualTo(second.root().speciesId());
    wireMock.verify(exactly(1), getRequestedFor(urlEqualTo("/evolution-chain/67")));
  }

  @Test
  void failedEvolutionChainCallIsNotCachedAndRetryHitsUpstream() {
    wireMock.stubFor(
        get(urlEqualTo("/evolution-chain/67"))
            .willReturn(aResponse().withStatus(500).withBody("error")));

    assertThatThrownBy(() -> adapter.fetchEvolutionChain(67))
        .isInstanceOf(CatalogUnavailableException.class);

    stubEvolutionChain(67, resource("evolution-chain-67.json"));

    CatalogEvolutionChain recovered = adapter.fetchEvolutionChain(67);
    assertThat(recovered.root().speciesId()).isEqualTo(133);
    wireMock.verify(exactly(2), getRequestedFor(urlEqualTo("/evolution-chain/67")));
  }

  private static CatalogEvolutionNode child(CatalogEvolutionChain chain, int speciesId) {
    return chain.root().children().stream()
        .filter(node -> node.speciesId() == speciesId)
        .findFirst()
        .orElseThrow();
  }

  private void stubPokemon(int id, String body) {
    wireMock.stubFor(
        get(urlEqualTo("/pokemon/" + id))
            .willReturn(aResponse().withHeader("Content-Type", "application/json").withBody(body)));
  }

  private void stubSpecies(int id, String body) {
    wireMock.stubFor(
        get(urlEqualTo("/pokemon-species/" + id))
            .willReturn(aResponse().withHeader("Content-Type", "application/json").withBody(body)));
  }

  private void stubEvolutionChain(int id, String body) {
    wireMock.stubFor(
        get(urlEqualTo("/evolution-chain/" + id))
            .willReturn(aResponse().withHeader("Content-Type", "application/json").withBody(body)));
  }

  private static String minimalPokemonBody(int id, String name) {
    return """
        {
          "id": %d,
          "name": "%s",
          "height": 7,
          "weight": 69,
          "sprites": { "front_default": "https://example.com/%d.png" },
          "abilities": [],
          "types": [],
          "stats": []
        }
        """
        .formatted(id, name, id);
  }

  private static String minimalSpeciesBody(int evolutionChainId) {
    return """
        {
          "names": [ { "name": "Bulbasaur", "language": { "name": "en" } } ],
          "genera": [ { "genus": "Seed Pokémon", "language": { "name": "en" } } ],
          "flavor_text_entries": [],
          "generation": { "name": "generation-i" },
          "evolution_chain": {
            "url": "https://pokeapi.co/api/v2/evolution-chain/%d/"
          }
        }
        """
        .formatted(evolutionChainId);
  }

  private static String resource(String name) {
    String path = "/pokeapi/" + name;
    try (InputStream in = PokeApiCatalogAdapterDetailTest.class.getResourceAsStream(path)) {
      Objects.requireNonNull(in, "missing test resource " + path);
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new IllegalStateException("failed to read " + path, e);
    }
  }
}
