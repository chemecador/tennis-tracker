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

## Backend & Data Model (planned, not implemented)

Direction we are committing to before writing the persistence layer. Treat this as the target; deviations need a reason.

### Storage

- Firestore is the cloud store. Realtime Database is reserved for the future live-spectator feature only.
- Room is the local source of truth while a match is in progress. The match is pushed to Firestore on completion, so playing offline never loses data.
- Cloud sync is initiated from the phone via the Wearable Data Layer. The watch only writes directly to Firestore when no paired phone is available — the Firestore SDK is too heavy to be the watch's default path.

### Firestore schema

- `users/{uid}`: `displayName`, `username` (unique, used for friend search), `elo: { tennis, padel }`, `stats`, `searchable: bool` (default `false`, user opts in to appear in search).
- `matches/{matchId}`: `sport`, `format`, `players: [uid|"guest:<name>"]`, `winner`, `score` (JSON from `:shared`), `startedAt`, `finishedAt`, `status: pending|confirmed|rejected`, `createdBy`.
- Query matches per user with `array-contains` on `players`. Do not duplicate matches into per-user subcollections.
- No photo uploads for now — user profiles are text-only.

### Friends

- Username (not email) is the lookup key for adding friends.
- `friendships/{a_b}` where the document id is the two uids sorted alphabetically and joined with `_`. This prevents duplicate `a→b` / `b→a` entries.
- FCM notifies a user when they are added as the opponent of a match awaiting confirmation.

### Match confirmation & ELO integrity

- A match against another registered user is created with `status: pending`. The opponent confirms or rejects from the mobile app.
- Auto-confirm after 7 days of no response.
- Only `confirmed` matches affect ELO. Matches against `guest:` opponents are kept in personal history but never touch ELO.

### ELO

- Separate ELO per sport (`tennis` and `padel` are independent).
- Initial rating 1200. K-factor decays with experience (K=40 for the first 30 matches, then 20, then 10).
- Padel (doubles): team ELO = average of the two members; the ELO delta is split 50/50 between teammates.
- Two rankings exposed: global, and "among your friends".

### Firestore rules (non-negotiable)

- A user can only write a match document where their uid is in `players`.
- A match document can only be read by its participants.
- ELO fields on `users/{uid}` are writable only by Cloud Functions / trusted server logic, never by the client.
