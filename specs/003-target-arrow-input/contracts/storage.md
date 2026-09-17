# Contract: Storage — Room Schema v3 & Target-Type Persistence

**Spec ref**: FR-013 (per-session target), FR-014 (existing data preserved), FR-002/FR-001 (target options + display) | Constitution: on-device-first (III), local-only (V)

Supersedes/extends (`specs/002-local-sqlite-storage/contracts/storage.md`), which remains authoritative except where amended below.

## Storage

- Sole store: AndroidX Room DB `archery_score.db`, version **3**. Tables: `sessions`, `ends`, `arrows`.
- **New** column: `sessions.target_type TEXT NOT NULL DEFAULT 'CM122'` (Room `@ColumnInfo "target_type"`), added by certified `MIGRATION_2_3` (`ALTER TABLE sessions ADD COLUMN target_type TEXT NOT NULL DEFAULT 'CM122'`) — additive, non-destructive, no table recreate (FR-014). minSdk 26 SQLite supports constant-default `ADD COLUMN`.
- `ends` / `arrows` **unchanged** — placement coordinates are never stored; arrows remain numeric `(score, is_x_ring)`.
- Preferences (DataStore): new key `default_target_type` (enum name string; missing/invalid → `CM122`). Existing keys unchanged.

## Target type values

`CM122` | `CM80` | `CM60` | `CM40` | `TRIPLE_VERTICAL` | `TRIPLE_TRIANGULAR`. Stored as the enum `name`; mapped in `Mapper` (unknown stored value → safe default `CM122`, matching the existing enum-mapping pattern used for `discipline`/`round_type`).

## Invariants as shipped

1. Every write is local through Room; no network surface (unchanged from v2 contract).
2. Single-ACTIVE-session per device (unchanged).
3. `score ∈ 1..max(roundType)`; X-ring only on 10 + `TEN_ZONE` — validated by `ScoreValidator` on every placement (FR-007) and on import.
4. Target selection is scoped to a session (FR-013): `sessions.target_type` set at creation from the set-up menu and used for every placement in that session.
5. Existing v2 data survives upgrade intact; backfilled `target_type = 'CM122'` (FR-014, SC-006).
6. DB corrupt/missing recovery unchanged (delete + rebuild empty, never a crash).

## Data-exchange amendment (CSV round-trip)

Because CSV is the sole exchange mechanism with a strict round-trip guarantee (002 SC-009), the export format is extended to carry the new attribute:

- Header becomes **10 columns**: `session_id,date,distance_m,discipline,round_type,target_type,end_number,arrow_number,score,is_x_ring`.
- `target_type` = one of the six enum names.
- Import accepts the new **and** the legacy 9-column header (legacy rows default to `CM122`); every existing validation rule and the atomic-abort-on-first-error behavior are unchanged.
- Round-trip identity now includes `target_type`. Amended contract: `specs/002-local-sqlite-storage/contracts/csv-roundtrip.md`.

## Migration strategy

- v3 applied by `MIGRATION_2_3` shipped in-app; Room validates against exported schema JSON (instrumented `RoomMigration23Test`).
- Future versions follow Room's standard migration pattern; **never** a destructive fallback on upgrade.

## Delivery notes

- New strings (six target labels, "set target", score badges `X`/`M`/ring, placement dialog title, OK/cancel) in `res/values/strings.xml` — no hardcoded strings.
- UI shows the selected face for all placement (FR-001); cycle-safe (`TargetFace` has no external I/O).