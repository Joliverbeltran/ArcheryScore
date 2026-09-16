---

description: "Task list for feature 003-target-arrow-input implementation"
---

# Tasks: Visual Target Arrow Placement

**Input**: Design documents from `/specs/003-target-arrow-input/`

**Prerequisites**: plan.md (required), spec.md (required for user stories — US1/US2 P1, US3 P2), research.md, data-model.md, contracts/target-input.md, contracts/storage.md

**Tests**: Test tasks are included — the constitution mandates TDD (gate I, non-negotiable) and plan.md lists the test matrix. Tests are written FIRST (RED) before their implementation.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story. User stories from spec.md, in priority order:
- **US1 (P1)**: Place Arrows Visually on a Target Face
- **US2 (P1)**: Choose the Target Type Up Front
- **US3 (P2)**: Automatic and Correct Scoring from Placement

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (US1, US2, US3)
- Mobile app: all code lives under `android/app/src/` (on-device; no backend layer)

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Verify the baseline and add cross-cutting shared resources (strings) before any code changes.

- [X] T001 [P] Run baseline verification `./gradlew :app:testDebugUnitTest :app:lintDebug` from `android/` on branch `003-target-arrow-input` and confirm green before any source changes
- [X] T002 [P] Add target-type and placement string resources to `android/app/src/main/res/values/strings.xml`: six target labels ("122 cm", "80 cm", "60 cm", "40 cm", "Triple vertical", "Triple triangular"), dialog title/OK/cancel, score-badge content descriptions (M / X / ring). No hardcoded strings in code (constitution constraints).

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Persistence of the target type — `TargetType` enum, `Session.targetType`, Room v3 (`MIGRATION_2_3`), DataStore `default_target_type`, Mapper, and the 10-column CSV format. MUST be complete before ANY user story (US1 displays the face, US2 selects it, US3 scores against it).

**⚠️ CRITICAL**: No user story work can begin until this phase is complete.

### Tests for Foundational (write FIRST, ensure they FAIL) ⚠️

- [X] T003 [P] Write RoomMigration23Test in `android/app/src/androidTest/kotlin/com/archeryscore/app/data/local/RoomMigration23Test.kt` asserting v2→v3 migration preserves sessions/ends/arrows and backfills `target_type='CM122'` (RED until `MIGRATION_2_3` exists)
- [X] T004 [P] Write UserPreferencesTargetTypeTest in `android/app/src/test/kotlin/com/archeryscore/app/domain/model/UserPreferencesTargetTypeTest.kt` asserting `defaultTargetType` defaults to `CM122` and persists/round-trips through DataStore (RED until implemented)
- [X] T005 [P] Write CsvRoundTripTargetTypeTest in `android/app/src/test/kotlin/com/archeryscore/app/data/csv/CsvRoundTripTargetTypeTest.kt` asserting the 10-column header round-trips byte-identically and legacy 9-column files import with `target_type='CM122'` (RED until implemented)

### Implementation for Foundational

- [X] T006 [P] Create `TargetType` enum (`CM122`, `CM80`, `CM60`, `CM40`, `TRIPLE_VERTICAL`, `TRIPLE_TRIANGULAR`) with string-res label reference and spot-layout constants (D-3: 2.2R spacing; vertical `(0,−2.2),(0,0),(0,+2.2)`; triangular `(0,−1.1),(−1.1,+0.95),(+1.1,+0.95)`) in `android/app/src/main/kotlin/com/archeryscore/app/domain/model/TargetType.kt` (satisfies T003/T004/T005)
- [X] T007 [P] Add `targetType: TargetType = TargetType.CM122` to the `Session` model in `android/app/src/main/kotlin/com/archeryscore/app/domain/model/Session.kt`
- [X] T008 [P] Add `target_type: String` column to `SessionEntity` in `android/app/src/main/kotlin/com/archeryscore/app/data/local/entity/Entities.kt`
- [X] T009 [P] Add `MIGRATION_2_3` (`ALTER TABLE sessions ADD COLUMN target_type TEXT NOT NULL DEFAULT 'CM122'`) and bump DB version to 3 in `android/app/src/main/kotlin/com/archeryscore/app/data/local/AppDatabase.kt` (satisfies T003)
- [X] T010 [P] Add `defaultTargetType: TargetType = TargetType.CM122` to `UserPreferences` in `android/app/src/main/kotlin/com/archeryscore/app/domain/model/UserPreferences.kt`
- [X] T011 [P] Add `default_target_type` DataStore key with safe parse (missing/invalid name → `CM122`, matching existing roundType/discipline pattern) in `android/app/src/main/kotlin/com/archeryscore/app/data/prefs/DataStorePreferencesRepository.kt` (satisfies T004)
- [X] T012 [P] Add `TargetType.name` ↔ enum mapping in `android/app/src/main/kotlin/com/archeryscore/app/data/mapper/Mapper.kt`
- [X] T013 Extend CsvExporter to emit `target_type` after `round_type` (10-column header) with enum-name values, in `android/app/src/main/kotlin/com/archeryscore/app/data/csv/CsvExporter.kt` (depends T006, T007, T008)
- [X] T014 Extend CsvImporter to parse the new 10-column header and accept the legacy 9-column header (rows default to `CM122`), keeping all existing row-validation rules and atomic-abort behavior, in `android/app/src/main/kotlin/com/archeryscore/app/data/csv/CsvImporter.kt` (depends T006, T013; satisfies T005)

**Checkpoint**: Foundation ready — target type flows through Room, DataStore, Mapper, and CSV. User story implementation can now begin.

---

## Phase 3: User Story 1 - Place Arrows Visually on a Target Face (Priority: P1) 🎯 MVP

**Goal**: Replace numeric score entry with tap/drag/OK visual placement: tap sets a pending marker, drag refines it, OK persists the derived score and auto-advances through the end, cancel discards, running total updates live, and misplacements can be corrected within the end.

**Independent Test**: Start a new session (default 122 cm face), place all arrows of one end by tap-and-drag on the target, tap OK each time; verify each arrow is stored with the correct score and the running total updates (FR-003..FR-012, FR-015, SC-001/SC-002/SC-005).

### Tests for User Story 1 (write FIRST, ensure they FAIL) ⚠️

- [X] T015 [P] [US1] Write PlacementScorerTest in `android/app/src/test/kotlin/com/archeryscore/app/domain/model/PlacementScorerTest.kt` covering ring values for `TEN_ZONE` and `FIVE_ZONE`, boundary touch → higher (inner) value, `r ≥ 1` → miss (0), X only when 10-zone and `r ≤ 0.5·w`, and the FIVE_ZONE band mapping (5 bands, `isX` always false) (RED until `PlacementScorer` exists; matches `contracts/target-input.md` score table)
- [X] T016 [P] [US1] Write TargetPlacementFlowTest in `android/app/src/androidTest/kotlin/com/archeryscore/app/ui/record/TargetPlacementFlowTest.kt` (Compose, instrumented) covering tap→marker appears, drag→marker follows finger, OK→persisted + advances to next slot, cancel/back→discarded nothing persisted, and OK-without-tap does not persist an arrow (RED until dialog exists)

### Implementation for User Story 1

- [X] T017 [US1] Implement `PlacementScorer` — pure domain `place(x, y, targetType, scoringType): PlacementResult(score, isXRing, miss, spotIndex)` with WA equal-width-zone math (D-2/R-2/R-3), boundary→higher value, `r≥1` miss, X at `r ≤ 0.5·w`, and nearest-spot resolution for triple faces (FR-007..FR-010) in `android/app/src/main/kotlin/com/archeryscore/app/domain/model/PlacementScorer.kt` (depends T015)
- [X] T018 [US1] Create `TargetFace` composable — Canvas renderer drawing the 5-colour/10-zone face (colours, X ring, thin dividing lines) for single faces and triple layouts (vertical/triangular per D-3), normalized-to-screen mapping, in `android/app/src/main/kotlin/com/archeryscore/app/ui/components/TargetFace.kt` (depends T006, T017)
- [X] T018b [P] [US1] Write TargetFaceFitTest in `android/app/src/androidTest/kotlin/com/archeryscore/app/ui/components/TargetFaceFitTest.kt` (Compose) asserting aspect-correct, inset-based scaling keeps the full face reachable and tap-accurate in constrained and rotated containers (edge case: small screens/rotated device; RED until inset handling exists)
- [X] T018c [US1] Ensure `TargetFace` insets (never stretches) to its bounds, centers within the usable area, and keeps the full face reachable/tappable on small and rotated screens (U1) in `android/app/src/main/kotlin/com/archeryscore/app/ui/components/TargetFace.kt` (depends T018, T018b)
- [X] T019 [US1] Add placement flow state to `ActiveSessionViewModel` (pending marker position, current arrow slot, provisional score badge, live running total) and extend `ActiveSessionViewModelTest` in `android/app/src/test/kotlin/com/archeryscore/app/ui/record/ActiveSessionViewModelTest.kt` (depends T017)
- [X] T020 [US1] Create `TargetPlacementDialog` — tap sets pending marker, drag refines, OK persists `(score, isX)` via existing `EditScoreUseCase` then auto-advances to `arrow_number + 1`, last arrow completes the end, off-face placement clamped then resolves as miss (0), FR-005 no persistence until OK, provisional badge shows band value for `FIVE_ZONE` (1–5) and ring value for `TEN_ZONE` (1–10, X) — in `android/app/src/main/kotlin/com/archeryscore/app/ui/record/TargetPlacementDialog.kt` (depends T018, T019)
- [X] T021 [US1] Replace the numeric `ScoreDialog` with `TargetPlacementDialog` for active-end score entry in `android/app/src/main/kotlin/com/archeryscore/app/ui/record/ActiveSessionScreen.kt` (depends T020; satisfies FR-006, FR-011)
- [X] T022 [US1] Enable correction within the current end (FR-015, SC-007): tapping a confirmed arrow chip opens the placement dialog in correction mode showing the previous stored value in the header and an **unplaced** marker (clear confirmation step, no silent overwrite — see `contracts/target-input.md`); OK re-persists via `EditScoreUseCase` and auto-advances through the remaining arrows; cancel preserves the original value, in `android/app/src/main/kotlin/com/archeryscore/app/ui/record/TargetPlacementDialog.kt` + `ActiveSessionScreen.kt` (depends T021)

**Checkpoint**: A full end is recordable with only tap, drag, and OK; scores are correct against the 122 cm face; totals update instantly. US1 fully functional and testable independently.

---

## Phase 4: User Story 2 - Choose the Target Type Up Front (Priority: P1)

**Goal**: Six target options in the front (session set-up) menu; the chosen face is shown for every placement in the session and the last choice is remembered as the default.

**Independent Test**: Select each of the six options in the front menu; confirm exactly six options are shown, the matching face appears during placement, and scores map to that face (FR-001/002/013, SC-004/SC-005).

### Tests for User Story 2 (write FIRST, ensure they FAIL) ⚠️

- [X] T023 [P] [US2] Write StartScreenTargetTypeTest in `android/app/src/androidTest/kotlin/com/archeryscore/app/ui/start/StartScreenTargetTypeTest.kt` (Compose, instrumented) asserting exactly six target options are listed and the selection is pre-selected from `default_target_type` (RED until dropdown exists)
- [X] T024 [P] [US2] Write ActiveSessionTargetPersistenceTest in `android/app/src/test/kotlin/com/archeryscore/app/ui/record/ActiveSessionTargetPersistenceTest.kt` asserting a session created with a chosen `targetType` surfaces that type in the placement dialog consistently for all arrows of the session (FR-013, RED until wiring exists)

### Implementation for User Story 2

- [X] T025 [US2] Add `TargetTypeDropdown` presenting exactly six options (FR-002) to the session set-up menu (UI only; state driven by `ResumeSessionViewModel`) in `android/app/src/main/kotlin/com/archeryscore/app/ui/start/StartScreen.kt` (depends T006, T010, T027)
- [X] T026 [US2] Persist the chosen `targetType` into `sessions.target_type` on session creation (FR-013) via `ResumeSessionViewModel.createSession(...)` backed by the session repository in `android/app/src/main/kotlin/com/archeryscore/app/ui/resume/ResumeSessionViewModel.kt` (depends T007, T008, T012, T025)
- [X] T027 [US2] Persist the selection as `default_target_type` and pre-select it for the next new session (R-6, FR-016, SC-004) in `android/app/src/main/kotlin/com/archeryscore/app/ui/resume/ResumeSessionViewModel.kt`, reading the saved default via `DataStorePreferencesRepository` (depends T011, T023)
- [X] T028 [US2] Wire the session's persisted `targetType` into the placement dialog so the displayed face and scoring match the chosen target for the whole session (FR-001) in `android/app/src/main/kotlin/com/archeryscore/app/ui/record/ActiveSessionViewModel.kt` (depends T007, T021, T023)

**Checkpoint**: US1 AND US2 both work independently — full session with a chosen face, remembered default, consistent display and scoring.

---

## Phase 5: User Story 3 - Automatic and Correct Scoring from Placement (Priority: P2)

**Goal**: Verified score correctness — exhaustive ring/boundary/miss coverage, triple-face spot assignment, previously confirmed arrows kept visible (grouping), and full SC-003/SC-002 verification.

**Independent Test**: Place markers at known positions (inside a ring, exactly on a boundary, between/outside a triple's spots, completely outside the face) and verify the confirmed score matches standard archery rules in every case (FR-008/009/010/012, SC-003).

### Tests for User Story 3 (write FIRST, ensure they FAIL) ⚠️

- [X] T029 [P] [US3] Write TripleLayoutTest in `android/app/src/test/kotlin/com/archeryscore/app/domain/model/TripleLayoutTest.kt` asserting the spot-center layout constants (T006) and nearest-spot assignment for taps inside a spot, between spots, and outside all three spots (verifies FR-010/SC-005)
- [X] T030 [P] [US3] Extend PlacementScorerTest (T015) with an exhaustive boundary matrix — every ring boundary for `TEN_ZONE` (10 rings) and every band boundary for `FIVE_ZONE` (5 bands) scores the higher value, plus `r ≥ 1` miss and X-rule re-verification (SC-003)
- [X] T031 [P] [US3] Write TargetMarkersVisibleTest in `android/app/src/androidTest/kotlin/com/archeryscore/app/ui/record/TargetMarkersVisibleTest.kt` (Compose, instrumented) asserting previously confirmed arrows of the current end stay visible on the target during subsequent placement (FR-012, RED until implemented)

### Implementation for User Story 3

- [X] T032 [US3] Render previously confirmed arrows of the current end as static markers on the target while placing subsequent arrows (FR-012, grouping comparison) in `android/app/src/main/kotlin/com/archeryscore/app/ui/record/TargetPlacementDialog.kt` via `TargetFace` (depends T031, T018)
- [X] T033 [US3] Ensure every OK-confirmed placement resolves exclusively through `PlacementScorer` (incl. triple nearest-spot) to `EditScoreUseCase` with zero numeric entry, covering miss and X storage (SC-002) in `android/app/src/main/kotlin/com/archeryscore/app/ui/record/TargetPlacementDialog.kt` (depends T017, T020)
- [X] T034 [US3] Add accessibility content descriptions (scoring zones, X ring, badge labels using T002 strings) and confirm Compose a11y lint clean in `android/app/src/main/kotlin/com/archeryscore/app/ui/components/TargetFace.kt` (inherited a11y constraints)

**Checkpoint**: All user stories independently functional. Scoring verified against the official rule set; previous arrows visible; a11y clean.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Regression, verification against all success criteria, and merge hygiene.

- [X] T035 [P] Run the full quickstart verification `./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:connectedAndroidTest` from `android/` and record SC-001..SC-007 results against `specs/003-target-arrow-input/quickstart.md` checklist
- [X] T036 [P] Verify zero regression on legacy data (FR-014/SC-006): run the existing suites `RoomMigrationTest`, `ImportRestoreTest`, and confirm a pre-existing numeric session still opens, totals/stats unchanged, and legacy CSV import works
- [X] T037 Review the full diff for quality gates: no hardcoded strings, no new dependencies, no network surface, TDD evidence present (tests commit before implementation) — summarize in the PR description

**Verification record (Phase 6)**: `:app:testDebugUnitTest` 123 tests, 0 failures, 0 errors; `:app:lintDebug` clean; `:app:assembleDebug` + `:app:compileDebugAndroidTestKotlin` green (instrumented suites — Compose flow/fit/menu/markers, `RoomMigration23Test`, `ImportRestoreTest` — compile-verified; `connectedAndroidTest` requires an emulator, unavailable in this environment). Regression suites intact; legacy 9-col CSV import and Room 2→3 migration unchanged. TDD evidence: RED tests authored before each implementation task across T015–T031 (working tree uncommitted until merge); no runtime dependencies added (`ui-test-junit4`/`ui-test-manifest` are test-only, permitted by plan.md line 21); no manifest/network changes.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — can start immediately
- **Foundational (Phase 2)**: Depends on Setup (T002 strings) — BLOCKS all user stories (target type must persist/view before any UI uses it)
- **User Stories (Phase 3+)**: All depend on Foundational completion
  - **US1**: Depends on Foundational (data layer + TargetType); its own tasks sequential (T015→T016 scorable tests → scorer → UI → dialog → screen wiring)
  - **US2**: Depends on Foundational + US1 UI artifacts (TargetFace/TargetPlacementDialog); integrates with US1 but is independently testable via the menu
  - **US3**: Depends on Foundational + US1; additive verification/hardening on top — does not alter US1/US2 behavior
- **Polish (Final Phase)**: Depends on all desired user stories complete

### User Story Dependencies

- US1 (P1): after Foundational — no dependency on other stories
- US2 (P1): after Foundational — uses US1's `TargetFace` + placement dialog; independent test = menu selection
- US3 (P2): after Foundational + US1 — exhaustive scoring verification, triple assignment, marker visibility

### Within Each Phase

- Tests (RED) MUST be written and FAIL before implementation
- Per task: [P] tasks are independent — run in any order; non-[P] tasks list their dependency explicitly
- Phase 2: T003/T004/T005 (tests) → T006 (satisfies them) → T007..T012 [P] → T013 → T014
- Phase 3: T015 ∥ T016 (RED tests) → T017 → T018 → T018b (∥) → T018c → T019 → T020 → T021 → T022
- Phase 4 order: T023 ∥ T024 → T027 (ViewModel default) → T025 (dropdown UI) → T026 (session create) → T028 (wire into dialog)

### Parallel Opportunities

- Phase 1: T001 ∥ T002
- Phase 2: T003/T004/T005 (three test files) then T007..T012 (six independent files) in parallel before T013/T014
- Phase 3: T015 ∥ T016; then T018/T019 after T017 (T018b ∥ after T018, T018c after T018b)
- Phase 4: T023 ∥ T024; then T027 → T025 → T026 → T028
- Phase 5: T029 ∥ T030 ∥ T031
- Phase 6: T035/T036/T037 in parallel
- Different user stories can be worked in parallel by different engineers once Foundational is done (US1 does not depend on US2; US3 hardens after US1)

---

## Parallel Example: Phase 2 Foundational

```bash
# Launch all foundational tests together:
Task: "Write RoomMigration23Test in android/app/src/androidTest/kotlin/com/archeryscore/app/data/local/RoomMigration23Test.kt"
Task: "Write UserPreferencesTargetTypeTest in android/app/src/test/kotlin/com/archeryscore/app/domain/model/UserPreferencesTargetTypeTest.kt"
Task: "Write CsvRoundTripTargetTypeTest in android/app/src/test/kotlin/com/archeryscore/app/data/csv/CsvRoundTripTargetTypeTest.kt"

# Launch independent data-layer tasks together:
Task: "Add targetType to Session.kt"
Task: "Add target_type to SessionEntity in Entities.kt"
Task: "Bump version 3 + MIGRATION_2_3 in AppDatabase.kt"
Task: "Add default_target_type key in DataStorePreferencesRepository.kt"
Task: "Add TargetType mapping in Mapper.kt"
```

## Parallel Example: User Story 1

```bash
# Launch the RED test suites together:
Task: "Write PlacementScorerTest in domain/model/PlacementScorerTest.kt"
Task: "Write TargetPlacementFlowTest in ui/record/TargetPlacementFlowTest.kt (androidTest)"
Task: "Write TargetFaceFitTest in ui/components/TargetFaceFitTest.kt (androidTest)"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Phase 1: Setup (T001, T002)
2. Phase 2: Foundational — CRITICAL, blocks all stories (T003..T014)
3. Phase 3: User Story 1 (T015..T022) — default `CM122` face, tap/drag/OK flow, auto-advance, totals, correction
4. **STOP and VALIDATE**: US1 independent test (full end via tap/drag/OK under 45s, correct scores, running total updates)
5. Demo-ready MVP: visual placement replaces numeric entry for new sessions

### Incremental Delivery

1. Setup + Foundational → target type persists end-to-end (Room v3, DataStore, CSV 10-col, legacy import green)
2. Add US1 → test independently (visual placement MVP, default face)
3. Add US2 → test independently (six-option menu, remembered default, consistent face/score mapping)
4. Add US3 → test independently (exhaustive boundary/triple scoring, previous arrows visible, a11y)
5. Each story adds value without breaking numeric entry on existing sessions (FR-014)

### Parallel Team Strategy

With multiple developers (after Phase 2 done):

1. Developer A: US1 (placement flow)
2. Developer B: US2 (menu + retention) — can start once `TargetFace`/dialog land, or implement dropdown against the Foundational model and wire at the US2 checkpoint
3. Developer C: US3 tests (T029/T030) can be authored in parallel against the plan's `PlacementScorer`/`contracts/target-input.md` contract even before T017 lands; T031/T032 land when the dialog exists

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps the task to its spec user story for traceability
- Each user story is independently completable and testable (checkpoints above)
- RED tests must fail before their implementation task
- Commit after each task or logical group (per commit skill); TDD evidence required at merge
- Stop at any checkpoint to validate the story independently
- Avoid: vague tasks, same-file conflicts, cross-story dependencies that break independence (only PHASE order creates dependencies)
- Storage for placement coordinates is explicitly out of scope (spec Assumption: confirmed arrows are numeric only) — no `placement` column or arrow-schema change