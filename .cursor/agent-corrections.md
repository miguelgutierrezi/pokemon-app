# Agent corrections

Log of user corrections for the agent to apply in future sessions. Maintained
by the agent per `.cursor/rules/agent-corrections.mdc`.

## 2026-10-16 — Corrections log language
- **Source**: user
- **Context**: The first version of the agent-corrections rule and log was
  written in Spanish.
- **Correction**: Always write `.cursor/rules/agent-corrections.mdc` and
  `.cursor/agent-corrections.md` in English.

## 2026-10-16 — Constitution precedence and correction source
- **Source**: Miguel's review
- **Context**: The rule said to apply every correction with no check against
  binding project docs, and entries had no origin field.
- **Correction**: (1) Corrections never override the constitution or the
  feature's spec/plan/tasks — on conflict, stop and ask. (2) Every entry
  must include `Source`: `user` or `Miguel's review`.

## 2026-10-16 — Maven module naming and package-info accuracy (T002–T003)
- **Source**: Miguel's review
- **Context**: After T002–T003, package-info Javadoc drifted from the plan
  (messaging; entrypoint attributed to `.web`), internal module versions
  were repeated in each POM, and artifactIds were bare module names.
- **Correction**: (1) infrastructure Javadoc: persistence, PokeAPI client,
  caching and security; web Javadoc: controllers, DTOs and error handling
  (PokemonApplication is in `com.miguel.pokemonapp`). (2) Parent
  `dependencyManagement` for the four internal modules; no `<version>` on
  those deps in child POMs. (3) artifactIds `pokemonapp-{domain,application,
  infrastructure,web}` (directories stay short names).
