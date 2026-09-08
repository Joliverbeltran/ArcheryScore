# Implementation Plan: Archery Score Tracking

**Branch**: `001-archery-score` | **Date**: 2026-09-08 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/001-archery-score/spec.md`

**Note**: This template is filled in by the `/speckit.plan` command. See `.specify/templates/plan-template.md` for the execution workflow.

## Summary

Build a native Android **archery score tracking app** (Kotlin + Jetpack Compose, Material 3, MVVM + Clean Architecture, Hilt) that lets a solo archer record sessions (ends/arrows/scores), resume in-progress sessions, view history and statistics, and export CSV — with **offline-first storage** (Room cache + WorkManager queue) syncing to **Supabase** as the source of truth using **last-write-wins per arrow** conflict resolution. Produces a signed, installable **APK ≤ 15MB** for Android 8.0+ using only current, CVE-scanned libraries, developed strictly via TDD.

Technical approach (from research.md): Kotlin 2.3.21, Gradle 9.4.1+, AGP 9.2.0, Compose BOM 2026.08.00, Hilt 2.60.1, Navigation Compose 2.10.0, Room 2.8.4, DataStore 1.2.1, WorkManager 2.11.0+, supabase-kt BOM 3.8.0 (auth/postgrest), minSdk 26 / targetSdk 36 / compileSdk 37.

## Technical Context

**Language/Version**: Kotlin **2.3.21** (KSP2; latest-stable compatible with Hilt 2.60.1 — see research.md)

**Primary Dependencies**: Jetpack Compose (BOM 2026.08.00, Compose 1.12.0, Material 3) · Hilt 2.60.1 + hilt-navigation-compose 1.4.0 · Navigation Compose 2.10.0 · Room 2.8.4 (local cache) · DataStore 1.2.1 (preferences) · WorkManager 2.11.0+ (sync queue) · supabase-kt BOM 3.8.0 (`auth-kt`, `postgrest-kt`) · Ktor 3.4.0 (okhttp engine, transitive)

**Storage**: Supabase (PostgreSQL, source of truth, RLS per-user) + Room local mirror/outbox (constitution III). Schema in `contracts/storage.md`.

**Testing**: JUnit5 + MockK + Turbine (unit) · Room testing + Supabase test project (integration) · Compose UI tests (critical journeys) — TDD per constitution I, gates in quickstart.md.

**Target Platform**: Android 8.0+ (minSdk 26); targetSdk 36, compileSdk 37; physical device APK install required (SC-006).

**Project Type**: mobile-app (single-module Android, package-by-layer)

**Performance Goals**: score entry → running-total ≤ 100ms (SC-002); cold start < 2s mid-range; statistics ≤ 1s for ≤ 500 sessions (SC-005); history ≤ 2s (SC-004).

**Constraints**: APK ≤ 15MB · HTTPS only · no hardcoded strings (string resources) · offline-capable (SC-008) · zero data loss on offline→online (SC-007/003) · minSdk 26.

**Scale/Scope**: single-user solo archer (one ACTIVE session invariant), ≤ ~500 sessions, Android-only, no multi-archer/tournament features (spec assumptions).

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| # | Principle | Gate | Status |
|---|-----------|------|--------|
| I | TDD (NON-NEGOTIABLE) | Tests written before implementation; RED→GREEN→REFACTOR; test-first evidence in commits; unit + integration + UI layers | ✅ Planned: task breakdown lists test-first steps; quickstart TDD workflow |
| II | Security & Dependency Hygiene | All deps latest-stable & CVE-scanned; no critical/high vulns; creds in `local.properties` (git-ignored) | ✅ Planned: versions locked in research.md; `dependencyCheck` gate in quickstart; secrets never committed |
| III | Supabase-First Storage | Supabase = source of truth; offline queue syncs on reconnect | ✅ Planned: RLS schema + sync contract (data-sync.md, storage.md) |
| IV | Native Android Modern Stack | Kotlin 100%, Compose/M3, MVVM+Clean, Hilt, Compose Nav, Gradle KTS | ✅ Planned |
| V | Offline Resilience | Score entry works offline; auto-sync on reconnect; sync status indicators; no data loss | ✅ Planned: WorkManager + outbox + SYNCED/PENDING/ERROR states (FR-010) |

**Post-design re-check**: ✅ All 5 principles satisfied. One **constitution amendment required** (non-blocking for build, blocking for merge): v1.0.0 pins Target SDK 34 / Gradle 8.x; selected stack needs **targetSdk 36, compileSdk 37, Gradle 9.4.1+**. Tracked as `CONSTITUTION_AMENDMENT_FOLLOWUP` below.

> **CONSTITUTION_AMENDMENT_FOLLOWUP**: Amend `.specify/memory/constitution.md` v1.0.0 → v1.0.1 (PATCH: clarifications) before merge: update `Min/Target SDK` (26/36), Gradle line (9.x), and tech table rows 1/7. Rationale: 2026 AndroidX/Play requirements (research.md).

No complexity violations to justify — see **Structure Decision** (single module).

## Project Structure

### Documentation (this feature)

```text
specs/001-archery-score/
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output (/speckit.plan command)
├── data-model.md        # Phase 1 output (/speckit.plan command)
├── quickstart.md        # Phase 1 output (/speckit.plan command)
├── contracts/           # Phase 1 output (/speckit.plan command)
│   ├── auth.md
│   ├── data-sync.md
│   ├── storage.md
│   └── csv-export.md
└── tasks.md             # Phase 2 output (/speckit.tasks command - NOT created by /speckit.plan)
```

### Source Code (repository root)

```text
android/
├── build.gradle.kts            # root; version catalog plugin management
├── settings.gradle.kts
├── gradle/
│   ├── libs.versions.toml      # version catalog (research.md matrix)
│   └── wrapper/                # Gradle 9.4.1+
├── gradle.properties
└── app/
    ├── build.gradle.kts
    ├── proguard-rules.pro
    └── src/
        ├── main/
        │   ├── AndroidManifest.xml
        │   └── kotlin/com/archeryscore/app/
        │       ├── ArcheryScoreApp.kt        # Application + DI graph
        │       ├── MainActivity.kt
        │       ├── di/                       # Hilt modules
        │       ├── domain/                   # models, enums, use cases (pure Kotlin)
        │       │   ├── model/                # Session, End, Arrow, Preferences, enums
        │       │   ├── repository/           # interfaces
        │       │   └── userguide/            # (usecases) SessionUsecase, StatsUsecase, ExportUsecase
        │       ├── data/                     # Room, Supabase, DataStore, outbox
        │       │   ├── local/                # Room DAOs/Entities, Outbox
        │       │   ├── remote/               # Supabase clients, DataSource
        │       │   ├── repository/           # impls + mappers + sync logic
        │       │   └── sync/                 # SyncWorker, SyncRunner, SyncStatus
        │       └── ui/                       # Compose
        │           ├── navigation/           # NavHost, routes
        │           ├── session/              # record/resume screen + ViewModel
        │           ├── history/              # list/detail + ViewModel
        │           ├── statistics/           # stats + ViewModel
        │           ├── export/               # CSV generator, share flow
        │           ├── auth/                 # sign-in/sign-up + ViewModel
        │           ├── components/           # shared composables
        │           └── theme/                # Material 3 theme
        └── test/                             # unit tests (JUnit5+MockK+Turbine)
            ├── kotlin/com/archeryscore/app/domain/...
            ├── kotlin/com/archeryscore/app/data/sync/...
            └── kotlin/com/archeryscore/app/ui/...
        └── androidTest/                      # integration + UI tests (Room, Supabase, Compose)

supabase/
└── migrations/               # 0001_init.sql (contracts/storage.md) — apply via supabase CLI
```

**Structure Decision**: Single `:app` module (Gradle DSL Kotlin, version catalog in `gradle/libs.versions.toml`), package-by-layer (`domain` / `data` / `ui`) inside. Chosen over multi-module (`:core:*`) because the project is a small single-user app: one artifact keeps the 15MB APK and cold-start budgets easy to hit, avoids cross-module coordination overhead, and layer packages still enforce Clean Architecture boundaries + mockability for TDD. The only external split is `supabase/migrations/`, which is deployment-owned (Supabase CLI), not app code.

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

None. Constitution Check passes; single-module structure keeps complexity minimal.