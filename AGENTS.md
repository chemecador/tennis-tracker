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
- `:mobile`: phone app (Compose Material3). Full match flow (setup, scoreboard, summary) plus the email/password + register login UI; the watch only offers anonymous guest sign-in.

## Project Rules

- Keep match state immutable and test scoring behavior in `:shared`.
- Reuse existing test helpers before creating new setup code.
- `MatchSessionViewModel` is the single source of truth for Wear match state.
- Summary navigation happens when there is a winner; scoring logic must not know about navigation.
- With AGP 9, do not apply `alias(libs.plugins.kotlin.android)` in `:wear` or `:mobile`.
- Firebase Auth is wired in both modules; `google-services.json` must be present at `mobile/` and `wear/` for builds to succeed. Each device authenticates independently — cross-device session sharing (via the Wearable Data Layer) is not implemented yet.
