package com.miguel.pokemonapp.web.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT")
public class OpenApiConfiguration {

  @Bean
  OpenAPI pokefolioOpenApi() {
    return new OpenAPI()
        .info(
            new Info()
                .title("Pokéfolio API")
                .version("0.2.0")
                .description("Paginated National Dex list and related HTTP APIs."));
  }
}
