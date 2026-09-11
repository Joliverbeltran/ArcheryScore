# Quickstart — Local-Only Storage (Remove Supabase)

**Branch**: `002-local-sqlite-storage` | **Spec**: `spec.md` | **Plan**: `plan.md`

Prereqs and daily-combat commands for engineers working on this feature. Full TODOs land in `tasks.md` (Phase 2, `/speckit.tasks`).

## Environment

- Android SDK (compileSdk 37), JDK 21, AGP 9.2.0, Gradle 9.x (Kotlin DSL).
- **No** Supabase project, `SUPABASE_URL` / `SUPABASE_ANON_KEY`, or `supabase/` dir required — remove them.

## Build & test commands

```bash
cd android
./gradlew :app:testDebugUnitTest    # unit tests (JUnit5 + Mockk + Turbine)
./gradlew :app:lintDebug            # lint, zero errors
./gradlew :app:assembleDebug        # build
./gradlew :app:connectedAndroidTest # instrumented: Room migration 1→2, corruption, CSV import
```

## Verification checklist (maps to success criteria)

- [ ] Offline demo: enable airplane mode → record → finish → restart → session intact with **no sync/account UI** (SC-001, SC-002, SC-004)
- [ ] History + stats render from local DB only; ≤2s / ≤1s for ≤500 sessions (SC-003)
- [ ] CSV export succeeds offline (SC-007); import the exported file → sessions restored (SC-009); duplicate import skips, no changes (FR-015)
- [ ] Upgrade path: install previous build (Room v1 with data) → open new build → data preserved (SC-005)
- [ ] Corrupt DB file → app opens to empty state, no crash (FR-012)
- [ ] No network: merged manifest has no `INTERNET`/`ACCESS_NETWORK_STATE`; `dumpsys package | grep -i permission` (SC-006)
- [ ] Dependency scan: `./gradlew dependencyCheckAnalyze` (or equivalent) → no critical/high CVEs (constitution II)

## Governance gate (before merge)

- [ ] `constitution.md` amended: Principle III → on-device-first, Principle V → local-only, Supabase rows removed, version bumped (MAJOR) (FR-013, SC-008)
- [ ] Docs templates re-aligned (`.specify/templates/`)
- [ ] TDD evidence in commits (tests first), full unit+instrumented suite green, lint zero errors

## Known teardown summary

Touch points for the removal (see `plan.md` → Project Structure for the tree):
`data/sync/**`, `data/network/`, `data/auth/**`, `SyncStatusRepository` + `AuthRepository` (domain), `SyncWriteDao`/`PreferencesDao` (Room), `ArcheryScoreApp` Configuration.Provider, Manifest provider/permissions, gradle catalog + `app/build.gradle.kts`, `supabase/` dir, `BuildConfig.SUPABASE_*`.
Additions: `data/csv/CsvImporter.kt`, import action in History, `MIGRATION_1_2`, import/result strings.