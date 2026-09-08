# Tasks: Archery Score Tracking

**Input**: Design documents from `/specs/001-archery-score/`

**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, contracts/

**Tests**: Constitution Principle I mandates strict TDD (tests written BEFORE implementation, RED→GREEN→REFACTOR). Test tasks are REQUIRED and appear first in each story — not optional.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

Repo root layout per plan.md: single-module Android app under `android/`, Supabase SQL under `supabase/migrations/`.

- Unit tests: `android/app/src/test/kotlin/com/archeryscore/app/...`
- Instrumented/integration/Compose tests: `android/app/src/androidTest/kotlin/com/archeryscore/app/...`
- Main source: `android/app/src/main/kotlin/com/archeryscore/app/...`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization and basic structure

- [ ] T001 Create project structure per plan.md (`android/` single module, `gradle/wrapper`, `settings.gradle.kts`, `gradle.properties`, empty `domain/data/ui` packages)
- [ ] T002 Create version catalog `android/gradle/libs.versions.toml` with the full research.md version matrix (Kotlin 2.3.21, AGP 9.2.0, Compose BOM 2026.08.00, Hilt 2.60.1, Nav 2.10.0, Room 2.8.4, DataStore 1.2.1, WorkManager 2.11.0+, supabase-kt BOM 3.8.0, Ktor 3.4.0, MockK ≥1.14.6, Turbine 1.3.x)
- [ ] T003 Wire plugins in `android/app/build.gradle.kts` + root (Kotlin, KSP matching AGP 9.2.0, Hilt, Compose, Room KSP, JUnit5) — compileSdk 37 / targetSdk 36 / minSdk 26
- [ ] T004 [P] Configure lint (zero-error gate) + `local.properties.example` with `SUPABASE_URL`/`SUPABASE_ANON_KEY` placeholders (git-ignored per constitution II)
- [ ] T005 Amend constitution to v1.0.1 (CONSTITUTION_AMENDMENT_FOLLOWUP): update Tech table — Target SDK 36, Gradle 9.x, Kotlin 2.3.21 — in `.specify/memory/constitution.md`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure that MUST be complete before ANY user story can be implemented

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [ ] T006 Create `supabase/migrations/0001_init.sql` from contracts/storage.md (sessions/ends/arrows tables, check constraints, one-ACTIVE partial unique index, RLS policies); apply via Supabase CLI
- [ ] T007 [P] Room entities + DAOs mirroring data-model.md (Session, End, Arrow, Preferences, SyncWrite outbox) in `android/app/src/main/kotlin/com/archeryscore/app/data/local/`
- [ ] T008 [P] Auth: `AuthRepository` interface + Supabase `auth-kt` implementation per contracts/auth.md (signUp/signIn/restore/signOut, typed `AuthResult`)
- [ ] T009 [P] Hilt DI modules (`di/`): Room database, SupabaseClient (URL+anon key from BuildConfig/local.properties), DataStore, NetworkMonitor, Dispatchers
- [ ] T010 [P] Sync infrastructure: `SyncWrite` outbox + `SyncRunner` (push→confirm→pull, LWW by `edited_at`) + `SyncWorker` (WorkManager, network constraint, backoff, attempt cap 10) per contracts/data-sync.md
- [ ] T011 [P] Connectivity awareness: `NetworkMonitor` + WorkManager `NetworkType.CONNECTED` constraint wiring
- [ ] T012 Sync status derivation: `SyncStatusRepository` (SYNCED/PENDING/ERROR per session, FR-010) per contracts/data-sync.md
- [ ] T013 Navigation shell: `MainActivity`, `ArcheryScoreApp`, Material 3 theme, `NavHost` with auth/session/history/statistics routes in `android/app/src/main/kotlin/com/archeryscore/app/ui/`
- [ ] T014 DataStore preferences repository (defaults: roundType, endCount, arrowsPerEnd, distance, discipline, x-ring pref) per data-model.md PREFERENCES
- [ ] T015 Mappers + repository interface layer: `android/app/src/main/kotlin/com/archeryscore/app/data/repository/` (local↔remote entity mapping, `SessionDataSource`/`AuthDataSource` interfaces)

**Checkpoint**: Foundation ready - user story implementation can now begin in parallel

---

## Phase 3: User Story 1 - Record an Archery Round (Priority: P1) 🎯 MVP

**Goal**: Create configurable sessions, enter arrow scores with instant running total, validate against round type, resume in-progress sessions, edit completed scores with confirmation, all offline-first.

**Independent Test**: Create a session → enter scores → verify running total updates ≤100ms and persists after force-restart → complete → reconnect → Supabase rows match.

### Tests for User Story 1 (write FIRST, confirm RED) ⚠️

- [ ] T016 [P] [US1] Unit test `ScoreValidator` (FR-002/011: 1-10 TEN_ZONE, 1-5 FIVE_ZONE, X-ring rule `score==10` only, invalid negatives/max throw) in `android/app/src/test/.../domain/userguide/ScoreValidatorTest.kt`
- [ ] T017 [P] [US1] Unit test `SessionCalculator` (FR-003: running total, final score, X-count) in `android/app/src/test/.../domain/userguide/SessionCalculatorTest.kt`
- [ ] T018 [P] [US1] Unit test `ResumeSessionViewModel` (FR-015: auto-restore ACTIVE on launch, block second ACTIVE — spec edge case) in `android/app/src/test/.../ui/session/ResumeSessionViewModelTest.kt`
- [ ] T019 [P] [US1] Unit test `EditScoreUseCase` (FR-014: COMPLETE edit requires confirmation; confirmed edit bumps `edited_at` + enqueues SyncWrite) in `android/app/src/test/.../domain/userguide/EditScoreUseCaseTest.kt`
- [ ] T020 [P] [US1] Unit test `RecordSessionViewModel` (FR-001 configurable ends/arrows, FR-017 distance+discipline) in `android/app/src/test/.../ui/session/RecordSessionViewModelTest.kt`
- [ ] T021 [US1] Integration test: offline score entry → reconnect → SyncWorker flushes outbox → Supabase rows match; LWW — simulate device A/B same arrow, later `edited_at` wins (FR-016, contracts/data-sync.md)

### Implementation for User Story 1

- [ ] T022 [US1] `ScoreValidator` in `domain/model/` (drives FR-002 validation, used by UI + repository)
- [ ] T023 [US1] `SessionCalculator` in `domain/userguide/` (running total + final totals, FR-003)
- [ ] T024 [US1] `SessionRepository` impl: Room-first writes, outbox enqueue, Supabase push (FR-004/005)
- [ ] T025 [US1] `RecordSessionViewModel` (FR-001 create, FR-017 metadata; runs totals via T023)
- [ ] T026 [US1] `ResumeSessionViewModel` (FR-015 restore + ACTIVE invariant, spec edge case handling)
- [ ] T027 [US1] `EditScoreUseCase` + confirmation dialog (FR-014)
- [ ] T028 [US1] `RecordSessionScreen` + arrow input grid + running total header (Compose, M3; sync indicator chip FR-010) in `ui/session/`
- [ ] T029 [US1] Wire persistence on every arrow save (synchronous Room write — spec edge case "force-close mid-session")

**Checkpoint**: At this point, User Story 1 should be fully functional and testable independently (offline + sync)

---

## Phase 4: User Story 2 - View Score History (Priority: P2)

**Goal**: Reverse-chronological session list, per-end detail breakdown, session deletion with confirmation, CSV export via share sheet, empty state.

**Independent Test**: Create multiple sessions → verify history order, detail breakdown, delete-with-confirmation, and CSV share intent.

### Tests for User Story 2 (write FIRST, confirm RED) ⚠️

- [ ] T030 [P] [US2] Unit test `HistoryViewModel` (FR-006 reverse-chron sorting, FR-012 empty state, sync status indicators FR-010) in `android/app/src/test/.../ui/history/HistoryViewModelTest.kt`
- [ ] T031 [P] [US2] Unit test `SessionDetailMapper` (FR-007 per-end breakdown + per-end totals) in `android/app/src/test/.../domain/userguide/SessionDetailMapperTest.kt`
- [ ] T032 [P] [US2] Unit test `DeleteSessionUseCase` (FR-009: confirmation, cascade delete, DELETE outbox coalescing per contracts/data-sync.md) in `android/app/src/test/.../domain/userguide/DeleteSessionUseCaseTest.kt`
- [ ] T033 [P] [US2] Unit test `CsvExporter` (contracts/csv-export.md: exact header, RFC-4180 escaping, ordering, UTF-8) in `android/app/src/test/.../data/export/CsvExporterTest.kt`
- [ ] T034 [US2] Instrumented test: CSV share flow produces `content://` URI via FileProvider, `text/csv` (contracts/csv-export.md)

### Implementation for User Story 2

- [ ] T035 [US2] History repository queries (`android/app/src/main/kotlin/com/archeryscore/app/data/repository/`) — date-desc, per-session sync status join
- [ ] T036 [US2] `HistoryViewModel` + list screen (FR-006, FR-012 empty state, FR-010 indicators) in `ui/history/`
- [ ] T037 [US2] `SessionDetailViewModel` + detail screen (FR-007 breakdown, per-end stats, edit entry point FR-014)
- [ ] T038 [US2] Delete flow (FR-009 confirmation dialog + cascade + DELETE outbox)
- [ ] T039 [US2] `CsvExporter` + `FileProvider` + share sheet (FR-018, contracts/csv-export.md)

**Checkpoint**: At this point, User Stories 1 AND 2 should both work independently

---

## Phase 5: User Story 3 - Track Performance Statistics (Priority: P3)

**Goal**: Average score, best session, improvement trend across completed sessions, with date-range filtering and a "not enough data" state.

**Independent Test**: Create sessions with known scores → verify averages/best/trend math and date-range filter; fewer than 3 sessions shows the meaningful-trend hint.

### Tests for User Story 3 (write FIRST, confirm RED) ⚠️

- [ ] T040 [P] [US3] Unit test `StatsCalculator` (FR-008: average per session, best session, improvement trend; X-count edge cases) in `android/app/src/test/.../domain/userguide/StatsCalculatorTest.kt`
- [ ] T041 [P] [US3] Unit test `StatsFilter` (FR-008 scenario 2 date-range filter; scenario 3 <3 sessions → "need more data" state) in `android/app/src/test/.../domain/userguide/StatsFilterTest.kt`

### Implementation for User Story 3

- [ ] T042 [US3] `StatsCalculator` in `domain/userguide/`
- [ ] T043 [US3] `StatsRepository` impl — aggregations over ≤500 sessions (SC-005: ≤1s), completed sessions only
- [ ] T044 [US3] `StatisticsViewModel` + screen (avg/best/trend, date-range chips) in `ui/statistics/`

**Checkpoint**: All user stories should now be independently functional

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Improvements that affect multiple user stories

- [ ] T045 [P] Compose UI tests for critical journeys (constitution I): score entry (US1) + session detail (US2) in `android/app/src/androidTest/.../ui/`
- [ ] T046 [P] Edge-case tests: invalid score entry (Esc), network failure mid-sync (Esc 2), Supabase quota → ERROR status + notification (Esc 4) per contracts/data-sync.md
- [ ] T047 Run quickstart.md validation end-to-end: `testDebugUnitTest`, `lintDebug` (zero errors), `connectedDebugAndroidTest`
- [ ] T048 CVE gate: `dependencyCheckAggregate` — no critical/high vulnerabilities (constitution II; blocks merge)
- [ ] T049 Performance verification: score entry ≤100ms (SC-002), stats ≤1s @ 500 sessions (SC-005), cold start <2s, history ≤2s (SC-004)
- [ ] T050 APK gate: `assembleRelease` (R8) ≤15MB (constitution constraint), install + smoke test on Android 8.0+ device (FR-013, SC-006)
- [ ] T051 Final governance review: TDD evidence in commit history (test-first), constitution compliance, `AGENTS.md`/docs accurate — before merge to `develop`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Stories (Phase 3+)**: All depend on Foundational phase completion
  - Implement sequentially in priority order (P1 → P2 → P3) to respect the one-ACTIVE-session invariant and shared session data
- **Polish (Final Phase)**: Depends on all desired user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: Starts after Foundational; no dependencies on other stories
- **User Story 2 (P2)**: Starts after Foundational; integrates with US1 data (sessions/ends/arrows) but independently testable
- **User Story 3 (P3)**: Starts after Foundational; consumes completed sessions from US1 (+ optional US2 data) but independently testable

### Within Each User Story

- Tests MUST be written and FAIL (RED) before implementation (constitution I)
- Models → use cases → repositories → ViewModels → UI
- Story complete and verified before moving to the next priority

### Parallel Opportunities

- All Setup tasks marked [P] run in parallel (T004, T005)
- All Foundational tasks marked [P] run in parallel (T007..T011, T015) after T006 schema
- Tests within a story marked [P] run in parallel (e.g., T016..T020, T030..T033, T040..T041)
- T045/T046 in final phase run in parallel

---

## Parallel Example: User Story 1

```bash
# Launch all US1 test tasks together (RED phase, TDD):
Task: "Unit test ScoreValidator in .../ScoreValidatorTest.kt"   (T016)
Task: "Unit test SessionCalculator in .../SessionCalculatorTest.kt" (T017)
Task: "Unit test ResumeSessionViewModel in .../ResumeSessionViewModelTest.kt" (T018)
Task: "Unit test EditScoreUseCase in .../EditScoreUseCaseTest.kt" (T019)
Task: "Unit test RecordSessionViewModel in .../RecordSessionViewModelTest.kt" (T020)
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (CRITICAL - blocks all stories)
3. Complete Phase 3: User Story 1 (tests FIRST, RED→GREEN)
4. **STOP and VALIDATE**: Test User Story 1 independently (unit + integration + manual APK)
5. Deploy/demo if ready

### Incremental Delivery

1. Complete Setup + Foundational → Foundation ready (Room + Supabase + Auth + Sync)
2. Add User Story 1 → Test independently → Demo (MVP! APK build)
3. Add User Story 2 → Test independently → Demo
4. Add User Story 3 → Test independently → Demo
5. Each story adds value without breaking previous stories

### Parallel Team Strategy

With multiple developers:

1. Team completes Setup + Foundational together
2. Once Foundational is done:
   - Developer A: User Story 1 (+ blocks others)
   - Developer B: prepares US2 tests (RED) while US1 implements shared session data
   - Developer C: prepares US3 tests (RED)
3. Stories complete and integrate independently

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story is independently completable and testable
- Verify tests fail before implementing (RED), commit test-first evidence per constitution I
- Commit after each task or logical group (`type(scope): description` per constitution commit convention)
- Stop at any checkpoint to validate story independently
- Avoid: vague tasks, same-file conflicts, cross-story dependencies that break independence
- Spec edge cases mapped: invalid score → T016; network failure mid-sync → T046; force-close mid-session → T029; Supabase quota → T046; create-while-ACTIVE → T018/T026
- FR-005/010/016 (offline queue, sync indicators, LWW) are delivered by Foundational (T010) + consumed in UI tasks (T028, T036)
- Constitution amendment T005 must land before merge (CONSTITUTION_AMENDMENT_FOLLOWUP in plan.md)