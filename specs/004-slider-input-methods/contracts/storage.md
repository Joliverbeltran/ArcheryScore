# Contract — Preferences & CSV Import Bound

**Feature**: `004-slider-input-methods` | **Spec**: [`../spec.md`](../spec.md) | **Decisions**: R-5, R-6 in [`../research.md`](../research.md)

This feature changes no Room schema. It changes the semantics of three existing DataStore keys and one CSV import validation bound.

## Part A — DataStore preference defaults

Keys are unchanged; see [`../data-model.md`](../data-model.md) for defaults.

| Guarantee | Requirement |
|-----------|-------------|
| Read-once semantics | `observePreferences()` continues to yield a `UserPreferences` whose numeric fields may be raw legacy values (outside the slider sets). Consumers MUST snap for display (contract C4). |
| Write on create | `ResumeSessionViewModel.createSession(...)` MUST persist, in a single atomic `updatePreferences`, the values used for the new session for all of: `defaultRoundType` (existing), `defaultTargetType` (existing), `defaultDistanceM`, `defaultEndCount`, `defaultArrowsPerEnd`. |
| Idempotence | Repeating a write with identical values leaves the stored `UserPreferences` unchanged. |
| No write on drag | Moving a slider without creating a session MUST NOT modify preferences. |
| Durability | After process restart, the next session set-up pre-populates from the last created session's values. |
| Failure isolation | A preferences write failure MUST NOT corrupt the created `Session`; the session is created and the error surfaces through the existing VM error path. |

### Verified behavior

```text
Given defaults (round=TEN_ZONE, distance=18, ends=6, arrows=3, target=CM122)
When  createSession(round=FIVE_ZONE, distance=30, ends=12, arrows=6, target=CM80)
Then  stored defaults become distance=30, ends=12, arrows=6, target=CM80
And   a second set-up pre-selects FIVE_ZONE / 30 m / 12 ends / 6 arrows / CM80
```

## Part B — CSV import distance bound (FR-013)

**File**: `android/app/src/main/kotlin/com/archeryscore/app/data/csv/CsvImporter.kt`
**Change**: the `distance_m` validation changes from `distanceM !in 10..300` to `distanceM !in 8..300`.

| Input `distance_m` | Before | After |
|--------------------|--------|-------|
| `7` | rejected | **rejected** |
| `8` | rejected | **accepted** |
| `10` | accepted | accepted |
| `300` | accepted | accepted |
| `301` | rejected | **rejected** |
| non-numeric / missing | rejected | rejected |

Other row-validation rules are unchanged. The 10-column v3 header and the legacy 9-column header handling are unchanged.

### Round-trip guarantee

For any session the UI can create (distance ∈ `{8,12,18,30,40,50,70,90}`), `export → import → export` MUST produce byte-identical CSV, including the `distance_m` value `8`.

### Contract amendment note (feature 002)

`specs/002-local-sqlite-storage/contracts/csv-roundtrip.md` is annotated to record that the accepted `distance_m` range is now `8..300` (previously `10..300`). All other terms of that contract remain in force.

## Verification mapping

| Guarantee | Test |
|-----------|------|
| Part A write/no-write/idempotence | `ResumeSessionViewModelTest` (extend) |
| Part B boundary table | `CsvImporterTest` (extend) |
| Part B round-trip at 8 m | `CsvImporterTest` / round-trip test (extend) |
| Instrumented end-to-end default flow | `StartScreenSliderTest` (new) |
