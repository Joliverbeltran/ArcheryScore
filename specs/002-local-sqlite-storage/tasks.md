# Tasks: Local-Only Storage (Remove Supabase)

**Input**: Design documents from `/specs/002-local-sqlite-storage/`

**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, contracts/, quickstart.md

**Tests**: TDD is NON-NEGOTIABLE per the project constitution (Principle I) — test-first tasks are included per story.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (US1..US4)
- All paths under `android/app/src/main/kotlin/com/archeryscore/app/` unless absolute-referenced

## Path Conventions

- **Mobile**: android/ module (single `:app` project). Main source: `android/app/src/main/kotlin/com/archeryscore/app/`; unit tests: `android/app/src/test/kotlin/com/archeryscore/app/`; instrumented: `android/app/src/androidTest/kotlin/com/archeryscore/app/`.

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Remove cloud/sync tooling from the build so the app can no longer reach the network.

- [X] T001 Remove `supabase`, `ktor`, and `workmanager` entries from `android/gradle/libs.versions.toml`
- [X] T002 [P] Remove supabase/ktor/workmanager dependency blocks and `SUPABASE_URL`/`SUPABASE_ANON_KEY` `buildConfigField`s from `android/app/build.gradle.kts`
- [X] T003 [P] Remove `INTERNET` + `ACCESS_NETWORK_STATE` permissions and the WorkManager `InitializationProvider` from `android/app/src/main/AndroidManifest.xml`
- [X] T004 [P] Remove WorkManager `Configuration.Provider` + `SyncWorkerFactory` wiring from `android/app/src/main/kotlin/com/archeryscore/app/ArcheryScoreApp.kt` (revert to plain `Application` with `@HiltAndroidApp`)
- [X] T005 [P] Delete the `supabase/` cloud project directory at the repo root (config.toml, migrations)

**Checkpoint**: Build no longer references Supabase/Ktor/WorkManager anywhere.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Database v2 + removal of the sync/auth/network layers — MUST be complete before ANY user story.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete.

- [X] T006 Write failing instrumented test `MigrationTest` for `MIGRATION_1_2` (v1 DB with a sample session/ends/arrows + sync_writes → v2 preserves session rows, drops `sync_writes`/`prefs`) in `android/app/src/androidTest/kotlin/com/archeryscore/app/data/local/`
- [X] T007 [P] Delete sync layer package `data/sync/` (`SyncRunner.kt`, `SyncWorker.kt`, `SyncWorkerFactory.kt`, `SyncOutboxWriter.kt`, `OutboxSyncStatusRepository.kt`, `SupabaseRemoteDataSource.kt`, `Remote.kt`, any `SyncScheduler` file)
- [X] T008 [P] Delete `data/network/NetworkMonitor.kt`
- [X] T009 [P] Delete `data/auth/` (`SupabaseAuthRepository.kt`, `DefaultAuthRepository.kt`) and delete test `android/app/src/test/kotlin/com/archeryscore/app/data/auth/SupabaseAuthRepositoryTest.kt`
- [X] T010 [P] Delete `domain/model/SyncStatus.kt`; remove `SyncStatus` + `SyncStatusRepository` from `domain/repository/Repositories.kt` and `SessionListItem.syncStatus`
- [X] T011 [P] Remove `AuthRepository`, `AuthResult`, `AuthFailureReason` from `domain/repository/Repositories.kt`
- [X] T012 [P] Drop `userId` and `lastSyncedAt` from `domain/model/Session.kt`
- [X] T013 [P] Update `data/local/entity/Entities.kt`: remove `userId`/`lastSyncedAt` from `SessionEntity` and its `user_id` Index; delete `data/local/entity/SyncOutbox.kt` (`SyncWriteEntity`, `PreferencesEntity`)
- [X] T014 [P] Delete `data/local/dao/SyncAndPreferencesDao.kt` (`SyncWriteDao`, `PreferencesDao`) and `data/local/dao/ArrowDao.kt`/`EndDao.kt`/`SessionDao.kt` userId query params
- [X] T015 [P] Update `data/local/AppDatabase.kt`: entities = `[SessionEntity, EndEntity, ArrowEntity]`, `version = 2`, remove `syncWriteDao()`/`preferencesDao()`
- [X] T016 [P] Implement `MIGRATION_1_2` (DDL from `data-model.md`: recreate `sessions` without `user_id`/`last_synced_at`, copy rows, rename, recreate `index_sessions_date`, drop `sync_writes`/`prefs`) in `AppDatabase` and register via `.addMigrations()` in `build()`
- [X] T017 [P] Add corrupt-DB recovery (catch `CorruptionException` → delete DB file → rebuild empty) in `AppDatabase.build()` (FR-012)
- [X] T018 [P] `data/repository/RoomSessionRepository.kt`: remove `SyncOutboxWriter` dependency + outbox writes; drop `userId` params from DAO calls; `observeSessions()` returns `List<Session>`
- [X] T019 [P] `data/prefs/DataStorePreferencesRepository.kt`: remove `LOCAL_USER_ID`, `ensureLocalUserId()`, `observeLocalUserId()`; drop `userId` params
- [X] T020 [P] `data/mapper/Mapper.kt`: remove `syncStatus()`, `sessionToEntity(...userId...)` userId arg, `prefsToEntity()`/`prefsFromEntity()`
- [X] T021 [P] `di/AppModule.kt`: delete supabase/auth/sync providers (`provideSupabaseClient`, `provideAuthRepository`, `provideRemote`, `provideSyncRunner`, `provideSyncStatusRepository`, `provideSyncScheduler`, `provideOutboxWriter`, `provideSyncWriteDao`, `providePreferencesDao`); rewire `SessionRepository` without outbox
- [X] T022 [P] `domain/repository/Repositories.kt` interfaces: drop `userId` params on `SessionRepository.observeSessions/observeActiveSession/getActiveSession` and `PreferencesRepository.observePreferences/updatePreferences`
- [X] T023 [P] `data/repository/DefaultStatsRepository.kt`: drop `userId` param from `observeStats()`
- [X] T024 Update test doubles and all existing unit tests (`test/kotlin/com/archeryscore/app/test/TestDoubles.kt`, `RoomSessionRepositoryTest.kt`, `HistoryViewModelTest.kt`, `StatsViewModelTest.kt`, etc.) to the non-namespaced signatures; delete obsolete auth/sync mocks; `./gradlew :app:testDebugUnitTest` green

**Checkpoint**: Foundation ready — app compiles, unit tests pass, user story work can begin.

---

## Phase 3: User Story 1 - Record a Session Fully On-Device (Priority: P1) 🎯 MVP

**Goal**: Start the app straight to the home screen with no sign-in/account; create, resume, finish, and edit sessions with zero network — writes land only in Room.

**Independent Test**: Enable airplane mode → launch → create/save scores/finish a session → restart app → the finished session and its scores are present with no sync/account UI (SC-001, SC-002).

### Tests for User Story 1 (write FIRST, ensure they FAIL) ⚠️

- [X] T025 [P] [US1] Update `test/kotlin/com/archeryscore/app/ui/resume/ResumeSessionViewModelTest.kt`: session creation + preferences load must not depend on `AuthRepository` (fails to compile until T026)

### Implementation for User Story 1

- [X] T026 [US1] `ui/resume/ResumeSessionViewModel.kt`: remove `AuthRepository`; call `preferencesRepository.observePreferences()` and `sessionRepository.observeActiveSession()` without userId; create `Session` without `userId`
- [X] T027 [US1] `ui/start/StartScreen.kt`: verify launch path has no sign-in/account/network gate and lands directly on the home/start screen (remove any legacy auth check)

**Checkpoint**: User Story 1 fully functional offline (record/finish/resume/edit/delete persist locally across restart).

---

## Phase 4: User Story 2 - View History from Local Data (Priority: P2)

**Goal**: History list + per-session detail render entirely from on-device Room data, newest first, correct totals, no sync indicator.

**Independent Test**: With a completed session present, open History offline → session listed with correct total, opens in Detail; empty DB shows the empty state (SC-003 history ≤ 2s).

### Tests for User Story 2 (write FIRST, ensure they FAIL) ⚠️

- [X] T028 [P] [US2] Update `test/kotlin/com/archeryscore/app/ui/history/HistoryViewModelTest.kt`: list loads via non-namespaced `observeSessions()` with no `AuthRepository`

### Implementation for User Story 2

- [X] T029 [US2] `ui/history/HistoryViewModel.kt`: remove `AuthRepository`/`currentUserId`; collect `sessionRepository.observeSessions()` directly
- [X] T030 [US2] `ui/history/HistoryScreen.kt`: delete the sync `AssistChip` and `SyncStatus` import (sync-status row)
- [X] T031 [P] [US2] Delete `sync_synced`/`sync_pending` strings from `res/values/strings.xml`

**Checkpoint**: History and Detail work fully offline with no sync UI.

---

## Phase 5: User Story 3 - Statistics Computed Locally (Priority: P2)

**Goal**: Stats (average, best session, trend, needs-more-data) computed purely from local Room data.

**Independent Test**: With ≥3 known-score sessions stored, open Stats offline → numbers match manual calculation; with <3 sessions, the "more data needed" message shows (SC-003 stats ≤ 1s).

### Tests for User Story 3 (write FIRST, ensure they FAIL) ⚠️

- [X] T032 [P] [US3] Update `test/kotlin/com/archeryscore/app/ui/stats/StatsViewModelTest.kt`: stats derive from local-only `observeStats()` with `needsMoreData` for <3 sessions, no auth

### Implementation for User Story 3

- [X] T033 [US3] `ui/stats/StatsViewModel.kt`: remove `AuthRepository`/`currentUserId`; call `statsRepository.observeStats(range)` without userId
- [X] T034 [US3] `data/repository/DefaultStatsRepository.kt`: confirm aggregation reads only `SessionRepository` (local Room); no remote/network path

**Checkpoint**: Statistics computed and accurate with no connectivity.

---

## Phase 6: User Story 4 - Clean Offline-First Experience (Priority: P3)

**Goal**: No sync/account/network concept anywhere in the app; CSV export keeps working offline; CSV **import** restores sessions (FR-014/FR-015/SC-009); upgrade preserves existing local sessions (SC-005).

**Independent Test**: Walk record/history/stats/export/import with no connectivity — zero sync/account UI; export then import the CSV; sessions reappear; re-importing the same file skips duplicates without changing data.

### Tests for User Story 4 (write FIRST, ensure they FAIL) ⚠️

- [x] T035 [P] [US4] Write `CsvImporterTest` in `test/kotlin/com/archeryscore/app/data/csv/`: round-trip property (export → import → export identical), per-field rejection rules from `contracts/csv-roundtrip.md`, atomicity on one bad row (zero writes), duplicate-`session_id` skip, empty-file/size guards
- [x] T036 [P] [US4] Write instrumented import test in `android/app/src/androidTest/kotlin/com/archeryscore/app/data/csv/`: import a previously exported file → sessions restored and visible (SC-009)

### Implementation for User Story 4

- [x] T037 [P] [US4] Implement `CsvImporter` + row validation in `data/csv/CsvImporter.kt` (RFC 4180 parse, exact header, per-field rules, structured error with row/column/reason, atomic result) (FR-015)
- [x] T038 [US4] Add `importSessions()` (group rows by `session_id` → COMPLETE sessions + ends + arrows, skip IDs already present) to `SessionRepository`/`RoomSessionRepository` (FR-014, idempotent)
- [x] T039 [US4] Add "Import CSV" action to `ui/history/HistoryScreen.kt` via SAF `ACTION_OPEN_DOCUMENT` (`text/csv`) + result snackbar/dialog ("imported N, skipped M")
- [x] T040 [P] [US4] Add import strings to `res/values/strings.xml` (import action/title, success, skipped, error messages)
- [x] T041 [US4] UI sweep: grep `ui/`, `domain/`, `data/` for `sync|network|account|upload|pending` and remove every leftover indicator/messaging

**Checkpoint**: No sync/account/network surface remains; export and import round-trip succeed offline.

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Governance amendment and end-to-end verification across all stories.

- [x] T042 [P] Amend `.specify/memory/constitution.md`: Principle III → On-Device-First Storage, Principle V → Local-Only Operation, drop Supabase/credentials rows + HTTPS-only constraint, update Sync Impact Report header, bump version `1.0.1 → 2.0.0` (FR-013, SC-008)
- [x] T043 [P] Re-align `.specify/templates/plan-template.md`, `spec-template.md`, `tasks-template.md` with on-device-first wording
- [x] T044 [P] Verify no network reachability per `quickstart.md`: merged manifest has no `INTERNET`/`ACCESS_NETWORK_STATE` (`./gradlew :app:processReleaseManifest`), no WorkManager jobs scheduled
- [x] T045 [P] Run `./gradlew :app:lintDebug` (zero errors) + dependency scan (no critical/high CVEs), perform unit + instrumented suite
- [x] T046 [P] Execute the `quickstart.md` validation checklist end-to-end (SC-001..SC-009, upgrade path v1→v2)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — teardown groundwork.
- **Foundational (Phase 2)**: Depends on Setup; **blocks all user stories**.
- **User Stories (Phase 3+)**: All depend on Foundational. US1 → US2 → US3 share the session-domain; run sequentially or in parallel per staffing.
- **US4 (Phase 6)**: Depends on Foundational (Room v2) + the record/history surfaces from US1/US2; includes the CSV import (FR-014) and the SC-005 upgrade verification.
- **Polish (Phase 7)**: Depends on all user stories; includes the constitution amendment (FR-013) which MUST land before merge.

### User Story Dependencies

- **US1 (P1)**: Foundational only — no other story. MVP.
- **US2 (P2)**: Foundational + US1's local-only repository (list mirrors session writes).
- **US3 (P2)**: Foundational + US1 data; independent of US2 (stats repository reads `SessionRepository` directly).
- **US4 (P3)**: Foundational + US2 history surface (import entry point); independent verification of export/import.

### Within Each User Story

- Tests (always included per constitution I) MUST be written and FAIL before implementation.
- Models before services; services before UI; core before integration; story complete before next priority.

### Parallel Opportunities

- Phase 1 teardown tasks T002–T005 run in parallel (distinct files).
- Phase 2 deletions T007–T011 run in parallel; entity/dao/db/migration tasks T013–T017 are distinct files but must land together before compile.
- T019–T023 (DataStore, Mapper, AppModule, interfaces, StatsRepo) parallel once T012–T018 are staged.
- US1..US3 view-model updates (T026/T029/T033) can be parallelized after Foundational.
- Phase 7 items T042–T046 parallel.

---

## Parallel Example: User Story 4

```bash
# Launch tests together (write first, expect FAIL):
Task: T035 "CsvImporterTest: round-trip + rejection + atomicity"
Task: T036 "Instrumented import test: export → import restores sessions"

# After tests pass, implementation in parallel:
Task: T037 "Implement CsvImporter in data/csv/CsvImporter.kt"
Task: T040 "Add import strings to res/values/strings.xml"
```

## Parallel Example: Foundational

```bash
# Parallel deletions:
Task: T007 "Delete data/sync/**  " (deleted files only)
Task: T008 "Delete data/network/NetworkMonitor.kt"
Task: T009 "Delete data/auth/** and its test"

# Parallel signature updates once T012–T016 land:
Task: T019 "DataStorePreferencesRepository: drop local_user_id + userId params"
Task: T022 "Repositories interfaces: drop userId params"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (teardown build).
2. Complete Phase 2: Foundational (CRITICAL — blocks all stories; Room v2 migration + auth/sync removal).
3. Complete Phase 3: User Story 1 — app opens directly to home, records/edits/finishes/resumes sessions entirely on-device.
4. **STOP and VALIDATE**: airplane-mode record → finish → restart → intact (SC-001/SC-002).
5. Individual commits per task with TDD evidence.

### Incremental Delivery

1. Setup + Foundational → foundation ready (build green, migration preserves data).
2. US1 → test offline recording → MVP demo.
3. US2 → offline history/detail → demo (no sync chips).
4. US3 → local statistics → demo.
5. US4 → CSV export-offline + import round-trip → clean offline-first complete.
6. Polish → constitution amendment (constitution.md v2.0.0) lands BEFORE merge (FR-013/SC-008).

### Parallel Team Strategy

With multiple developers:

1. Team completes Setup + Foundational together (single integration).
2. Once Foundational is done: Developer A → US1, Developer B → US3 (independent), then Developer C → US2 + US4.
3. Stories integrate independently; final integration in Phase 7.

---

## Notes

- [P] tasks = different files, no dependencies.
- [Story] label maps each task to a spec user story for traceability.
- Verify tests fail before implementing (constitution I — TDD evidence in commit history).
- Commit after each task or logical group; commit messages follow `type(scope): description` (constitution).
- Do NOT commit `node_modules/`, `package.json`, or leftover `supabase/` artifacts — teardown commit deletes them explicitly or leaves untracked.
- Stop at any checkpoint to validate the story independently.
- Avoid split-brain conflicts: each file is owned by one task within a phase.