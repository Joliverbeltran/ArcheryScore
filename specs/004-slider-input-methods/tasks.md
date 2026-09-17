---
description: "Task list for feature 004-slider-input-methods"
---

# Tasks: Step-Slider Session Input Methods

**Input**: Design documents from `/specs/004-slider-input-methods/`

**Prerequisites**: [`plan.md`](./plan.md), [`spec.md`](./spec.md), [`research.md`](./research.md), [`data-model.md`](./data-model.md), [`contracts/`](./contracts/), [`quickstart.md`](./quickstart.md)

**Tests**: REQUIRED. The ArcheryScore Constitution (Principle I, NON-NEGOTIABLE) mandates strict TDD. Every implementation task is preceded by a failing test task (RED → GREEN).

**Organization**: Tasks are grouped by user story to enable independent implementation and testing. All three P1 stories (US1–US3) share the foundational `SessionSetupOptions` object and `OptionSlider` component, created once in Phase 2.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies on incomplete tasks)
- **[Story]**: Which user story this task belongs to (US1, US2, US3, US4)
- Every task includes its exact file path

## Path Conventions

Mobile app, single Gradle module `:app` under `android/`:
- Production: `android/app/src/main/kotlin/com/archeryscore/app/`
- Unit tests: `android/app/src/test/kotlin/com/archeryscore/app/`
- Instrumented tests: `android/app/src/androidTest/kotlin/com/archeryscore/app/`
- Resources: `android/app/src/main/res/values/`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Confirm the baseline — this feature adds **no** new dependency (Material 3 `Slider` already ships with the Compose BOM).

- [ ] T001 Confirm `android/app/build.gradle.kts` needs no new dependency and run `./gradlew testDebugUnitTest` from `android/` to establish a green baseline before any change.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: The pure option catalog, the reusable discrete slider control, and the value/accessibility strings that **all three** user stories depend on.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete.

- [ ] T002 [P] Write RED unit tests for the option catalog in `android/app/src/test/kotlin/com/archeryscore/app/domain/model/SessionSetupOptionsTest.kt` — exact ascending/unique lists (`DISTANCES_M = [8,12,18,30,40,50,70,90]`, `ARROWS_PER_END = [1,3,6]`, `END_COUNTS = [1,3,6,9,12]`), index↔value round-trip, and `nearest()` reference vectors incl. ties→larger and clamps at `Int.MIN_VALUE`/`Int.MAX_VALUE` (contracts C1–C3).
- [ ] T003 [P] Add slider value-label and accessibility strings to `android/app/src/main/res/values/strings.xml` — `distance_value` (`%1$d m`), `end_count_value` (`%1$d`), `arrows_per_end_value` (`%1$d`), plus content-description formats naming each control and its value (contract C6).
- [ ] T004 Implement `SessionSetupOptions` in `android/app/src/main/kotlin/com/archeryscore/app/domain/model/SessionSetupOptions.kt` (the three lists, `indexOf*`/`*AtIndex`, and `nearest(values, saved)`) until T002 is green (R-2, R-3).
- [ ] T005 Write RED instrumented test for the reusable control in `android/app/src/androidTest/kotlin/com/archeryscore/app/ui/start/OptionSliderTest.kt` — the slider snaps to discrete steps, exposes Material `SetProgress` semantics, renders the mapped value label, and contains no editable text node (R-1, R-7).
- [ ] T006 Implement `OptionSlider` in `android/app/src/main/kotlin/com/archeryscore/app/ui/start/OptionSlider.kt` — index-based Material 3 `Slider` (`valueRange = 0f..(values.size-1)`, `steps = values.size-2`), live label from `*_value` strings, and content description — until T005 is green (R-1, R-7).

**Checkpoint**: `SessionSetupOptions` + `OptionSlider` + strings are ready. All three P1 stories can now be implemented.

---

## Phase 3: User Story 1 - Set Distance with a Slider (Priority: P1) 🎯 MVP

**Goal**: Replace the free-text distance field with a step slider offering exactly `8, 12, 18, 30, 40, 50, 70, 90 m`, seeded from the saved default (snapped), and allow the `8 m` value to survive CSV round-trip.

**Independent Test**: Open session setup, drag the distance control, verify only the 8 preconfigured values are selectable and the label updates with no keyboard; seed a stored `25 m` default and confirm the slider opens at `30 m`.

### Tests for User Story 1 ⚠️ (write first, must FAIL)

- [ ] T007 [P] [US1] Write RED instrumented test in `android/app/src/androidTest/kotlin/com/archeryscore/app/ui/start/StartScreenDistanceSliderTest.kt` — distance slider offers exactly the 8 steps, snaps to each, shows `"<n> m"`, seeds from a saved `25` at `30` (FR-008), exposes no editable distance text field, and never requests an IME (FR-001, FR-004, FR-005, FR-006).
- [ ] T008 [P] [US1] Write RED unit tests in `android/app/src/test/kotlin/com/archeryscore/app/data/csv/CsvImporterTest.kt` — `distance_m = 8` accepted, `7` rejected, `300` accepted, `301` rejected, and an `8 m` `export → import → export` identity (FR-013, SC-008, storage Part B).

### Implementation for User Story 1

- [ ] T009 [US1] Replace the distance `OutlinedTextField` (`android/app/src/main/kotlin/com/archeryscore/app/ui/start/StartScreen.kt:101-107`) with `OptionSlider` wired to `SessionSetupOptions.DISTANCES_M`, seed its index from `state.defaults.defaultDistanceM` via `nearest(...)`, gate the set-up form on `!state.loading` (R-4/FR-008), and pass the selected value into `viewModel.createSession(distanceM = ...)` (FR-007, already covered by `ResumeSessionViewModelTest`). Depends on T006, T007.
- [ ] T010 [US1] Widen the distance validation in `android/app/src/main/kotlin/com/archeryscore/app/data/csv/CsvImporter.kt:128` from `10..300` to `8..300` until T008 is green (FR-013). Depends on T008.

**Checkpoint**: Distance slider is fully functional and independently testable (MVP).

---

## Phase 4: User Story 2 - Set Arrows Per End with a Slider (Priority: P1)

**Goal**: Replace the free-text arrows-per-end field with a step slider offering exactly `1, 3, 6`.

**Independent Test**: Open session setup, interact with the arrows-per-end control, verify only the 3 predefined values are selectable and the created session uses the selected value.

### Tests for User Story 2 ⚠️ (write first, must FAIL)

- [ ] T011 [P] [US2] Write RED instrumented test in `android/app/src/androidTest/kotlin/com/archeryscore/app/ui/start/StartScreenArrowsSliderTest.kt` — arrows slider offers exactly `1, 3, 6`, snaps to each, shows the value label, seeds from a stored `2` at `3` (tie → larger, FR-008), and never requests an IME (FR-002, FR-004, FR-005, FR-006).

### Implementation for User Story 2

- [ ] T012 [US2] Replace the arrows-per-end `OutlinedTextField` (`android/app/src/main/kotlin/com/archeryscore/app/ui/start/StartScreen.kt:118-125`) with `OptionSlider` wired to `SessionSetupOptions.ARROWS_PER_END`, seeded from `state.defaults.defaultArrowsPerEnd` via `nearest(...)`, and pass the value into `viewModel.createSession(arrowsPerEnd = ...)`. Depends on T006, T011 (and reuses the T009 loading gate).

**Checkpoint**: US1 and US2 both work independently.

---

## Phase 5: User Story 3 - Set Number of Ends with a Slider (Priority: P1)

**Goal**: Replace the free-text ends field with a step slider offering exactly `1, 3, 6, 9, 12`.

**Independent Test**: Open session setup, interact with the ends control, verify only the 5 predefined values are selectable and the created session uses the selected value.

### Tests for User Story 3 ⚠️ (write first, must FAIL)

- [ ] T013 [P] [US3] Write RED instrumented test in `android/app/src/androidTest/kotlin/com/archeryscore/app/ui/start/StartScreenEndsSliderTest.kt` — ends slider offers exactly `1, 3, 6, 9, 12`, snaps to each, shows the value label, seeds from a stored `4` at `3` (FR-008), and never requests an IME (FR-003, FR-004, FR-005, FR-006).

### Implementation for User Story 3

- [ ] T014 [US3] Replace the ends `OutlinedTextField` (`android/app/src/main/kotlin/com/archeryscore/app/ui/start/StartScreen.kt:109-116`) with `OptionSlider` wired to `SessionSetupOptions.END_COUNTS`, seeded from `state.defaults.defaultEndCount` via `nearest(...)`, and pass the value into `viewModel.createSession(endCount = ...)`. Depends on T006, T013 (and reuses the T009 loading gate).

**Checkpoint**: All three P1 sliders are independently functional.

---

## Phase 6: User Story 4 - Keyboard-Free Session Setup (Priority: P2)

**Goal**: The whole set-up (all three numeric parameters) is keyboard-free, and the last-used slider values auto-save as the new defaults so the next set-up opens pre-positioned.

**Independent Test**: Configure all three sliders and start a session, then reopen set-up and confirm the three sliders are pre-positioned at the last-used values with no manual re-entry and no keyboard at any point.

### Tests for User Story 4 ⚠️ (write first, must FAIL)

- [ ] T015 [P] [US4] Write RED unit test in `android/app/src/test/kotlin/com/archeryscore/app/ui/resume/ResumeSessionViewModelTest.kt` — `createSession` persists `defaultDistanceM`, `defaultEndCount`, `defaultArrowsPerEnd`, and `defaultTargetType` in one `updatePreferences`, and no preference write occurs without a session (FR-010, SC-007, contract C5 / storage Part A).
- [ ] T016 [P] [US4] Add the holistic keyboard-free acceptance test in `android/app/src/androidTest/kotlin/com/archeryscore/app/ui/start/StartScreenKeyboardFreeTest.kt` — composing the distance/arrows/ends `OptionSlider`s together, assert each exposes `SetProgress` slider semantics, has no editable text node, and updates its label without an IME (FR-006, SC-001). Expected green once T009/T012/T014 land.

### Implementation for User Story 4

- [ ] T017 [US4] Extend `ResumeSessionViewModel.createSession` (`android/app/src/main/kotlin/com/archeryscore/app/ui/resume/ResumeSessionViewModel.kt:86`) to copy `distanceM`, `endCount`, and `arrowsPerEnd` into `UserPreferences` alongside `defaultTargetType` before `updatePreferences`, until T015 is green (R-5, FR-010). Depends on T015.

**Checkpoint**: Keyboard-free setup and remembered defaults both validated.

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Quality gates and end-to-end validation (Constitution quality gates).

- [ ] T018 [P] Run `./gradlew lintDebug` from `android/` and resolve any errors introduced by the slider changes (Constitution Q1).
- [ ] T019 Run `./gradlew testDebugUnitTest connectedDebugAndroidTest` from `android/` and confirm the full suite is green, including the existing `ResumeSessionViewModelTest` and `CsvImporterTest` (FR-009, SC-005 no regression).
- [ ] T020 [P] Execute the manual acceptance walkthrough in `specs/004-slider-input-methods/quickstart.md`, including the legacy `25 m → 30 m` snap and the `8 m` CSV export/import/export round-trip (SC-008).
- [ ] T021 [P] Accessibility verification of all three sliders in `android/app/src/main/kotlin/com/archeryscore/app/ui/start/OptionSlider.kt` — TalkBack announces each control and its current value via the string resources from T003 (contract C6).
- [ ] T022 [P] Verify no hardcoded user-facing strings remain (all labels from `android/app/src/main/res/values/strings.xml`) and that the distance-range annotation is present in `specs/002-local-sqlite-storage/contracts/csv-roundtrip.md`.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — start immediately.
- **Foundational (Phase 2)**: Depends on T001 — **BLOCKS all user stories**.
- **User Stories (Phase 3–6)**: All depend on Phase 2. US1 first (MVP); US2/US3 can proceed once Phase 2 completes; US4 depends on the three sliders being wired (T009, T012, T014).
- **Polish (Phase 7)**: Depends on all targeted stories being complete.

### Task-level Dependencies

- T002 → T004 (test before implementation)
- T003, T004 → T005 → T006
- T006 → T007/T008 (stories need the control)
- T007 → T009; T008 → T010
- T006 → T011 → T012
- T006 → T013 → T014
- T009, T012, T014 → T016
- T015 → T017
- T009/T012/T014/T017 → T018–T022

### User Story Dependencies

- **US1 (P1)**: Starts after Phase 2 — no dependency on other stories. **MVP.**
- **US2 (P1)**: Starts after Phase 2 — reuses `OptionSlider`; edits the same file (`StartScreen.kt`) as US1, so its wiring task (T012) must follow T009 sequentially.
- **US3 (P1)**: Starts after Phase 2 — same file constraint; T014 follows T009/T012 sequentially.
- **US4 (P2)**: Depends on US1–US3 wiring (T009, T012, T014) plus its own VM change (T017).

### Within Each User Story

- Test tasks MUST be written and FAIL before the corresponding implementation task.
- The shared control (`OptionSlider`) and catalog (`SessionSetupOptions`) precede all wiring.
- Complete a story before moving to the next priority.

### Parallel Opportunities

- T002 and T003 (different files).
- T007 and T008 (distance UI test vs. CSV unit test — different files).
- T011 and T013, and T015, can run in parallel with other test tasks (distinct new files).
- All Polish `[P]` tasks (T018, T020, T021, T022).

---

## Parallel Example: User Story 1

```bash
# Launch both US1 test tasks together (independent files, both RED first):
Task: "Distance slider instrumented test in android/app/src/androidTest/kotlin/com/archeryscore/app/ui/start/StartScreenDistanceSliderTest.kt"
Task: "CSV 8 m bound unit tests in android/app/src/test/kotlin/com/archeryscore/app/data/csv/CsvImporterTest.kt"
```

```bash
# After Phase 2, launch the independent story test files in parallel:
Task: "Arrows slider test in .../StartScreenArrowsSliderTest.kt"
Task: "Ends slider test in .../StartScreenEndsSliderTest.kt"
Task: "Auto-save unit test in .../ResumeSessionViewModelTest.kt"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup.
2. Complete Phase 2: Foundational (**CRITICAL** — blocks all stories).
3. Complete Phase 3: US1 (distance slider + CSV bound).
4. **STOP and VALIDATE**: run `StartScreenDistanceSliderTest` and `CsvImporterTest`; walk through the distance portion of `quickstart.md`.
5. This is a shippable increment.

### Incremental Delivery

1. Setup + Foundational → control and catalog ready.
2. US1 → validate → demo (MVP).
3. US2 → validate → demo.
4. US3 → validate → demo.
5. US4 → validate auto-save + keyboard-free holistic outcome.
6. Polish → lint, full suite, accessibility, manual acceptance.

### Parallel Team Strategy

With multiple developers after Phase 2:
- Developer A: US1 (distance) — note `StartScreen.kt` edits are sequential with B/C.
- Developer B: US2 (arrows).
- Developer C: US3 (ends).
- The CSV bound (T008/T010) is file-independent and can be handled alongside.
- US4 after the three wirings land.

Because US1–US3 all edit `StartScreen.kt`, coordinate the three wiring tasks to run **sequentially** even if the test tasks run in parallel.

---

## Notes

- `[P]` = different files, no dependency on incomplete tasks.
- `[Story]` labels map tasks to spec user stories for traceability.
- Verify each RED test fails before implementing (TDD evidence in commit history — Constitution quality gate 6).
- Commit after each logical group with `type(scope): description` (e.g., `test(start): RED distance slider`, `feat(start): distance step slider`).
- Existing `ResumeSessionViewModelTest` (`30 m / 12 ends / 6 arrows`) and `CsvImporterTest` must stay green: their values are already valid steps and the bound widens only.
- Do not add third-party slider components (spec Assumption) and do not change the discipline/round/target dropdowns (FR-012).
- Stop at any checkpoint to validate a story independently.
