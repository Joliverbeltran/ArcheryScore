# ArcheryScore

A native Android archery score tracking app for solo archers. Record archery rounds end-by-end with per-arrow scoring, resume in-progress sessions, review history with per-end breakdowns, track statistics and trends, and export/import your data as CSV. **Fully on-device** — all scores live in local SQLite with no account, no cloud, and no network surface.

The app is developed strictly with **Test-Driven Development** (TDD), ships as a single installable **APK ≤ 15 MB** for **Android 8.0+**, and uses only current, CVE-scanned libraries.

## Key Features

- **Record archery rounds** — configure end count and arrows per end, enter 1–10 (10-zone) or 1–5 (5-zone) scores with X-ring support, and watch the running total update instantly (< 100 ms).
- **Resume anytime** — sessions persist automatically and are restored on next launch; a single ACTIVE session is enforced.
- **Five disciplines** — Olympic recurve, traditional recurve, barebow, longbow, and compound bow, each recorded with shooting distance (e.g. 18 m, 30 m, 70 m).
- **History & detail** — every completed session listed newest-first, with a per-end breakdown, totals, and X-count.
- **Statistics** — average score per session, best session, and improvement trend over completed sessions, filterable by date range, with a "need more data" state below 3 sessions.
- **CSV export & import** — share any session's data through the Android share sheet as a `text/csv` attachment, and import previously exported files via the History screen (SAF picker). Imports are idempotent: duplicate session IDs are skipped.

## Tech Stack

| Layer | Technology |
| ----- | ---------- |
| Language | Kotlin 2.3.21 (100% Kotlin) |
| UI | Jetpack Compose 1.12 (BOM 2026.08.00), Material 3 |
| Architecture | MVVM + Clean Architecture (single `:app` module, package-by-layer) |
| DI | Hilt 2.60.1 |
| Navigation | Navigation Compose 2.10.0 |
| Local storage | Room 2.8.4 (SQLite — single source of truth) |
| CSV | Kotlin stdlib (RFC 4180 compliant export/import) |
| Preferences | DataStore Preferences 1.2.1 |
| Build | Gradle 9.4.1+, AGP 9.2.0 (built-in Kotlin), Gradle Kotlin DSL + version catalog |
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
│   │   ├── libs.versions.toml       # version catalog
│   │   └── wrapper/                 # Gradle 9.4.1+
│   ├── gradle.properties
│   └── app/
│       ├── build.gradle.kts
│       ├── proguard-rules.pro
│       └── src/
│           ├── main/kotlin/com/archeryscore/app/
│           │   ├── ArcheryScoreApp.kt    # Application class
│           │   ├── MainActivity.kt
│           │   ├── di/                   # Hilt modules (DB, repositories)
│           │   ├── domain/               # Pure Kotlin — models, enums, use cases
│           │   │   ├── model/            # Session, End, Arrow, RoundType, Discipline…
│           │   │   ├── repository/       # Interface contracts (Sessions, Stats…)
│           │   │   └── usecase/          # Session/Stats/Export use cases
│           │   ├── data/
│           │   │   ├── local/            # Room entities + DAOs, AppDatabase
│           │   │   ├── repository/       # Repository impls + mappers
│           │   │   ├── csv/              # CsvExporter + CsvImporter
│           │   │   └── prefs/            # DataStore preferences repository
│           │   └── ui/                   # Compose — screens + ViewModels
│           │       ├── navigation/       # NavHost, bottom bar routes
│           │       └── record/ resume/ history/ stats/ start/
│           ├── test/                     # JUnit 5 unit tests (MockK + Turbine)
│           └── androidTest/              # Instrumented / Compose UI tests
└── specs/
    ├── 001-archery-score/           # Original spec + plan
    └── 002-local-sqlite-storage/    # Feature spec: Supabase removal + local-only
```

## Architecture

### On-device, single source of truth

All data lives in SQLite (Room) on the device. There is no cloud backend, no sync queue, and no network dependency.

```
Score entry
   │
   ▼
Room (sessions/ends/arrows)   ← the ONLY source of truth
   │
   ├─► CSV export  (share via Android share sheet)
   └─► CSV import  (SAF picker → validation → restore)
```

- Room handles all persistence with a certified migration path (v1 → v2).
- CSV export/import is the only data-exchange mechanism — no account or server required.
- The app has **no network permissions** (`INTERNET`, `ACCESS_NETWORK_STATE` are absent from the merged manifest).

### Corrupt-DB recovery

If the on-device SQLite file is corrupted, the app recovers by deleting the database file and starting from a clean state — no crash, no data leak.

### Layering (Clean Architecture)

- **`domain`** — pure Kotlin models, repository *interfaces*, and use cases. No Android/AndroidX imports.
- **`data`** — Room, DataStore, CSV exporter/importer, and mappers implement those interfaces.
- **`ui`** — Jetpack Compose screens and ViewModels that observe repository flows via Hilt-injected state. UI state is a single immutable `UiState` per screen.

### Data model

| Entity | Notes |
| ------ | ----- |
| `Session` | metadata (date, round type, distance m, discipline, end count, arrows/end, notes) + status `ACTIVE → COMPLETE` |
| `End` | one "end" of shooting (typically 3 or 6 arrows), numbered within a session |
| `Arrow` | a single shot; `score` + `is_x_ring`, carries `edited_at` |
| `Preferences` | user defaults (round type, ends, arrows, distance, discipline, X-ring pref) |

Round types: **10-zone** (max 10) and **5-zone** (max 5) with X-ring scoring. Score validation is centralized in `ScoreValidator`.

## Prerequisites

- **JDK 17+** (JDK 21 recommended for AGP 9).
- **Android SDK** with platform **37**, build-tools 37, and platform-tools (`sdkmanager` or Android Studio latest stable).
- A physical Android device (Android 8.0+) or an emulator for running the app and instrumented tests.
- **No Supabase project, API keys, or backend configuration is needed.**

## Getting Started

### 1. Clone the repository

```bash
git clone git@github.com:Joliverbeltran/ArcheryScore.git
cd ArcheryScore
```

> Active branches: `002-local-sqlite-storage` (current work), `main`.

### 2. Configure the Android SDK

```bash
cd android
cp local.properties.example local.properties
```

Edit `local.properties` to set your SDK path:

```properties
sdk.dir=/home/YOUR_USER/Android/Sdk
```

No other configuration is required — there are no API keys, backend URLs, or cloud credentials.

### 3. Build and install

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

No network, cloud, or auth credentials are required.

## Available Commands

| Task | Command |
| ---- | ------- |
| Run unit tests | `./gradlew testDebugUnitTest` |
| Run instrumented tests (device needed) | `./gradlew connectedAndroidTest` |
| Lint gate (zero errors) | `./gradlew lintDebug` |
| Build debug APK | `./gradlew assembleDebug` |
| Build release APK (R8 minified) | `./gradlew assembleRelease` |
| Install on device | `./gradlew installDebug` |

All Gradle commands run from the `android/` directory.

## Testing

Development follows strict TDD (tests written *before* implementation — RED → GREEN → REFACTOR), with evidence in the commit history.

- **Unit tests** — JUnit 5 + MockK + Turbine covering domain use cases, mappers, view models, repositories, CSV export/import round-trip, stats filtering, and more. **13 suites, all green**.
  ```bash
  ./gradlew testDebugUnitTest
  ```
- **Instrumented / Compose UI tests** — Room migration verification, CSV import-restore, and critical user journeys. These require a connected device or emulator:
  ```bash
  ./gradlew connectedAndroidTest
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

## CSV Export & Import

### Export
Open any completed session from the History screen and tap **Export CSV**. The file opens in the Android share sheet — send it to email, cloud storage, or any app that accepts `text/csv`.

### Import
From the History screen, tap **Import CSV** and select a previously exported file. The importer:
- Validates the header (must match exactly)
- Checks every field (UUID, date, distance, discipline, round type, scores within bounds)
- Returns structured errors (`row`, `column`, `reason`) on any invalid row
- Skips sessions whose IDs already exist in the database (idempotent)
- Operates atomically: no partial imports on malformed input

## Troubleshooting

**`org.jetbrains.kotlin.android` plugin is deprecated / AGP 9 built-in Kotlin**
AGP 9.0 moved Kotlin compilation into the Android Gradle plugin itself. The classic `org.jetbrains.kotlin.android` plugin is **not** applied in this project, and the temporary opt-out flags `android.builtInKotlin=false` / `android.newDsl=false` have been removed (the opt-out is dropped in AGP 10). Don't re-add them unless a third-party plugin forces the classic setup.

**`KSP/AGP mismatch`**
For AGP 9 built-in Kotlin, use KSP ≥ 2.3.6 and Hilt ≥ 2.59 (this project uses KSP 2.3.11 / Hilt 2.60.1). The Compose and kotlinx-serialization compiler plugins are applied normally alongside built-in Kotlin.

**Compose fails with a compileSdk warning**
Compose 1.12 requires compileSdk 37. Install the platform:
```bash
sdkmanager "platforms;android-37"
```

**"/platform-tools/adb: No such file or directory"**
Ensure `sdk.dir` in `local.properties` points to a full SDK with `platform-tools`, and that `adb` is on your PATH.

**`assembleRelease` looks stuck at R8**
R8 minification on a first build is CPU/RAM hungry; let it run (a quiet shell can look stalled). If the shell session is torn down, rerun with a detached invocation, e.g. `setsid ./gradlew :app:assembleRelease`.

## Project Status & Governance

This project is developed against a formal specification under `specs/001-archery-score/` (plan, data model, contracts, and task list) and `specs/002-local-sqlite-storage/` (Supabase removal, local-only architecture) with a project constitution (`.specify/memory/constitution.md` v2.0.0) that mandates:

- Strict **TDD** with test-first evidence in every commit.
- **Latest-stable CVE-scanned dependencies**.
- **SQLite (Room) as the sole source of truth** — no cloud, no sync, no account.
- Native Android modern stack (Kotlin, Compose, MVVM + Clean, Hilt).
- **No network surface**: no `INTERNET` permission, no WorkManager sync jobs.

Device-gated validation (instrumented tests on a connected device, on-device performance timing, install smoke tests, and the v1→v2 upgrade path) is tracked in `specs/002-local-sqlite-storage/quickstart.md` and must be completed in a CI/device environment before release.