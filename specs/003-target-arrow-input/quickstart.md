# Quickstart — Visual Target Arrow Placement

**Branch**: `003-target-arrow-input` | **Spec**: `spec.md` | **Plan**: `plan.md`

Prereqs and daily-combat commands for engineers working on this feature. Full TODOs land in `tasks.md` (Phase 2, `/speckit.tasks`).

## Environment

- Android SDK (compileSdk 37), JDK 21, AGP 9.2.0, Gradle 9.x (Kotlin DSL).
- No new dependencies; no network config; fully offline build.
- Domain context: WA target geometry (`research.md` R-2/R-3), placement contract (`contracts/target-input.md`), schema v3 (`contracts/storage.md`, `data-model.md`).

## Build & test commands

```bash
cd android
./gradlew :app:testDebugUnitTest    # unit tests: PlacementScorer, TripleLayout, CSV round-trip, prefs
./gradlew :app:lintDebug            # lint, zero errors (incl. Compose a11y)
./gradlew :app:assembleDebug        # build
./gradlew :app:connectedAndroidTest # instrumented: Room migration 2→3, placement UI, target-face fit, menu, marker visibility
# Note: Compose UI tests (TargetPlacementFlowTest, TargetFaceFitTest, StartScreenTargetTypeTest, TargetMarkersVisibleTest)
# are instrumented under androidTest using ui-test-junit4 + createComposeRule; the migration test uses MigrationTestHelper.
```

## Verification checklist (maps to success criteria)

- [ ] New session set-up shows the six target options; selection is remembered as the default next time (FR-002, FR-016, SC-004, R-6)
- [ ] Placement: tap → marker appears; drag → marker follows to exact spot; OK → score persists and advances to next arrow; all arrows of an end sequence (FR-003..FR-006, SC-001)
- [ ] Full face reachable and accurately tappable on small and rotated screens (aspect-correct, inset-based fit)
- [ ] Score correctness: verified placement set — inside ring, on ring boundary (higher value), off-face (miss 0), X on 10 — matches WA rules 100% (FR-007..FR-009, SC-003)
- [ ] Triple faces: tap near each spot scores against that spot; gaps resolve to nearest spot; previously placed arrows stay visible (FR-010, FR-012, SC-005)
- [ ] Cancel/back discards an unconfirmed marker; nothing persists until OK (FR-005)
- [ ] Correction: re-tap an arrow chip in the current end → re-place → OK (FR-015)
- [ ] Upgrade path: install previous build with data → open new build → sessions intact, `target_type = CM122` (FR-014, SC-006)
- [ ] CSV: export → import → export identical, now including `target_type`; legacy 9-col file still imports (R-6, 002 SC-009)
- [ ] TDD evidence in commits (tests first), unit+instrumented suite green, lint zero errors

## Governance gate (before merge)

- [ ] No constitution amendment required (all gates PASS — see `plan.md` Constitution Check); no new dependencies added
- [ ] Doc updates: `specs/002-local-sqlite-storage/contracts/csv-roundtrip.md` amended for the 10-column header; AGENTS.md plan pointer updated
- [ ] TDD evidence in commits (tests first), full unit+instrumented suite green, lint zero errors

## Known touch points (see `plan.md` → Project Structure for the tree)

Additions: `domain/model/TargetType.kt`, `domain/model/PlacementScorer.kt`, `Session.targetType`, `UserPreferences.defaultTargetType`, `SessionEntity.target_type` + `MIGRATION_2_3`, DataStore key, `ui/components/TargetFace.kt`, `ui/record/TargetPlacementDialog.kt`, placement state in `ActiveSessionViewModel`, `TargetTypeDropdown` in `StartScreen`, CSV `target_type` column, target string resources.
Removals: the numeric chip-based `ScoreDialog` in `ui/record/ActiveSessionScreen.kt` (score entry only).
Untouched: `ends`/`arrows` schema, repository interfaces, stats/history/export reads, dependency catalog, manifest permissions.