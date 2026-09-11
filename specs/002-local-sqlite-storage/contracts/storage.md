# Contract: Local-Only Storage (Room Schema v2 & No-Network Guarantee)

**Spec ref**: FR-001..FR-012 | Constitution: on-device-first (amended, FR-013/SC-008)

Supersedes (`specs/001-archery-score/contracts/`):
- `storage.md` (Supabase schema/RLS — **removed** with the cloud)
- `data-sync.md` (outbox/WorkManager sync — **removed**)
- `auth.md` (email/password auth — **removed**)

## Storage

- Sole store: AndroidX Room DB `archery_score.db`, version **2**. Tables: `sessions`, `ends`, `arrows`. (`prefs`, `sync_writes` dropped.) Exact DDL in `../data-model.md`.
- Preferences: DataStore single-file store, no `local_user_id` key.
- **No account, no identity, no namespacing** (FR-006). Untyped single device user.
- Migration `1→2` is certified and preserves existing `sessions`/`ends`/`arrows` (FR-007, SC-005). No destructive fallback for the upgrade path.

## Invariants as shipped

1. Every write is local and synchronous through Room's transaction window; there is no read/write path to any remote system (FR-004).
2. Single-ACTIVE-session per device, enforced by repository query on `status = 'ACTIVE'` (FR-010).
3. `score ∈ 1..max(roundType)`; X-ring only on 10 + `TEN_ZONE` — validated at entry and on import (FR-002, FR-015).
4. DB corrupt/missing recovery: fresh empty DB on missing; on corruption, delete + rebuild empty behavior with empty-state UI, never a crash (FR-012).
5. Sessions persist across restart/force-close; ACTIVE sessions auto-restore (FR-011).

## No-network guarantee (SC-006)

- App declares **no** `INTERNET` / `ACCESS_NETWORK_STATE` permissions.
- Compile-time: no `okhttp`/`ktor`/supabase sockets present; `NetworkMonitor` removed.
- Runtime observable: `adb shell dumpsys package <pkg> | grep -i permission` shows no network permissions; no WorkManager jobs scheduled.
- Release: `INTERNET` absent from the merged manifest, verified by `./gradlew :app:processReleaseManifest` output.

## Migration strategy

- v2 schema is applied by `MIGRATION_1_2` shipped in-app; Room validates against exported schema JSON in instrumented tests.
- Future version changes follow Room's standard migration pattern (NO destructive fallback on upgrade).

## Delivery notes

- New strings (import action, results, errors) in `res/values/strings.xml` — no hardcoded strings.
- Statistics/history/export read exclusively from Room (FR-003, FR-009); statistics ≤1s / history ≤2s for ≤500 sessions (SC-003).