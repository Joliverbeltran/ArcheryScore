# Contract: CSV Round-Trip (Export + Import)

**Spec ref**: FR-009 (export), FR-014 (import), FR-015 (validation) | Constitution: on-device-only (amended)

> **Amendment (feature 003-target-arrow-input)**: the export header is extended to 10 columns by inserting `target_type` after `round_type` (values: `CM122|CM80|CM60|CM40|TRIPLE_VERTICAL|TRIPLE_TRIANGULAR`). Import remains backward-compatible and still accepts this legacy 9-column header (rows default to `CM122`). All other rules and the round-trip identity are unchanged. Authoritative addition: `specs/003-target-arrow-input/contracts/storage.md`.

> **Amendment (feature 004-slider-input-methods)**: the accepted `distance_m` range is widened to **`8..300`** (previously `10..300`) so that `8 m` sessions created via the step slider round-trip. Only the lower bound changes; the upper bound, all other validation rules, the header, and the round-trip identity are unchanged. Authoritative addition: `specs/004-slider-input-methods/contracts/storage.md` (Part B).

The export format is **unchanged** from feature 001 (`specs/001-archery-score/contracts/csv-export.md`) so existing exports remain valid. The import contract below is the reverse operation: it consumes exactly what `CsvExporter.kt` emits.

## Export format (authoritative for import)

- Encoding: UTF-8, line endings CRLF, no BOM.
- Header row + one row per arrow, chronological (session date asc, `end_number` asc, `arrow_number` asc).
- All fields escaped per RFC 4180 (quoted when containing `,`, `"`, `\n`, `\r`; embedded quotes doubled).

| Column | Source / format |
|--------|-----------------|
| `session_id` | `Session.id` (UUID) |
| `date` | `Session.date` — ISO-8601 UTC `yyyy-MM-dd'T'HH:mm:ss'Z'` |
| `distance_m` | `Session.distance_m` (`10..300`) |
| `discipline` | `Session.discipline` (enum name) |
| `round_type` | `Session.round_type` (`TEN_ZONE`\|`FIVE_ZONE`) |
| `end_number` | `End.end_number` (`>=1`) |
| `arrow_number` | `Arrow.arrow_number` (`>=1`) |
| `score` | `Arrow.score` (`1..max(roundType)`) |
| `is_x_ring` | `Arrow.is_x_ring` (`true`/`false`) |

```csv
session_id,date,distance_m,discipline,round_type,end_number,arrow_number,score,is_x_ring
d291c19e-08a9-4f2c-b8c4-7f3c8a1b2d3e,2026-09-08T09:15:00Z,18,OLYMPIC_RECURVE,TEN_ZONE,1,1,10,true
d291c19e-08a9-4f2c-b8c4-7f3c8a1b2d3e,2026-09-08T09:15:00Z,18,OLYMPIC_RECURVE,TEN_ZONE,1,2,9,false
```

## Import contract

- **Input**: one CSV file chosen via SAF `ACTION_OPEN_DOCUMENT` (`text/csv`, any provider).
- **Acceptance for a row**: header row **exactly** matches the export header (case-sensitive, column order); every data row passes all of below. On the first violation the import **aborts atomically** — nothing is written and an error naming `(row, column, reason)` is shown (FR-015). Existing data is never modified by a failed import.
  - exactly 9 fields after RFC 4180 parsing;
  - `session_id`: well-formed UUID;
  - `date`: parses as ISO-8601 UTC `yyyy-MM-dd'T'HH:mm:ss'Z'`;
  - `distance_m`: integer in `10..300`;
  - `discipline`: one of `OLYMPIC_RECURVE|TRADITIONAL_RECURVE|BAREBOW|LONGBOW|COMPOUND`;
  - `round_type`: `TEN_ZONE|FIVE_ZONE`;
  - `end_number`, `arrow_number`: integers `>=1`;
  - `score`: integer in `1..max(roundType)` — `TEN_ZONE`→`1..10`, `FIVE_ZONE`→`1..5`;
  - `is_x_ring`: `true`|`false`; `true` allowed only when `score == 10`; `false` (not empty) otherwise.
- **File guards**: max 5 MB / 100 000 rows; at least one data row required.
- **Reconstruction** (once the file is fully valid):
  - group rows by `session_id`;
  - `endCount = max(end_number)`, `arrowsPerEnd = max(arrow_number)` per session;
  - `status = COMPLETE` (an imported round-trip is a finished session; never resurrected as ACTIVE);
  - `createdAt = date`, `updatedAt = now`; `notes = null` (not in the format);
  - preserve `session_id`, arrow IDs are freshly generated (export does not carry arrow/end IDs).
- **Idempotency / duplicates**: a `session_id` already present in the DB is **skipped** (no overwrite, no duplicate rows); the completion dialog reports `Imported N session(s), skipped M already present`, `100%` success otherwise (SC-009).
- **Errors** surface via snackbar/dialog from string resources; never a crash.

## Acceptance link

- Unit: `CsvImporterTest` — round-trip property (export → import → export identical), each rejection rule, atomicity (bad row → zero writes), all-invalid UUID/date/enum cases.
- Instrumented: migration test preserves data; end-to-end SAF import of an exported file.
- Behavior tests: import of a previously exported file creates sessions visible in History (User Story 4, #4); duplicate import skips without changing existing rows (FR-015).