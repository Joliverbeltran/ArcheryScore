# Contract: Target Placement Input & Score Mapping

**Spec ref**: FR-001..FR-012, FR-015 | **Branch**: `003-target-arrow-input` | Supersedes the numeric `ScoreDialog` input in `ui/record`.

## Scope

Defines the interaction contract for recording an arrow's score by placing a marker on a rendered target face, and the pure mapping from a placement position to a stored score. Implementation details (Compose specifics) live in the plan; this contract is the behavioral agreement tests must hold.

## Placement coordinate space

- Normalized 2D space; **unit = one face/sub-face radius** (`R`).
- Single faces (122/80/60/40cm): the face center is `(0,0)`; the scoring face is the circle `x²+y² ≤ 1`.
- Triple faces: three spot centers per D-3 layout (vertical: `(0,−2.2),(0,0),(0,+2.2)`; triangular: `(0,−1.1),(−1.1,+0.95),(+1.1,+0.95)`). Each spot is a circle of radius `1` about its center.
- Screen→space conversion: linear scale/offset from the composable's bounding box (aspect-correct, centered), no distortion.

## Placement state machine (per arrow slot)

```
IDLE ──TAP/DRAG──▶ PENDING(marker at P) ──OK──▶ PERSISTED(score,sX) ──▶ NEXT_ARROW | CLOSE
                        │
                        └──CANCEL/back──▶ DISCARD (nothing persisted)
```

- **TAP** at screen point → set **pending marker** position `P` (FR-003); provisional score derived live (FR-007, badge shown) but **not persisted**.
- **DRAG** → update pending `P` continuously (FR-004); still unpersisted.
- **OK** → resolve `PlacementScorer(P)`, persist `(score, isX)` through the existing `EditScoreUseCase` (FR-005); then **auto-advance**:
  - if the end has remaining arrows → fresh pending state for `arrow_number + 1` (FR-006);
  - else → close the dialog (end complete).
- **CANCEL/back** → discard pending marker; no write. Re-opens on the same arrow if tapped again.
- Off-face taps: marker is **clamped to the face drawable area** so it stays visible; resolved as the corresponding normalized point (outside a scoring circle → miss, FR-008). Drag can bring it back inside before OK.

## Score mapping (authoritative — FR-007/008/009, WA rules)

Given normalized `r = √(x²+y²)` measured from the resolved face center, zone width `w = 1/maxScore`:

| Case | Rule |
|------|------|
| `r ≥ 1` | **Miss** — `score = 0`, `isX = false` (FR-008) |
| `0 < r < 1` | `score = maxScore − floor(r/w)`, clamped to `≥ 1` — boundary touches score the **higher** inner value (FR-009) |
| `r ≤ 0.5·w` and 10-zone | `score = 10`, `isX = true` (inner-10 "X"; X requires `TEN_ZONE`, consistent with `ScoreValidator`) |
| 5-zone | `isX` always `false` |

Triple faces: the resolved face is the **nearest spot center** to the tap point (FR-010); scoring rules above are then applied relative to that center.

## Persisted output

Confirmed arrows remain the existing `(score: Int, isXRing: Boolean)`; **no placement coordinates are stored** (spec Assumption). All existing downstream behavior (totals, history, stats, CSV) is unchanged.

## Error/edge handling (FR-015)

- Correction within the current end: tapping a chip re-opens placement for that arrow; OK re-persists and continues through the remaining arrows of the end (heritage editing-on-completed-sessions + confirmation stays per feature 001).
- Dialog interruption (rotation/back): pending marker is transient; nothing is persisted until OK (FR-005). Confirmed arrows remain intact.

## Acceptance link

- `PlacementScorerTest`: every ring value for 10- and 5-zone, every boundary line (higher value), `r ≥ 1` miss, X/`r ≤ 0.5w`, 5-zone never-X.
- `TripleLayoutTest`: spot-center constants; nearest-spot for taps in-face, on boundaries, and in gaps.
- `TargetPlacementFlowTest` (Compose UI): tap → marker; drag → marker moves; OK → persisted + advances; cancel → discarded; off-face tap shows miss; last arrow closes.
- Behavior tests: visual placement produces correct session totals; existing numeric sessions still edit fine (FR-014).