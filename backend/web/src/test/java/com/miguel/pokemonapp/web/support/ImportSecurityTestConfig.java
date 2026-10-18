package com.miguel.pokemonapp.web.support;

import com.miguel.pokemonapp.web.config.JwtConfiguration;
import com.miguel.pokemonapp.web.config.PublicRoutes;
import com.miguel.pokemonapp.web.config.SecurityConfiguration;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.context.annotation.Import;

/**
 * Brings the JWT + security filter chain into {@code @WebMvcTest} slices (they do not pick up
 * {@code @Configuration} beans from the main application automatically).
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import({
  SecurityConfiguration.class,
  JwtConfiguration.class,
  PublicRoutes.class,
  WebMvcSecurityTestConfiguration.class
})
public @interface ImportSecurityTestConfig {}
