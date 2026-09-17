# Data Model — Step-Slider Session Input Methods

**Phase 1 output** | **Spec**: [`spec.md`](./spec.md) | **Branch**: `004-slider-input-methods` | **Date**: 2026-09-17

Decisions R-1..R-7 in [`research.md`](./research.md) apply.

## Overview

This feature is an **input-method change**. It introduces **no new Room table, column, migration, or entity**: persisted `Session`/`End`/`Arrow` data is unchanged (Room stays at **v3**). The only persisted behavioral change is *which values* are written to the three existing preference keys (now the last-used slider selections) and a widened CSV import bound. The new domain type is a pure, in-memory option catalog.

## Entities & Relationships

No ERD change. `UserPreferences` values remain plain `Int`/enum fields; the option catalog below constrains what the UI can produce.

## New domain type — `SessionSetupOptions`

Pure object in `domain/model` (no Android imports). Owns the allowed sets and the index↔value↔snap rules from R-1..R-3.

| Member | Type | Value / behavior |
|--------|------|------------------|
| `DISTANCES_M` | `List<Int>` | `[8, 12, 18, 30, 40, 50, 70, 90]` (ordered, ascending) |
| `ARROWS_PER_END` | `List<Int>` | `[1, 3, 6]` |
| `END_COUNTS` | `List<Int>` | `[1, 3, 6, 9, 12]` |
| `indexOfDistance(value)` | `Int` | index of `value` in `DISTANCES_M`; seeds the slider |
| `distanceAtIndex(index)` | `Int` | value at clamped/snapped index; drives the label |
| `indexOfEnds` / `endCountAtIndex` | `Int` | same for `END_COUNTS` |
| `indexOfArrows` / `arrowsAtIndex` | `Int` | same for `ARROWS_PER_END` |
| `nearest(values, saved)` | `Int` | nearest element; tie → larger; clamp outside range |

**Invariants** (unit-tested):
1. Every list is non-empty, strictly ascending, and duplicate-free.
2. `nearest(values, values[i]) == values[i]` for all `i`.
3. `distanceAtIndex(indexOfDistance(v)) == v` (and same for ends/arrows).
4. Snap results always belong to the corresponding list, for any `Int` input including `Int.MIN_VALUE`/`MAX_VALUE`.
5. Tie-breaking always selects the larger candidate.

### Slider option catalog

| Control | Allowed values | Steps on track | Index range | Default source key |
|---------|----------------|----------------|-------------|--------------------|
| Distance (m) | `8, 12, 18, 30, 40, 50, 70, 90` | 8 (7 gaps) | `0..7` | `default_distance_m` |
| Arrows per end | `1, 3, 6` | 3 (2 gaps) | `0..2` | `default_arrows_per_end` |
| Ends | `1, 3, 6, 9, 12` | 5 (4 gaps) | `0..4` | `default_end_count` |

Material 3 `Slider` uses `valueRange = 0f..(size-1)f` and `steps = size - 2` (R-1).

## Preferences (DataStore) — semantics change only

No key or type changes. Fields in `domain/model/UserPreferences.kt`:

| Field | Key | Default | New behavior |
|-------|-----|---------|--------------|
| `defaultDistanceM` | `default_distance_m` | `18` | overwritten with the last-used distance on session create (FR-010) |
| `defaultEndCount` | `default_end_count` | `6` | overwritten with the last-used ends on session create |
| `defaultArrowsPerEnd` | `default_arrows_per_end` | `3` | overwritten with the last-used arrows on session create |
| `defaultTargetType` | `default_target_type` | `CM122` | unchanged (already persisted by 003) |

**Read path**: values are stored raw and may fall outside the option sets (legacy installs). The UI snaps them with `SessionSetupOptions.nearest(...)` when seeding slider indices (R-3, R-4). **Write path**: values always originate from sliders, hence are always valid steps.

## CSV round-trip change (FR-013)

| Aspect | Before | After |
|--------|--------|-------|
| Distance validation | `distanceM in 10..300` | `distanceM in 8..300` |
| Header / columns | unchanged (`session_id,date,distance_m,...`) | unchanged |
| Legacy files (≥ 10 m) | import | still import (bound is wider only) |
| Slider-created `8 m` sessions | could not round-trip | export→import→export byte-identical |

Contract detail in [`contracts/storage.md`](./contracts/storage.md); the 002 CSV contract is annotated with the new bound.

## State & lifecycle

| Concern | Behavior |
|---------|----------|
| Slider state | Local to `StartScreen`; initialized from snapped defaults once defaults finish loading (R-4) |
| Session creation | `ResumeSessionViewModel.createSession` writes `Session` (unchanged shape) and updates all four defaults (R-5) |
| Resume/active session | Unchanged; no slider state restored — setup screen is only shown when no session is active |
| Existing sessions (pre-004) | Untouched — no migration; their stored `distanceM`/`endCount`/`arrowsPerEnd` are already valid session data (the import bound only widens) |
| Offline / no sync | Unchanged — Room + DataStore only |
