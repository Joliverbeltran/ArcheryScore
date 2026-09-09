# ArcheryScore

A native Android archery score tracking app for solo archers. Record archery rounds end-by-end with per-arrow scoring, resume in-progress sessions, review history with per-end breakdowns, track statistics and trends, and export your data as CSV. Built to be **offline-first**: every score is saved locally and seamlessly pushed to **Supabase** (your source of truth) when connectivity returns.

The app is developed strictly with **Test-Driven Development** (TDD), ships as a single installable **APK ≤ 15 MB** for **Android 8.0+**, and uses only current, CVE-scanned libraries.

## Key Features

- **Record archery rounds** — configure end count and arrows per end, enter 1–10 (10-zone) or 1–5 (5-zone) scores with X-ring support, and watch the running total update instantly (< 100 ms).
- **Resume anytime** — sessions persist automatically and are restored on next launch; a single ACTIVE session is enforced.
- **Five disciplines** — Olympic recurve, traditional recurve, barebow, longbow, and compound bow, each recorded with shooting distance (e.g. 18 m, 30 m, 70 m).
- **History & detail** — every completed session listed newest-first, with a per-end breakdown, totals, and X-count.
- **Statistics** — average score per session, best session, and improvement trend over completed sessions, filterable by date range, with a "need more data" state below 3 sessions.
- **CSV export** — share any session's data through the Android share sheet as a `text/csv` attachment.
- **Offline-first sync** — scores are queued locally in an outbox and synced automatically via WorkManager when a network connection returns; sync status (SYNCED / PENDING / ERROR) is always visible.
- **Conflict resolution** — multi-device edits resolve by **last-write-wins per arrow** using edit timestamps.
- **Email/password auth** — Supabase GoTrue backed, with a local offline fallback identity so the app still works before you sign in.

## Tech Stack

| Layer | Technology |
| ----- | ---------- |
| Language | Kotlin 2.3.21 (100% Kotlin) |
| UI | Jetpack Compose 1.12 (BOM 2026.08.00), Material 3 |
| Architecture | MVVM + Clean Architecture (single `:app` module, package-by-layer) |
| DI | Hilt 2.60.1 |
| Navigation | Navigation Compose 2.10.0 |
| Local storage | Room 2.8.4 (offline cache + sync outbox) |
| Preferences | DataStore Preferences 1.2.1 |
| Background sync | WorkManager 2.11.0+ |
| Backend / source of truth | Supabase (PostgreSQL + GoTrue Auth + PostgREST) via supabase-kt BOM 3.8.0 |
| HTTP | Ktor 3.4.0 (OkHttp engine) |
| Build | Gradle 9.4.1+, AGP 9.2.0, Gradle Kotlin DSL + version catalog |
| Testing | JUnit 5, MockK, Turbine, Compose UI tests, Android instrumented tests |

**Platform targets**: `minSdk 26` (Android 8.0), `targetSdk 36`, `compileSdk 37`.

## Project Structure

```
ArcheryScore/
├── AGENTS.md                        # Agent instructions (points to the spec plan)
├── android/                         # Android app (single :app module)
│   ├── settings.gradle.kts          # rootProject "ArcheryScore", includes :app
│   ├── build.gradle.kts
│   ├── gradle/
│   │   ├── libs.versions.toml       # version catalog (research.md matrix)
│   │   └── wrapper/                 # Gradle 9.4.1+
│   ├── gradle.properties
│   ├── local.properties.example     # template for sdk.dir + Supabase creds (git-ignored)
│   └── app/
│       ├── build.gradle.kts
│       ├── proguard-rules.pro
│       └── src/
│           ├── main/kotlin/com/archeryscore/app/
│           │   ├── ArcheryScoreApp.kt    # Application; WorkManager factory injection
│           │   ├── MainActivity.kt
│           │   ├── di/                   # Hilt modules (DB, Supabase client, repos)
│           │   ├── domain/               # Pure Kotlin — models, enums, use cases
│           │   │   ├── model/            # Session, End, Arrow, RoundType, Discipline…
│           │   │   ├── repository/       # Interface contracts (Auth, Sessions, Stats…)
│           │   │   └── usecase/          # Session/Stats/Export use cases
│           │   ├── data/
│           │   │   ├── local/            # Room entities + DAOs, sync outbox
│           │   │   ├── remote/           # Supabase data source
│           │   │   ├── repository/       # Repository impls + mappers
│           │   │   ├── sync/             # SyncRunner, SyncWorker, SyncStatus
│           │   │   ├── auth/             # SupabaseAuthRepository + local fallback
│           │   │   ├── csv/              # CSV generator + FileProvider share
│           │   │   ├── network/          # NetworkMonitor (connectivity flow)
│           │   │   └── prefs/            # DataStore preferences repository
│           │   └── ui/                   # Compose — screens + ViewModels
│           │       ├── navigation/       # NavHost, bottom bar routes
│           │       ├── start/ record/ resume/ history/ statistics/ export/ auth/
│           │       └── theme/ components/
│           ├── test/                     # JUnit 5 unit tests (MockK + Turbine)
│           └── androidTest/              # Instrumented / Compose UI tests
└── supabase/
    └── migrations/0001_init.sql          # Schema: sessions/ends/arrows + RLS
```

## Architecture

### Offline-first, cloud-authoritative

Supabase (PostgreSQL) is the **source of truth**, and Room acts as the local cache plus a **write-outbox**:

```
Score entry
   │  (works fully offline)
   ▼
Room (sessions/ends/arrows) ──► Sync write-outbox (queued operations)
                                   │
                                   ▼        WorkManager runs on network reconnect
                              SyncRunner  (push → confirm → pull)
                                   │
                                   ▼
                             Supabase (source of truth, RLS per user)
```

- Every local mutation records a row in the `sync_write` outbox with the entity type, operation (UPSERT/DELETE) and payload.
- `SyncWorker` is scheduled by WorkManager with a `CONNECTED` network constraint, exponential backoff, and a 10-attempt cap — the same trigger the `NetworkMonitor` exposes as a `Flow<Boolean>` for the UI.
- Conflicts between devices are resolved by **last-write-wins per arrow**: the row with the later `edited_at` timestamp wins.
- Missing/wrong credentials do not break the app: a `DefaultAuthRepository` provides a locally generated user id as the offline fallback identity.

### Layering (Clean Architecture)

- **`domain`** — pure Kotlin models, repository *interfaces*, and use cases. No Android/AndroidX imports.
- **`data`** — Room, DataStore, Supabase clients, mappers, and the sync engine implement those interfaces.
- **`ui`** — Jetpack Compose screens and ViewModels that observe repository flows via Hilt-injected state. UI state is a single immutable `UiState` per screen.

### Data model

| Entity | Notes |
| ------ | ----- |
| `Session` | metadata (date, round type, distance m, discipline, end count, arrows/end, notes) + status `ACTIVE → COMPLETE`; one ACTIVE per user enforced |
| `End` | one "end" of shooting (typically 3 or 6 arrows), numbered within a session |
| `Arrow` | a single shot; `score` + `is_x_ring`, carries `edited_at` for LWW sync |
| `SyncWrite` | outbox rows for push-to-Supabase |
| `Preferences` | user defaults (round type, ends, arrows, distance, discipline, X-ring pref) |

Round types: **10-zone** (max 10) and **5-zone** (max 5) with X-ring scoring. Score validation is centralized in `ScoreValidator`.

## Prerequisites

- **JDK 17+** (JDK 21 recommended for AGP 9).
- **Android SDK** with platform **37**, build-tools 37, and platform-tools (`sdkmanager` or Android Studio latest stable).
- A **Supabase project** (free tier is enough) with the URL and anon key — see [Supabase setup](#supabase-setup).
- The [Supabase CLI](https://supabase.com/docs/guides/cli) if you want to apply migrations from this repo.
- A physical Android device (Android 8.0+) or an emulator for running the app and instrumented tests.

## Getting Started

### 1. Clone the repository

```bash
git clone git@github.com:Joliverbeltran/ArcheryScore.git
cd ArcheryScore
```

> The active development branch is `001-archery-score`; `main` is an empty placeholder.

### 2. Configure the local environment

```bash
cd android
cp local.properties.example local.properties
```

Fill in your SDK path and Supabase credentials (this file is git-ignored and never committed):

```properties
sdk.dir=/home/YOUR_USER/Android/Sdk
SUPABASE_URL=https://<your-project-ref>.supabase.co
SUPABASE_ANON_KEY=<your-anon-key>
```

### 3. Apply the database schema (Supabase)

```bash
cd ../supabase
supabase db push          # applies migrations/0001_init.sql (tables + RLS policies)
```

Then in Supabase dashboard → **Authentication → Providers**, enable **Email + Password**.

### 4. Build and install

```bash
cd android
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Or install directly from Gradle:

```bash
./gradlew installDebug
```

## Configuration

All configuration lives in `android/local.properties` (template: `android/local.properties.example`).

| Key | Required | Description |
| --- | -------- | ----------- |
| `sdk.dir` | Yes | Absolute path to the Android SDK |
| `SUPABASE_URL` | No* | Your Supabase project URL (`https://<ref>.supabase.co`) |
| `SUPABASE_ANON_KEY` | No* | Your Supabase anon (publishable) key |

\* If the Supabase values are blank the app runs fully functional **offline with a local identity** (outbox sync is disabled until credentials are configured). Set them to enable real account auth and cloud sync.

Credentials are read at build time and baked into `BuildConfig` (see `app/build.gradle.kts`). Never commit real values.

## Available Commands

| Task | Command |
| ---- | ------- |
| Run unit tests | `./gradlew testDebugUnitTest` |
| Run instrumented tests (device needed) | `./gradlew connectedDebugAndroidTest` |
| Lint gate (zero errors) | `./gradlew lintDebug` |
| CVE dependency scan (needs network) | `./gradlew dependencyCheckAggregate` |
| All standard checks | `./gradlew check` |
| Build debug APK | `./gradlew assembleDebug` |
| Build release APK (R8 minified) | `./gradlew assembleRelease` |
| Install on device | `./gradlew installDebug` |
| Check dependency updates | `./gradlew dependencyUpdates` |

All Gradle commands run from the `android/` directory.

## Testing

Development follows strict TDD (tests written *before* implementation — RED → GREEN → REFACTOR), with evidence in the commit history.

- **Unit tests** — JUnit 5 + MockK + Turbine covering domain use cases, mappers, view models, repositories, auth validation, stats filtering, and sync outbox logic. Currently **56 tests, all green**.
  ```bash
  ./gradlew testDebugUnitTest
  ```
- **Instrumented / Compose UI tests** — critical user journeys (score entry, session detail) and edge cases. These require a connected device or emulator:
  ```bash
  ./gradlew connectedDebugAndroidTest
  ```
- **Lint** — enforced as a zero-error gate:
  ```bash
  ./gradlew lintDebug
  ```

## Building the Release APK

```bash
cd android
./gradlew assembleRelease
```

Output: `app/build/outputs/apk/release/app-release-unsigned.apk`.

- Release builds use **R8 full minification** + resource shrinking (see `proguard-rules.pro`).
- The APK must stay **≤ 15 MB**; the current release build is ~2.5 MB.
- To install the release build on a device you must sign it. Add a signing config to `android/app/build.gradle.kts` (keystore + `keystore.properties`, git-ignored) and build `app-release.apk`.

## Supabase Setup

1. Create a project at [supabase.com](https://supabase.com).
2. Apply the schema and RLS policies:
   ```bash
   cd supabase
   supabase link --project-ref <your-project-ref>
   supabase db push
   ```
3. Enable **Email + Password** under **Authentication → Providers**.
4. Copy the project URL and anon key into `android/local.properties`.

The migration `supabase/migrations/0001_init.sql` creates the `sessions`, `ends`, and `arrows` tables (with check constraints and a *one ACTIVE session per user* partial unique index) plus per-user **Row Level Security (RLS)** policies so each archer only ever reads/writes their own data.

## Troubleshooting

**`KSP/AGP mismatch`**
Keep the KSP version aligned with AGP 9.2.0 (see the note in `specs/001-archery-score/research.md`).

**Compose fails with a compileSdk warning**
Compose 1.12 requires compileSdk 37. Install the platform:
```bash
sdkmanager "platforms;android-37"
```

**"/platform-tools/adb: No such file or directory"**
Ensure `sdk.dir` in `local.properties` points to a full SDK with `platform-tools`, and that `adb` is on your PATH.

**`user already registered` during auth**
Use a fresh email in your Supabase test project, or enable the *confirm* flow toggle in Auth settings.

**Sync shows PENDING / ERROR**
Check `SUPABASE_URL`/`SUPABASE_ANON_KEY` in `local.properties` (values are baked at build time — rebuild after changing them) and confirm the migration/RLS were applied.

**`assembleRelease` looks stuck at R8**
R8 minification on a first build is CPU/RAM hungry; let it run (a quiet shell can look stalled). If the shell session is torn down, rerun with a detached invocation, e.g. `setsid ./gradlew :app:assembleRelease`.

## Project Status & Governance

This project is developed against a formal specification under `specs/001-archery-score/` (plan, data model, contracts, and task list) with a project constitution (`.specify/memory/constitution.md`) that mandates:

- Strict **TDD** with test-first evidence in every commit.
- **Latest-stable CVE-scanned dependencies**; credentials only in the git-ignored `local.properties`.
- **Supabase as source of truth** with Room as local cache/outbox.
- Native Android modern stack (Kotlin, Compose, MVVM + Clean, Hilt).

Device/network-gated validation (instrumented tests on a connected device, `dependencyCheckAggregate`, on-device performance timing, and install smoke tests) is tracked in `specs/001-archery-score/tasks.md` and must be completed in a CI/device environment before release.