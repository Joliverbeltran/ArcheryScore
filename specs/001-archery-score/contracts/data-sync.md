# Contract: Data Sync (Offline-first, Last-Write-Wins)

**Spec ref**: FR-004, FR-005, FR-010, FR-016, SC-003, SC-007

## Architecture

```
 Room (cache + outbox) ──▶ SyncWorker (WorkManager) ──▶ Supabase (source of truth)
       ▲                                                          │
       └────────────────── Realtime (optional, v1: pull-only) ◀────┘
```

- **Writes**: every mutation writes to Room first (UI-visible immediately), appends a row to `SYNC_WRITE`, then enqueues a WorkManager one-time `SyncWorker` (best-effort immediate + constraint `NetworkType.CONNECTED`).
- **Reads**: reads come from Room (screens are never blocked by network). Supabase is the source of truth; changes propagate to Room during the same sync cycle (pull after push).
- **Trigger**: `SyncWorker` runs on app start, on connectivity resume (WorkManager network constraint), and after any local write. Periodic retries with exponential backoff; `SYNC_WRITE.attempts` counter caps at 10 (then surfaces `ERROR` status, FR-010).

## Operation types (SYNC_WRITE)

| operation | payload | Applied to Supabase as |
|-----------|---------|------------------------|
| `UPSERT` | serialized entity (session/end/arrow/preferences) | `upsert` (idempotent by PK) |
| `DELETE` | `{ id, type }` | `delete` + cascade on session |

Batched per sync run: session-level operations batch child ends/arrows with them to keep the `ARROWS_PER_END × END_COUNT` fan-out small.

## Conflict resolution — Last-Write-Wins per arrow (FR-016)

- Key: `Arrow.edited_at` (timestamptz, client clock — set from `Clock.INSTANT` at edit time).
- On merge, for each arrow id: the newer `edited_at` replaces the older, both in Room and Supabase (`is_x_ring`, `score` travel together).
- Session/end metadata conflicts resolve via `updated_at` (session) / max child `edited_at` (end).
- Field-level merge ONLY for `Arrow` score pairs; no other entity merges (session/end metadata is last-write-wins whole-row on `updated_at`).

## States (FR-010)

| State | Definition | UI indicator |
|-------|-----------|--------------|
| `SYNCED` | no pending outbox rows for the session, last push succeeded | check icon |
| `PENDING` | outbox rows exist for the session, worker queued/running | cloud/clock icon |
| `ERROR` | a write failed after max attempts (10) | alert icon + retry action |

Detection per session: outbox rows referencing its entity ids. Derived in `SyncStatusRepository` (unit-tested).

## Ordering guarantees

1. Run is transactional per entity type: push (UPSERT/DELETE) → confirm by deleting outbox row → pull (latest server rows into Room) → derive status.
2. No pull-then-push race: pushes always precede pulls within one run.
3. Deletes of a session that still has queued child writes are coalesced: any child `SYNC_WRITE` rows for that session are removed, and one session `DELETE` row is kept.

## Failure handling / edge cases (spec, Esc)

- Network failure mid-run (Esc 2): unaffected outbox rows remain; run retries via backoff; no data loss (SC-003).
- Force-close mid-session (Esc 3): Room persists synchronously with every arrow save; on relaunch `ACTIVE` session restored (FR-015).
- Storage quota exceeded (Esc 4): `ERROR` status on the session + user notification; local data intact.

## Acceptance link

- Integration tests (TDD): offline write → reconnect → assert Supabase rows match; simulate two devices editing same arrow with different `edited_at` → assert later wins; delete-during-pending test.