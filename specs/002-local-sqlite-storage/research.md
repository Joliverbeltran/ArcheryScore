# Research — Local-Only Storage (Remove Supabase)

**Phase 0 output** | **Spec**: `spec.md` (FR-001..FR-015) | **Branch**: `002-local-sqlite-storage` | **Date**: 2026-09-11

This document records the key technical decisions and open-item resolutions for removing Supabase and making the app on-device-only. All decisions are reversible implementation details; the requirements are fixed by the spec.

## R-1 — Identity: remove user namespacing entirely

- **Decision**: Delete `AuthRepository` (interface, `SupabaseAuthRepository`, `DefaultAuthRepository`), `AuthResult`, `Session.userId`, `SessionEntity.userId`, the DataStore `local_user_id` key/`ensureLocalUserId()`/`observeLocalUserId()`, and every `userId`/`user_id` parameter and query filter.
- **Why**: Spec FR-006 (no sign-in/try) + Key Entities ("no remote identity entity; data is not namespaced"). Single implicit owner = the device.
- **Impact**: `observeSessions`, `observeActiveSession`, `getActiveSession`, `observePreferences`, `updatePreferences`, `observeStats`, `SyncStatusRepository` drop their `userId` parameter. ViewModels (`HistoryViewModel`, `StatsViewModel`, `ResumeSessionViewModel`, `ActiveSessionViewModel`) drop the `authRepository` dependency. New sessions (FR-001) are created without `userId`.

## R-2 — Room schema v2 and migration strategy (FR-007, SC-005)

- Current DB is `archery_score.db`, Room version 1, tables: `sessions`, `ends`, `arrows`, `prefs`, `sync_writes`.
- **Findings**:
  - The Room `prefs` table (`PreferencesEntity`/`PreferencesDao`/`Mapper.prefsToEntity`) is **dead code** — preferences flow from DataStore (`DataStorePreferencesRepository`); nothing consumes the Room table at runtime.
  - `sync_writes` is only written by the outbox/sync layer, which is being removed.
  - `sessions.user_id` and `sessions.last_synced_at` exist only for auth/sync.
- **Decision**: Bump to **version 2** with a certified migration `MIGRATION_1_2`, preserving existing `sessions`/`ends`/`arrows` rows (FR-007, SC-005):
  - Recreate `sessions` **without** `user_id` and `last_synced_at`; `INSERT … SELECT` existing rows; rename; recreate `index_sessions_date` (same Room-generated name). Drop `user_id` index with the old table.
  - `DROP TABLE sync_writes;` and `DROP TABLE prefs;`.
  - `ends` / `arrows` are structurally unchanged (already no user column) — no DDL needed; their FKs remain valid (rows copied before rename).
- minSdk 26 notes: the bundled SQLite predates `ALTER TABLE … DROP COLUMN`, so table recreate/rename is used (not in-place column drops).

## R-3 — Supabase + sync teardown (FR-004, FR-005, FR-006, SC-006)

Remove, in one change-set:
- Gradle: `libs.versions.toml` supabase/ktor/workmanager entries; `app/build.gradle.kts` dependency blocks and `buildConfigField SUPABASE_URL/SUPABASE_ANON_KEY`.
- Code: `data/sync/**` (`SyncRunner`, `SyncWorker`, `SyncWorkerFactory`, `SyncScheduler`, `SyncOutboxWriter`, `OutboxSyncStatusRepository`, `SessionRemoteDataSource`, `SupabaseRemoteDataSource`, `DisabledRemoteDataSource`, `Remote.kt`), `data/network/NetworkMonitor.kt`, `data/auth/**`, sync DAO/entity, `SyncStatus` model, `SyncStatusRepository`, `SessionListItem.syncStatus`.
- Wiring: `AppModule` supabase/auth/sync providers; `ArcheryScoreApp` `Configuration.Provider` + `SyncWorkerFactory` → revert to plain `Application`; Manifest WorkManager provider removal + drop `INTERNET`/`ACCESS_NETWORK_STATE` permissions; delete `supabase/` repo dir.
- `RoomSessionRepository` stops writing to the outbox and passes the Session through as the single authoritative write (local `insert/replace` semantics unchanged).
- Whole app retains **zero network code**, so SC-006 holds by construction.

## R-4 — History/Stats become purely local (FR-003)

- `DefaultStatsRepository` consumes `SessionRepository` already; only its `observeStats(userId, …)` signature changes.
- `HistoryViewModel`/`StatsViewModel` remove `authRepository`, use non-namespaced `observeSessions()`/`observeStats()`.
- `HistoryScreen` removes the sync `AssistChip` (`sync_synced`/`sync_pending` strings deleted; FR-005).

## R-5 — CSV import design (FR-014, FR-015, SC-009)

- **Round-trip by construction**: the importer consumes the exact format `CsvExporter` emits (contract `csv-roundtrip.md`), so export→import restores all rows (SC-009).
- **Reconstruction**: CSV rows carry session metadata denormalized per arrow. Group by `session_id`; derive `endCount = max(end_number)`, `arrowsPerEnd = max(arrow_number)` per session, `status = COMPLETE` (imported data is a finished round), `createdAt = date`, `updatedAt = now`. Session IDs reuse the CSV `session_id` (idempotent restore).
- **Validation** (FR-015): exact header, RFC 4180 parse, UUID/ISO-8601 dates, enum names, `distance_m ∈ 10..300`, `score` ≤ `max(roundType)` (TEN_ZONE→10, FIVE_ZONE→5), `is_x_ring` only when `score == 10`, `end_number`/`arrow_number ≥ 1`, file size cap (5 MB / 100k rows). **Atomic**: any row-level error aborts the whole import — zero rows written and a structured error (row + column + reason) is surfaced; existing data untouched.
- **Duplicates**: a `session_id` already present in the DB is skipped (no overwrite); the result dialog reports "N imported, M skipped (already present)". Skipping is idempotent and satisfies "leave existing data unchanged."
- **Entry point**: History screen top-bar "Import CSV" → SAF `ACTION_OPEN_DOCUMENT` (`text/csv`) → parse in a coroutine → result snackbar/dialog. Uses framework APIs only (no new dependency).

## R-6 — DB missing/corrupt handling (FR-012)

- Missing DB → Room creates an empty one → existing empty-states already render (User Story 2, #3).
- Corrupt DB → wrap `AppDatabase.build` so `CorruptionException` deletes the corrupt file and rebuilds empty in memory behavior (empty state, no crash). Normal upgrades never hit this path (migration preserves data per R-2).

## R-7 — Constitution amendment (FR-013, SC-008)

Timeline: amend `constitution.md` **as part of this feature, before merge**:
- Meridian change — Principle III `Supabase-First Storage` → `On-Device-First Storage` (Room/DataStore is the sole authoritative store).
- Principle V `Offline Resilience` → `Local-Only Operation` (no network required, no sync concepts).
- Principle II/Technology Stack: drop Supabase rows/credentials clauses; Storage row → Room; remove HTTPS-only clause (no network) or note as N/A.
- Header `Sync Impact Report` + `Version` bump (MAJOR per governance: principle removal, 1.0.1 → 2.0.0). Template files re-aligned.
- Delivered with the feature, satisfying "governance docs updated before merge."

## Open items resolved

| Item | Decision |
|------|----------|
| Backup/recovery mechanism | CSV round-trip (clarification 2026-09-11) |
| CSV import merge vs replace | Duplicate session_ids skipped (atomic import, no overwrite) |
| `prefs` room table vs DataStore | DataStore wins; Room `prefs` table deleted |
| DB version strategy | Version 2 with certified `MIGRATION_1_2` (no destructive fallback) |
| Imported session status | `COMPLETE` (never resurrected into an active session) |