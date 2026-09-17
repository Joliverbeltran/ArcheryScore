# Research — Visual Target Arrow Placement

**Phase 0 output** | **Spec**: `spec.md` | **Branch**: `003-target-arrow-input` | **Date**: 2026-09-15

Resolves every technical unknown in the plan's Technical Context. Format: **Decision / Rationale / Alternatives considered**.

## R-1: How to render the target faces (image vs programmatic)

- **Decision**: Draw the targets **programmatically with Compose `Canvas`** from World Archery (WA) geometry. No image assets shipped.
- **Rationale**: WA ring geometry is exact and scale-invariant (see R-2), so a face is a set of concentric colored circles — trivial with Canvas. Six static assets (plus dark/light variants) would add APK weight, contradict constitution constraint APK ≤ 15MB, and freeze the look; drawing keeps it crisp at any size. No new dependency.
- **Alternatives considered**: bundled vector/SVG assets (asset processing + APK cost, rejected); a third-party target-image library (supply-chain surface, rejected); Material icons (not applicable).

## R-2: Official ring geometry and score mapping (confirmed from WA rules)

- **Decision**: Use the WA 10-zone geometry — **10 scoring zones of equal width**, 5 colour bands (gold, red, blue, black, white) each split by a thin line into 2 rings; the inner-10 ("X") is a circle of radius **half the 10-ring radius**; ring "value v" has radius `w × (11 − v)` where `w = faceRadius / 10`. Verified published ring diameters for the 122/100/80 faces give a uniform `w` per face (e.g. 122cm → w = 6.1cm; 80cm → 4cm; 60cm → 3cm; 40cm → 2cm), confirming equal-width zones scaled by face radius.
- **Rationale**: The published rulebooks (World Archery Book 2 §7.2.2, indoor Book 3) define zones as equal width "measured from the centre of the gold" — so score resolution is **purely a function of normalized radius** `r = distance/faceRadius`. That makes `PlacementScorer` a small, exhaustively testable pure function with zero graphics coupling.
- **Alternatives considered**: ring diameters from a lookup table per face (rejected — equal-width formula is authoritative, simpler, and size-independent); approximating by linear zones without X-ring inner-ten (rejected — breaks X scoring already supported by the app).

> **R-2 addendum (FIVE_ZONE)**: The physical face is **always** the standard 10-zone WA graphic (5 colour bands, 10 rings); the scoring type only changes value attribution. For `FIVE_ZONE` (maxScore 5, `w = R/5`) one colour band (two adjacent rings) maps to a single value 1–5 (outer→inner) via the same formula `score = maxScore − floor(r/w)` — no code divergence. X-ring never applies (mirrors `ScoreValidator.canBeXRing`).

## R-3: Boundary and line conventions

- **Decision**: Ring dividing lines lie **within the higher-scoring zone** (WA rule). In normalized terms: score = `maxScore − floor(r / w)`, clamped to `≥1` — a point exactly on a ring boundary therefore falls into the higher (inner) value automatically (matches FR-009). Miss (score 0) when `r ≥ 1`. X when `r ≤ w/2` and scoring type allows it (10-zone only, consistent with existing `ScoreValidator`).
- **Rationale**: The formula falls straight out of the "line belongs to higher zone" convention and needs no epsilon hacks; unit-testable across every boundary.
- **Alternatives considered**: geometric "area membership" per ring with explicit tie handling (rejected — more code and an arbitrary boundary decision; WA convention resolves it).

## R-4: Triple face layout (vertical & triangular)

- **Decision**: Draw three identical sub-faces, spaced **center-to-center 2.2R** (R = sub-face radius) — matching WA's published 22cm spacing on 40cm triple spots (radius 20cm ⇒ 2.2 × R). Arrangement: **vertical** = 3 spots on a vertical line; **triangular** = one top spot, two bottom spots (~equilateral). Spot assignment = the spot whose center is **nearest** the tap point (FR-010). Sub-face default physical size 40cm; the physical label is cosmetic — scoring is radius-normalized per spot.
- **Rationale**: WA rulebooks document triple-spot spacing and that centres for triangular faces are the familiar two-low/one-high; nearest-centre assignment is the simplest rule that matches "tapping near one spot places the arrow there" and handles taps in the gaps (FR-010).
- **Alternatives considered**: explicit spot picker before placement (rejected — adds a step; tap-to-place already disambiguates); tiling logic to detect which spot's bounding circle contains the point with a miss in the gaps (rejected — misses between spots are then impossible to record, contradicting nearest-spot FR-010).

## R-5: Marker placement user model

- **Decision**: Tap sets an **unconfirmed** marker at the tapped normalized position; drag moves it continuously; OK persists **only** score+isX through the existing `EditScoreUseCase` (FR-005); Cancel/back discards the unconfirmed position. After OK the dialog **advances to the next arrow in the same end** (FR-006); after the end's last arrow it closes. Marker stays visible for already-confirmed arrows of that end (FR-012). Off-face taps still place the marker (clamped to the visible face area) and resolve as a miss (0) — FR-008.
- **Rationale**: Matches the requested interaction exactly ("tapping sets position… move until exact… OK validates… repeat for all arrows in same end") with the least state: only "pending marker position" is transient UI state; confirmed arrows remain numeric (per spec). Clamping keeps the marker on-screen so a miss is deliberately assertable.
- **Alternatives considered**: double-tap gesture for OK (rejected — conflicts with drag, no accidental-confirm protection); live-persist on drag end (rejected — violates review-then-OK intent); separate end-flow wizard screen (rejected — larger navigation surface; a dialog keeps the end context visible).

## R-6: Persistence of the selected target

- **Decision**: Add `sessions.target_type TEXT NOT NULL DEFAULT 'CM122'` (Room v2→v3 via `ALTER TABLE`); a per-session field (FR-013). Add `default_target_type` to DataStore `UserPreferences` so the set-up menu pre-selects the last-used face. CSV gains a `target_type` column (10-col header) with **backward-compatible** import of legacy 9-col files (default `CM122`), preserving the 002 round-trip identity (`export→import→export` identical) and migration-free upgrade of old files.
- **Rationale**: Target selection is a per-session attribute (spec), so it belongs on `sessions`; the DataStore mirror matches the existing five session-setup defaults. Because 002 contracted CSV as the sole data-exchange with a strict round-trip guarantee, omitting `target_type` would silently lose it on export/import — so the format is extended and legacy files still import.
- **Alternatives considered**: targets as a lookup/config table (rejected — static enum, no joins needed); store placement coordinates on arrows (explicitly out of scope in spec Assumptions); no CSV change (rejected — breaks round-trip and SC-009).

## R-7: Performance & correctness bounds

- **Decision**: `PlacementScorer` is a pure, allocation-light function (scalar math only) invoked once per tap/OK; Canvas draw is O(10) circles; spot lookup is O(3) for triple faces. Target resolution tested to complete well inside the 16ms frame budget; no per-frame allocations in the draw path.
- **Rationale**: Satisfies SC-001/SC-003 (end recorded in <45s of user time; 100% correct scoring on a verified placement set) with margin; keeps cold-start/APK goals untouched (R-1).
- **Alternatives considered**: precomputed ring bitmaps + hit-test lookup tables (rejected — more memory, gains nothing at 10 rings).

## Consolidated decisions that unblock Phase 1

| # | Decision |
|---|----------|
| D-1 | Target faces drawn with Compose Canvas from WA equal-width-zone geometry (no assets). |
| D-2 | `PlacementScorer`: score = `maxScore − floor(11·r)`-style radial formula; line-including-higher-zone; `r ≥ 1 → miss`; `r ≤ 0.5w → X` (10-zone only). |
| D-3 | Triple faces: 3 sub-faces, 2.2R spacing; nearest-centre spot assignment; vertical = column, triangular = two-low/one-high. |
| D-4 | Placement UX = unconfirmed marker (tap/drag) → OK persists score via existing use-case → auto-advance in-end; cancel discards. |
| D-5 | `sessions.target_type` column (Room v3) + `default_target_type` DataStore pref + 10-col CSV with legacy 9-col import. |
| D-6 | No new dependencies; TDD-first; placement math lives in `domain/model` for pure unit tests. |