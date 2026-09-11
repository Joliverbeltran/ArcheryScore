# Implementation Plan: Local-Only Storage (Remove Supabase)

**Branch**: `002-local-sqlite-storage` | **Date**: 2026-09-11 | **Spec**: `specs/002-local-sqlite-storage/spec.md`

**Input**: Feature specification from `/specs/002-local-sqlite-storage/spec.md`

**Note**: This template is filled in by the `/speckit.plan` command. See `.specify/templates/plan-template.md` for the execution workflow.

## Summary

Primary requirement (FR-001..FR-012): the app becomes fully on-device. Supabase is replaced by the existing internal SQLite database (AndroidX Room) as the **sole and authoritative** store — no cloud upload, no remote reads, no background sync, no account or sign-in, and no sync-status UI. History, statistics, and CSV export read only from the local database (FR-002, FR-003, FR-009). Both the Supabase client and the WorkManager sync pipeline are removed, along with all network permissions. Existing locally-stored sessions survive the upgrade through a certified Room migration 1→2 (FR-007, SC-005).

Backup/recovery is a **CSV round-trip** (clarification 2026-09-11): the current CSV exporter stays byte-compatible and a new importer restores sessions/scores from an exported file (FR-014), with strict validation that rejects malformed/incompatible files without touching existing data (FR-015, SC-009).

Governance: the project constitution is amended to remove the Supabase-First and sync pillars and reflect on-device-first storage **before this feature is merged** (FR-013, SC-008).

## Technical Context

**Language/Version**: Kotlin 2.3.21 (AGP 9.2.0 built-in Kotlin; Gradle 9.x, Kotlin DSL)

**Primary Dependencies**:
- KEEP: Jetpack Compose + Material 3 (Compose BOM 2026.08.00), Hilt, Room (`androidx.room:room-runtime`/`room-compiler` 2.8.4), DataStore Preferences, Compose Navigation, Coroutines, JUnit5 + Mockk + Turbine.
- REMOVE: `io.github.jan.supabase:*` (supabase-kotlin 3.8.0), `io.ktor:*` (3.4.0, only used by supabase), `androidx.work:*` (2.11.0, WorkManager sync pipeline), and the `BuildConfig.SUPABASE_URL` / `SUPABASE_ANON_KEY` fields.
- ADD: Android Storage Access Framework (`ACTION_OPEN_DOCUMENT`) for CSV import — framework class only, no new dependency.

**Storage**: AndroidX Room (SQLite) database `archery_score.db`, schema version **2** (dropped `sync_writes` + `prefs` tables, dropped `user_id`/`last_synced_at` columns; `ends`/`arrows` unchanged). Preferences remain in DataStore (`datastore/prefs.preferences_pb`); the `local_user_id` key is removed (no identity). Migration 1→2 preserves existing sessions/ends/arrows.

**Testing**: JUnit5 + Mockk + Turbine for unit tests; Room migrations validated in instrumented tests (schema export JSON); CSV parser/validator covered by pure unit tests (round-trip property: export→import→export is identical).

**Target Platform**: Android 8.0+ (minSdk 26), targetSdk 36, compileSdk 37. Single user, single device.

**Project Type**: Android mobile application (single `:app` module, Jetpack Compose, MVVM + Clean Architecture, Hilt DI).

**Performance Goals**: History list ≤ 2s and statistics ≤ 1s for up to 500 sessions (SC-003) — satisfied by existing DAO queries; cold start < 2s, APK ≤ 15MB both improve after removing supabase/ktor/workmanager. No network wait anywhere.

**Constraints**: Fully offline; no `INTERNET`/`ACCESS_NETWORK_STATE` permissions; no user identity/namespacing; single-ACTIVE-session invariant enforced locally; no hardcoded strings (string resources); TDD mandatory (constitution I).

**Scale/Scope**: Single `:app` module, mono-repo docs under `specs/`. Removal of `data/sync/**`, `data/network/**`, `data/auth/**`, domain auth/sync surfaces, gradle deps, manifest permissions, `supabase/` project dir; addition of CSV import + Room migration.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Gate | Verdict | Rationale |
|------|---------|-----------|
| I. TDD (NON-NEGOTIABLE) | PASS | Tests written before code for every unit of work; CSV importer + Room migration get test-first coverage (Red-Green-Refactor). |
| II. Security & Dependency Hygiene | PASS (improves) | Removing supabase/ktor/workmanager shrinks the supply chain; CVE scan run after dep removal. Supabase credentials no longer needed anywhere; `local.properties` & `BuildConfig` fields are deleted. |
| III. Supabase-First Storage | **AMENDMENT REQUIRED** | The feature's explicit purpose. Constitution principle III is replaced by an on-device-first principle. Amendment is implemented by this feature (FR-013, SC-008) and MUST land before merge — this is a MANDATED change, not a tolerated violation. |
| IV. Native Android Modern Stack | PASS | Kotlin, Compose/M3, MVVM+Clean, Hilt, Kotlin DSL, SDK constraints unchanged. |
| V. Offline Resilience | **AMENDMENT REQUIRED** | Sync-oriented sub-clauses (auto-sync, sync indicators) are eliminated; replaced by a pure "works offline with no network at all" principle. Amended via FR-013. |
| Constraints (APK ≤15MB, cold start <2s, HTTPS-only, no hardcoded strings) | PASS | Removals shrink APK/cold start; no network calls remain (HTTPS clause becomes moot); new import/export strings use resources. |
| Quality gates (tests, lint, no vulns, review, TDD evidence) | PASS | All enforced pre-merge (see `quickstart.md`). |

Post-design re-check: Phase 1 keeps every gate green; the two amendments are bounded, spec-mandated (FR-013/SC-008), and scheduled as a deliverable of this feature. No violations requiring tolerance → Complexity Tracking below is N/A.

## Project Structure

### Documentation (this feature)

```text
specs/002-local-sqlite-storage/
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output (/speckit.plan command)
├── data-model.md        # Phase 1 output (/speckit.plan command)
├── quickstart.md        # Phase 1 output (/speckit.plan command)
├── contracts/           # Phase 1 output (/speckit.plan command)
│   ├── csv-roundtrip.md # Export (unchanged) + new import contract
│   └── storage.md       # Local-only Room v2 schema & no-network guarantees
└── tasks.md             # Phase 2 output (/speckit.tasks command - NOT created by /speckit.plan)
```

### Source Code (repository root)

```text
android/
├── app/
│   ├── build.gradle.kts          # drop supabase/ktor/workmanager deps & SUPABASE_* buildConfigFields
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml        # drop INTERNET + ACCESS_NETWORK_STATE, WorkManager provider
│       │   └── kotlin/com/archeryscore/app/
│       │       ├── ArcheryScoreApp.kt     # remove WorkManager Configuration.Provider + SyncWorkerFactory
│       │       ├── di/AppModule.kt        # remove supabase/auth/sync bindings; add CSV import deps
│       │       ├── data/
│       │       │   ├── local/             # AppDatabase v2 + MIGRATION_1_2; entities/daos (drop sync/prefs)
│       │       │   ├── repository/        # RoomSessionRepository (drop outbox/userId), DefaultStatsRepository
│       │       │   ├── prefs/DataStorePreferencesRepository.kt  # drop local_user_id
│       │       │   └── csv/               # CsvExporter (unchanged) + NEW CsvImporter + CsvValidation
│       │       ├── domain/                # models (drop SyncStatus, Session.userId/lastSyncedAt), repositories (drop auth/sync/userId), NEW import usecase
│       │       └── ui/                    # record/history/stats/resume/detail/start screens; History import action
│       ├── test/                          # unit tests (incl. CSV round-trip/validation)
│       └── androidTest/                   # Room migration 1→2, corruption recovery, CSV import instrumented
├── gradle/
├── build.gradle.kts
├── settings.gradle.kts
└── libs.versions.toml            # remove supabase/ktor/workmanager catalog entries

supabase/                         # DELETED (cloud project artifacts, sql/migrations)

specs/002-local-sqlite-storage/   # planning docs (this feature)
```

**Structure Decision**: Unchanged single-module Android layout. The feature is a horizontal cleanup (removal across `data/`, `domain/`, `di/`, `ui/`, gradle, manifest, plus new import capability in `data/csv/` and `ui/history/`) — no new modules warranted. Cloud artifacts under `supabase/` are deleted with the teardown.

## Complexity Tracking

> Not applicable — Constitution Check passes; the two Principle gates (III, V) are satisfied by the spec-mandated amendments (FR-013/SC-008), not by deviations requiring override.