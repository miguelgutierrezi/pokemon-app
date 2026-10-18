package com.miguel.pokemonapp.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(
    packages = "com.miguel.pokemonapp",
    importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

  @ArchTest
  static final ArchRule domainDependsOnlyOnItselfAndJava =
      classes()
          .that()
          .resideInAPackage("com.miguel.pokemonapp.domain..")
          .should()
          .onlyDependOnClassesThat()
          .resideInAnyPackage("com.miguel.pokemonapp.domain..", "java..")
          .allowEmptyShould(true);

  @ArchTest
  static final ArchRule applicationDependsOnlyOnItselfDomainAndJava =
      classes()
          .that()
          .resideInAPackage("com.miguel.pokemonapp.application..")
          .should()
          .onlyDependOnClassesThat()
          .resideInAnyPackage(
              "com.miguel.pokemonapp.application..", "com.miguel.pokemonapp.domain..", "java..")
          .allowEmptyShould(true);

  @ArchTest
  static final ArchRule domainAndApplicationAreFreeOfOuterFrameworks =
      noClasses()
          .that()
          .resideInAnyPackage(
              "com.miguel.pokemonapp.domain..", "com.miguel.pokemonapp.application..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(
              "org.springframework..",
              "jakarta.persistence..",
              "jakarta.servlet..",
              "java.sql..",
              "java.net.http..",
              "com.miguel.pokemonapp.infrastructure..",
              "com.miguel.pokemonapp.web..")
          .allowEmptyShould(true);

  @ArchTest
  static final ArchRule domainAndApplicationMustNotDependOnSpringSecurity =
      noClasses()
          .that()
          .resideInAnyPackage(
              "com.miguel.pokemonapp.domain..", "com.miguel.pokemonapp.application..")
          .should()
          .dependOnClassesThat()
          .resideInAPackage("org.springframework.security..")
          .allowEmptyShould(true);
}
