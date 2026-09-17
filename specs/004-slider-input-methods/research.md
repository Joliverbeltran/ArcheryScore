# Phase 0 Research — Step-Slider Session Input Methods

**Spec**: [`spec.md`](./spec.md) | **Branch**: `004-slider-input-methods` | **Date**: 2026-09-17

Each decision resolves an open implementation question from the spec. No `NEEDS CLARIFICATION` items remain.

---

## R-1 — Discrete slider widget and step mapping

**Question**: How do we render a slider that only allows the required option sets, when the distance set (`8, 12, 18, 30, 40, 50, 70, 90`) is **not** evenly spaced numerically?

**Decision**: Use the Material 3 `Slider(value, onValueChange, valueRange, steps)` (already in the Compose BOM — no new dependency) and drive it by **option index**, not by the numeric value. Each slider operates on `0f..(n-1)f` with `steps = n - 2`; the thumb is therefore evenly spaced regardless of the underlying values, and the displayed label maps the snapped index back to its value via `SessionSetupOptions`.

**Alternatives considered**:
- *Value-range slider* (`valueRange = 8f..90f`, `steps = 6`) — rejected: thumb positions become proportional to the numeric value, clustering the low distances together (8/12/18 within ~12 % of the track) and making precise taps unreliable; snapping to the nearest *value* is also awkward for non-uniform gaps.
- *Single-choice chips / segmented buttons* — rejected: with 8 distance options the row overflows on small screens, and it loses the continuous drag affordance the spec calls for (FR-005).
- *Custom drag surface* — rejected: unnecessary; reimplementing Material 3 semantics/a11y.
- *Numeric stepper (+/−)* — rejected: still acceptably discrete but slower for large jumps and contradicts the "slider" requirement.

**Consequence**: Material 3 rounds the floating-point progress to the nearest step index, so the mapped index is always an exact integer. The mapping/index logic lives in the pure domain object (R-2), not in the composable.

---

## R-2 — Where step lists and mapping live

**Question**: Should the option sets and value↔index mapping be private to `StartScreen`?

**Decision**: Add a pure, Android-free domain object `com.archeryscore.app.domain.model.SessionSetupOptions` owning:
- `DISTANCES_M = listOf(8, 12, 18, 30, 40, 50, 70, 90)`
- `ARROWS_PER_END = listOf(1, 3, 6)`
- `END_COUNTS = listOf(1, 3, 6, 9, 12)`
- `indexOfDistance(Int)`, `distanceAtIndex(Int)`, and the same for ends/arrows
- `nearest(values, saved)` → nearest step (see R-3)

**Rationale**: This is business/domain data (the allowed tournament values). Keeping it out of the UI makes it exhaustively unit-testable (Constitution I) and reusable by the ViewModel auto-save and by any future import validation. It has no Android dependency, so it runs in fast JVM tests.

**Alternatives considered**: *constants inside `StartScreen.kt`* — rejected: untestable without Compose and would duplicate the sets if the importer or VM needed them.

---

## R-3 — Snapping rule for a saved value that is not a step

**Question**: FR-008 pre-populates sliders from stored defaults, but legacy/edited preferences may hold values outside the sets (e.g. `25 m`, `4 ends`, `2 arrows`). How is the nearest step chosen, including ties?

**Decision**: `nearest(values, saved)` returns the list element minimizing `abs(value - saved)`; on an exact tie it returns the **larger** step; values beyond either end clamp to the min/max step (no null case, list is non-empty).

Worked examples used as tests:

| Saved | Set | Nearest | Reason |
|-------|-----|---------|--------|
| 25 | distances | 30 | 7 (to 18) vs 5 (to 30) |
| 8 | distances | 8 | exact |
| 100 | distances | 90 | clamp above max |
| 4 | ends `1,3,6,9,12` | 3 | 1 (to 3) vs 2 (to 6) |
| 5 | ends | 6 | tie (2 vs 2) → larger |
| 2 | arrows `1,3,6` | 3 | tie (1 vs 1) → larger |
| 4 | arrows | 3 | 1 (to 3) vs 2 (to 6) |
| 12 | ends | 12 | max step allowed |

**Rationale**: "Round up on tie" is deterministic, easy to document, and matches archer intuition (prefer the longer/more demanding option when equidistant). Clamping guarantees the slider always has a valid starting index.

**Alternatives considered**: *reject the value and fall back to the 18 m / 6 ends / 3 arrows defaults* — rejected: loses a legitimate user preference for no benefit; *snap only at read time by migrating stored prefs* — rejected: preference storage stays raw and snapping at presentation keeps the rule in one place and is trivially testable.

---

## R-4 — Seeding sliders from asynchronously loaded defaults

**Question**: The current `StartScreen` seeds `remember { mutableIntStateOf(state.defaults...) }`, but `ResumeSessionViewModel` starts with an empty `UserPreferences()` and loads real prefs asynchronously. `remember` without keys captures the empty default on first composition, so saved defaults silently fail to appear. FR-008 requires the opposite.

**Decision**: Compose the set-up form only after defaults are loaded (gate on `state.loading == false`) and seed the three slider indices with `remember(state.defaults)` from `SessionSetupOptions.nearest(...)` / index lookups. Because the preference flow emits a single value and the form appears for the first time only after loading, the slider state initializes from the real defaults exactly once and then holds the user's in-progress edits.

**Rationale**: Fixes a latent correctness bug in the same code path the feature depends on, without introducing a new ViewModel or moving form state (keeps the existing local-state convention). The "gate on loading" pattern is already implied by the VM exposing `loading`.

**Alternatives considered**: *Move all three selections into `ResumeSessionViewModel`* — rejected as out-of-scope refactor for this feature; *`LaunchedEffect` to overwrite state after load* — rejected: risks clobbering a user edit and is harder to test.

**Verification**: Compose test seeds `FakePreferencesRepository` with non-default values (e.g. `50 m`, `4 ends`, `6 arrows`) and asserts the sliders open at `50`, `3` (snapped), `6`.

---

## R-5 — Automatic persistence of last-used values (FR-010)

**Question**: Where is the "save the chosen values as the new defaults" behavior implemented?

**Decision**: Extend `ResumeSessionViewModel.createSession(...)` — which already persists `defaultTargetType` — to write `defaultDistanceM`, `defaultEndCount`, and `defaultArrowsPerEnd` in the same `UserPreferences.copy(...)`. Values arriving from the sliders are always valid steps, so no write-time snapping is needed.

**Rationale**: Reuses the existing single write path and the existing acceptance test pattern; keeps the VM the owner of preference updates. Existing tests pass unchanged because their sample values (`30 m`, `12 ends`, `6 arrows`) are valid steps.

**Alternatives considered**: *A separate `SaveDefaultsUseCase`* — rejected: premature for a two-line copy; *saving on every slider change* — rejected: spec says defaults are remembered per created session, not per drag (would churn DataStore).

---

## R-6 — CSV importer distance lower bound (FR-013)

**Question**: The importer currently rejects `distanceM !in 10..300`, but `8 m` is a valid slider value, so a session created with `8 m` could be exported but not re-imported.

**Decision**: Change `CsvImporter.kt` to reject `distanceM !in 8..300` (upper bound unchanged). Update the 002 CSV contract annotation and add boundary tests: `8` accepted, `7` rejected, `300` accepted, `301` rejected, plus an `8 m` export→import round-trip.

**Rationale**: Makes the exchange format round-trip-safe for every value the UI can produce, while keeping a sane upper bound and not breaking legacy ≥ 10 m files.

**Alternatives considered**: *Wide the bound to `1..300`*(rejected: `1..7 m` are not valid sessions and would mask corrupt data; spec bounds it at 8); *remove the lower bound entirely* — rejected: loses a data-integrity check.

---

## R-7 — Keyboard-free guarantee and string resources

**Question**: FR-006 requires no keyboard for these three fields, and the constitution forbids hardcoded strings. What exactly changes and how is it tested?

**Decision**: Remove the three `OutlinedTextField`s entirely (only the session-notes field remains a text field). Each slider's value is shown via a string resource (`distance_value` = `%1$d m`, `end_count_value` = `%1$d`, `arrows_per_end_value` = `%1$d`) and an accessibility `stateDescription`/content description. The Compose test asserts each slider node has the `SetProgress` semantics (proving it is a slider, not a text input) and shows the expected label.

**Rationale**: The strongest, simplest evidence of "no keyboard" is the absence of any editable text node for these inputs plus the presence of slider semantics. Value labels satisfy FR-004.

**Alternatives considered**: *Keeping a read-only text field for the label* — rejected: adds a text-field semantics node and confuses a11y/testing.

---

## Summary of decisions

| ID | Decision |
|----|----------|
| R-1 | Index-based Material 3 `Slider` (`valueRange 0..n-1`, `steps = n-2`) for even spacing on non-uniform value sets |
| R-2 | Pure `domain/model/SessionSetupOptions` owns option sets, index↔value, and `nearest()` |
| R-3 | Nearest step; ties round up (larger); clamp outside the range |
| R-4 | Gate set-up form on `loading == false`; seed slider indices via keyed `remember` from snapped defaults |
| R-5 | `ResumeSessionViewModel.createSession` persists distance/ends/arrows alongside target type |
| R-6 | CSV import accepts `8..300` (was `10..300`); boundary + round-trip tests |
| R-7 | Delete the three text fields; label values from string resources; verify via slider semantics |
