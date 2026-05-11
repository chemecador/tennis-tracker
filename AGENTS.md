# AGENTS.md

## Commands

- Full build: `./gradlew build`
- Main tests: `./gradlew :shared:test`
- Specific test class: `./gradlew :shared:test --tests "com.chemecador.tennistracker.scoring.ScorerTennisTest"`
- Wear lint: `./gradlew :wear:lintDebug`
- Install Wear app: `./gradlew :wear:installDebug`

## Structure

- `:shared`: pure Kotlin scoring engine. Put all tennis/padel rule changes here.
- `:wear`: main Wear OS app built with Compose.
- `:mobile`: placeholder. Do not work on it unless explicitly requested.

## Project Rules

- Keep match state immutable and test scoring behavior in `:shared`.
- Reuse existing test helpers before creating new setup code.
- `MatchSessionViewModel` is the single source of truth for Wear match state.
- Summary navigation happens when there is a winner; scoring logic must not know about navigation.
- With AGP 9, do not apply `alias(libs.plugins.kotlin.android)` in `:wear` or `:mobile`.
