# Implementation Plan: Visual Target Arrow Placement

**Branch**: `003-target-arrow-input` | **Date**: 2026-09-15 | **Spec**: `specs/003-target-arrow-input/spec.md`

**Input**: Feature specification from `/specs/003-target-arrow-input/spec.md`

**Note**: This template is filled in by the `/speckit.plan` command. See `.specify/templates/plan-template.md` for the execution workflow.

## Summary

Replace numeric score entry with **visual arrow placement** (FR-001..FR-015, SC-001..SC-007): during score entry the app shows an image/rendering of the selected target face; the user taps where the arrow landed, drags the marker to the exact spot, and taps **OK** to persist. After each OK the flow advances automatically to the next arrow in the same end (FR-006). Six target options are selectable in the session set-up ("front") menu — 122cm, 80cm, 60cm, 40cm, triple vertical, triple triangular (FR-002) — and the chosen target is displayed whenever arrows are being placed (FR-001). The score is **derived automatically** from the confirmed placement against the target's scoring rings (FR-007), with standard-archery conventions: ring-line touches score the higher value (FR-009), and placements outside all rings are misses = 0 (FR-008). Triple faces assign the arrow to the nearest spot (FR-010).

Technical approach (see `research.md`): draw the target programmatically with Compose `Canvas` (no image assets — 10 equal-width WA zones in 5 colors, inner-10 "X"), place markers in a **normalized target coordinate space** (unit = face radius), and convert position → score with a pure domain function (`PlacementScorer`) that is exhaustively unit-tested (R-1..R-3). A new `sessions.target_type` column (Room v2→v3, `ALTER TABLE ADD COLUMN`, default `CM122`) plus a `default_target_type` DataStore preference persist the selection per session (FR-013) and as the default. The CSV export/import format gains a `target_type` column (10-col header) with backward-compatible import of the legacy 9-col files, preserving the 002 round-trip guarantee (SC-006). No new dependencies; TDD-first per constitution I.

## Technical Context

**Language/Version**: Kotlin 2.3.21 (AGP 9.2.0 built-in Kotlin; Gradle 9.x, Kotlin DSL)

**Primary Dependencies**:
- KEEP: Jetpack Compose + Material 3 (Compose BOM 2026.08.00) — `androidx.compose.foundation.Canvas` for target rendering, `pointerInput` for tap/drag; Hilt; Room 2.8.4; DataStore Preferences; Compose Navigation; Coroutines; JUnit5 + Mockk + Turbine.
- ADD: **none** (rendering is pure Compose Canvas; no image library, no new artifacts).

**Storage**: AndroidX Room (`archery_score.db`) schema version **3** — `sessions` gains `target_type TEXT NOT NULL DEFAULT 'CM122'` via `ALTER TABLE` in `MIGRATION_2_3`; `ends`/`arrows` unchanged (spec: placement is the input method, not a stored attribute). Preferences: `default_target_type` key added to DataStore; `UserPreferences.defaultTargetType = TargetType.CM122` default.

**Testing**: JUnit5 + Mockk + Turbine unit tests for `PlacementScorer` (ring mapping, boundaries→higher value, misses, X-ring, triple nearest-spot, layout constants), CSV round-trip + legacy 9-col import, and preference defaults. Compose UI tests for tap→marker/OK→advance/cancel→discard flows. Instrumented tests for Room migration 2→3 data preservation.

**Target Platform**: Android 8.0+ (minSdk 26), targetSdk 36, compileSdk 37. Single user, single device.

**Project Type**: Android mobile application (single `:app` module, Jetpack Compose, MVVM + Clean Architecture, Hilt DI).

**Performance Goals**: Tap→marker feedback immediately perceptible (UI thread, pure geometry, no allocations in draw path); placement→score resolution < 16 ms per OK (SC-001 under 45s/end is dominated by user drag time, not compute). Existing goals (history ≤2s, stats ≤1s, cold start <2s, APK ≤15MB) unchanged — canvas rendering adds negligible APK size.

**Constraints**: Fully offline, no network permission (unchanged). No hardcoded strings — target labels/score badges from string resources. Programmatic drawing keeps APK growth near zero (SC mid: 0% regression). TDD mandatory (constitution I): geometry/score logic and the Room migration are test-first.

**Scale/Scope**: Single `:app` module; additions touch `domain/` (TargetType, Session.targetType, PlacementScorer), `data/local` (entity column + MIGRATION_2_3), `data/csv` (10-col format), `data/prefs`, `ui/record` (placement UI replacing numeric `ScoreDialog`), `ui/start` (target dropdown), `ui/components` (TargetFace composable). Concrete tree in Project Structure.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Gate | Verdict | Rationale |
|------|---------|-----------|
| I. TDD (NON-NEGOTIABLE) | PASS | `PlacementScorer`, triple layout, `MIGRATION_2_3`, CSV target_type handling and placement UI flows are all written test-first (Red-Green-Refactor). |
| II. Security & Dependency Hygiene | PASS | Zero new dependencies; no network surface added; dependency/CVE scan unchanged; no secrets introduced. |
| III. On-Device-First Storage | PASS | Target type is a plain local column/preference; all data stays in the on-device Room DB. |
| IV. Native Android Modern Stack | PASS | Kotlin, Compose (Canvas/M3), MVVM+Clean, Hilt, Kotlin DSL, SDK constraints unchanged. |
| V. Local-Only Operation | PASS | Target is rendered on-device from geometry; no download, assets are programmatic; no sync/account UI introduced. |
| Constraints (APK ≤15MB, cold start <2s, no hardcoded strings) | PASS | Programmatic Canvas rendering avoids shipping 6 image assets (keeps APK small); target labels and badges come from string resources. |
| Quality gates (tests, lint, no vulns, review, TDD evidence) | PASS | Enforced pre-merge; see `quickstart.md` checklist. |

Post-design re-check: Phase 1 introduces no deviations; all gates remain green. No constitution amendment is required (feature 001/002 principles unchanged). Complexity Tracking → N/A.

## Project Structure

### Documentation (this feature)

```text
specs/003-target-arrow-input/
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output (/speckit.plan command)
├── data-model.md        # Phase 1 output (/speckit.plan command)
├── quickstart.md        # Phase 1 output (/speckit.plan command)
├── contracts/           # Phase 1 output (/speckit.plan command)
│   ├── target-input.md  # Placement UI + score-mapping contract (NEW)
│   └── storage.md       # Room schema v3 & target_type persist semantics (NEW)
└── tasks.md             # Phase 2 output (/speckit.tasks command - NOT created by /speckit.plan)
```

### Source Code (repository root)

```text
android/
├── app/
│   ├── build.gradle.kts                   # no dependency changes
│   └── src/
│       ├── main/
│       │   ├── res/values/strings.xml     # target-type labels, score badges (M/X/ring), dialog strings
│       │   └── kotlin/com/archeryscore/app/
│       │       ├── data/
│       │       │   ├── local/
│       │       │   │   ├── AppDatabase.kt             # version 3 + MIGRATION_2_3 (ALTER TABLE ADD COLUMN)
│       │       │   │   └── entity/Entities.kt         # SessionEntity + target_type
│       │       │   ├── mapper/Mapper.kt               # map TargetType.name <-> enum
│       │       │   ├── prefs/DataStorePreferencesRepository.kt  # default_target_type key
│       │       │   └── csv/                           # CsvExporter/CsvImporter: 10-col target_type
│       │       ├── di/AppModule.kt                    # unchanged (no new bindings)
│       │       ├── domain/
│       │       │   ├── model/
│       │       │   │   ├── TargetType.kt              # NEW enum (6 values) + label/spotLayout metadata
│       │       │   │   ├── PlacementScorer.kt         # NEW pure domain: normalize point -> score/isX/miss/spot
│       │       │   │   ├── Session.kt                 # + targetType (default CM122)
│       │       │   │   └── UserPreferences.kt         # + defaultTargetType
│       │       │   └── repository/Repositories.kt     # unchanged (targetType on Session model flows through)
│       │       └── ui/
│       │           ├── components/TargetFace.kt       # NEW Canvas renderer (single + triple layouts)
│       │           ├── record/
│       │           │   ├── TargetPlacementDialog.kt   # NEW: tap/drag/OK + auto-advance within an end
│       │           │   ├── ActiveSessionScreen.kt     # arrow chip -> TargetPlacementDialog (replaces ScoreDialog)
│       │           │   └── ActiveSessionViewModel.kt  # placement flow state (current arrow index, unconfirmed marker)
│       │           └── start/StartScreen.kt           # TargetTypeDropdown added to session set-up menu
│       ├── test/
│       │   ├── PlacementScorerTest.kt                 # ring/boundary/miss/X/triple-spot unit tests
│       │   ├── TripleLayoutTest.kt                    # spot-center constants, nearest-spot resolution
│       │   ├── CsvRoundTripTargetTypeTest.kt          # 10-col round-trip + legacy 9-col import
│       │   ├── UserPreferencesTargetTypeTest.kt       # default_target_type persistence
│       │   └── TargetPlacementFlowTest.kt             # Compose UI: tap/drag/OK/advance/cancel/discard
│       └── androidTest/
│           └── RoomMigration23Test.kt                 # MIGRATION_2_3 preserves sessions/ends/arrows
├── gradle/
├── build.gradle.kts
├── settings.gradle.kts
└── libs.versions.toml                    # unchanged

specs/003-target-arrow-input/             # planning docs (this feature)
specs/002-local-sqlite-storage/contracts/csv-roundtrip.md  # amended: 10-col format + legacy import
```

**Structure Decision**: Unchanged single-module Android layout. The feature is a focused swap of the score-entry interaction plus a small cross-cutting persistence touch (`target_type` through Room, DataStore, Mapper, CSV). No new module is warranted; all new logic lives in `domain/model` (pure, heavily unit-tested) and `ui/record` + `ui/components` (Compose).

## Complexity Tracking

> Not applicable — Constitution Check passes with no violations; all gates evaluated PASS post-design. No deviations to justify.