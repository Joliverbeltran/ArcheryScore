# Contract — Session Set-Up Slider Input

**Feature**: `004-slider-input-methods` | **Spec**: [`../spec.md`](../spec.md) | **Decisions**: R-1..R-4, R-7 in [`../research.md`](../research.md)

Defines the observable contract of the three session set-up controls and of the pure `SessionSetupOptions` domain object.

## C1. Allowed option sets (source of truth: `SessionSetupOptions`)

```text
DISTANCES_M   = [8, 12, 18, 30, 40, 50, 70, 90]   # metres
ARROWS_PER_END = [1, 3, 6]
END_COUNTS    = [1, 3, 6, 9, 12]
```

Any attempt to select a value outside these sets MUST be impossible through the UI (FR-003, FR-005).

## C2. Slider behavior

| Rule | Requirement |
|------|-------------|
| Widget | A slider control (Material 3 `Slider`), not a text field (FR-002). |
| Range/steps | `valueRange = 0f..(size-1)f`, `steps = size-2`; thumb positions are evenly spaced for every option regardless of the underlying value (R-1). |
| Snapping | Release/variation always resolves to exactly one option index; the mapped value MUST be an element of the option set (FR-005). |
| Value ↔ index | `distanceAtIndex(indexOfDistance(v)) == v`, and likewise for ends and arrows (R-2). |
| Live label | The current option value MUST be visible and update as the slider moves (FR-004): distance as `"<n> m"`, ends/arrows as `"<n>"`. |
| Keyboard | Selecting distance/ends/arrows MUST NOT open any soft keyboard or IME (FR-006); only the optional session-notes field remains a text input. |
| Persisted value | The value passed to session creation is exactly the slider's mapped option (never a raw interpolated float). |

## C3. Snapping function `nearest(values, saved)`

Precondition: `values` is non-empty, strictly ascending, duplicate-free.

Return the element `v` minimizing `abs(v - saved)` subject to:
1. If two elements are equidistant (exact tie), return the **larger** one.
2. If `saved` is below the minimum, return the minimum; if above the maximum, return the maximum.
3. The result is always an element of `values` (no `null`, no exception), for every `Int` input including `Int.MIN_VALUE` and `Int.MAX_VALUE`.

Reference vectors (MUST pass):

| `values` | `saved` | Result |
|----------|---------|--------|
| `DISTANCES_M` | 8 | 8 |
| `DISTANCES_M` | 25 | 30 |
| `DISTANCES_M` | 100 | 90 |
| `DISTANCES_M` | `Int.MIN_VALUE` | 8 |
| `END_COUNTS` | 4 | 3 |
| `END_COUNTS` | 5 | 6 (tie → larger) |
| `END_COUNTS` | 12 | 12 |
| `ARROWS_PER_END` | 2 | 3 (tie → larger) |
| `ARROWS_PER_END` | 4 | 3 |

## C4. Pre-population from stored defaults (FR-008)

1. On opening session set-up, each slider starts at the option obtained by snapping its stored default: `distanceAtIndex(indexOfDistance(nearest(DISTANCES_M, prefs.defaultDistanceM)))` (and analogous for ends/arrows).
2. Slider state is initialized only after preferences have loaded (R-4); before that, the set-up form is not shown.
3. A default that is already a valid step is used verbatim.
4. User changes remain in effect for the current set-up until the user navigates away or starts the session.

## C5. Remember last-used values (FR-010)

1. On successful session creation, the chosen `distanceM`, `endCount`, and `arrowsPerEnd` replace `defaultDistanceM`, `defaultEndCount`, and `defaultArrowsPerEnd` in persistent preferences, together with the existing `defaultTargetType` update.
2. Subsequent session set-ups pre-populate from the updated defaults (C4).
3. No preference write occurs for merely moving a slider; persistence happens on session creation.

## C6. Accessibility & strings

1. Each slider exposes slider semantics (a `SetProgress` action) and an accessibility description whose text names the control and states the current value.
2. All labels/value strings come from string resources (e.g. `distance_value` = `%1$d m`); no hardcoded user-facing text (Constitution C3).

## Verification mapping

| Contract | Tests |
|----------|-------|
| C1, C2, C3 | `SessionSetupOptionsTest` (unit) |
| C4, C5 | `ResumeSessionViewModelTest` (unit) + `StartScreenSliderTest` (instrumented) |
| C2/C6 (widget, label, no keyboard) | `StartScreenSliderTest` (instrumented) |
