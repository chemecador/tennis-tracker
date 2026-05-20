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

## Backend & Data Model

User profiles (`users/{uid}` + `usernames/{u}`) are implemented in `:mobile`. Everything else in this section is the agreed target; deviations need a reason.

### Implemented

- Firestore dependency wired in `:mobile`.
- `users/{uid}` document created on first login through `ChooseUsernameScreen`, with seeded ELO (1200 tennis / 1200 padel) and zeroed stats. Every signed-in user can read every profile — no per-user visibility flag.
- `usernames/{u}` lock document guarantees username uniqueness via a Firestore transaction; the doc id IS the lowercase username.
- `friendships/{minUid_maxUid}` collection. Document id is the two uids sorted lexicographically and joined with `_`, so `A→B` and `B→A` collapse into the same document and duplicates are impossible. Fields: `participants: [uidA, uidB]`, `status: pending|accepted`, `requestedBy`, `createdAt`, `acceptedAt?`. Double opt-in: only the receiver can promote `pending` → `accepted`; either party can delete (reject / cancel / unfriend).
- Friend search resolves a username via `usernames/{u}` (public read) and then fetches `users/{uid}`.
- Security rules live at `firestore.rules` (deploy manually from the Firebase console — no `firebase-tools` in repo yet). Rules forbid clients from mutating `elo`, `username`, `stats`, `createdAt` after creation. For `friendships/`, rules validate the id format matches the sorted participants, the requester is the `requestedBy`, and only the receiver can accept.
- Anonymous users skip the profile step and don't see the Friends entry.
- `:wear` is untouched — still anonymous-only, no profile.

### Planned (not implemented)

### Storage

- Firestore is the cloud store. Realtime Database is reserved for the future live-spectator feature only.
- Room is the local source of truth while a match is in progress. The match is pushed to Firestore on completion, so playing offline never loses data.
- Cloud sync is initiated from the phone via the Wearable Data Layer. The watch only writes directly to Firestore when no paired phone is available — the Firestore SDK is too heavy to be the watch's default path.

### Firestore schema

- `users/{uid}`: `displayName`, `username` (unique, used for friend search), `elo: { tennis, padel }`, `stats`, `searchable: bool` (default `false`, user opts in to appear in search).
- `matches/{matchId}`: `sport`, `format`, `players: [uid|"guest:<name>"]`, `winner`, `score` (JSON from `:shared`), `startedAt`, `finishedAt`, `status: pending|confirmed|rejected`, `createdBy`.
- Query matches per user with `array-contains` on `players`. Do not duplicate matches into per-user subcollections.
- No photo uploads for now — user profiles are text-only.

### Friends (notifications still pending)

- FCM should notify a user when they receive a friend request and when they are added as the opponent of a match awaiting confirmation. Not implemented yet.

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
