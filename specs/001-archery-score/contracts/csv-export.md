# Contract: CSV Export

**Spec ref**: FR-018 — "export sessions to CSV and share via Android share sheet"

## Format

- Encoding: UTF-8, line endings CRLF, no BOM.
- Header row + one row per arrow, chronological (session date asc, end_number asc, arrow_number asc).
- All fields escaped per RFC 4180 (quote if contains `,`, `"`, `\n`, `\r`; double any embedded quotes).

| Column | Source |
|--------|--------|
| `session_id` | `Session.id` |
| `date` | `Session.date` (ISO-8601 UTC, `yyyy-MM-dd'T'HH:mm:ss'Z'`) |
| `distance_m` | `Session.distance_m` |
| `discipline` | `Session.discipline` (enum name) |
| `round_type` | `Session.round_type` |
| `end_number` | `End.end_number` |
| `arrow_number` | `Arrow.arrow_number` |
| `score` | `Arrow.score` |
| `is_x_ring` | `Arrow.is_x_ring` (`true`/`false`) |

## Example

```csv
session_id,date,distance_m,discipline,round_type,end_number,arrow_number,score,is_x_ring
d291c19e-...,2026-09-08T09:15:00Z,18,OLYMPIC_RECURVE,TEN_ZONE,1,1,10,true
d291c19e-...,2026-09-08T09:15:00Z,18,OLYMPIC_RECURVE,TEN_ZONE,1,2,9,false
```

## Selection semantics

- Default: all sessions for the signed-in user.
- (v1) Export is session-scoped from the session detail screen; a global export-all is a nice-to-have. Pick session-scoped to keep scope tight per spec assumptions.

## Delivery

- Generated synchronously to a temp `File` in `cacheDir`, then `FileProvider` `content://` URI with `Intent.ACTION_SEND`, `text/csv`, chooser title from string resources (constitution constraint: no hardcoded strings).
- Errors (file IO, quota) surface as a Snackbar; no partial export file is shared.

## Acceptance link

- Unit tests: CSV escaping (quotes/commas/newlines), row ordering, header exact match.
- Instrumented test: build file → assert share Intent carries expected `content://` URI + type `text/csv`.