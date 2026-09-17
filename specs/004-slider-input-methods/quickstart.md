# Quickstart — Step-Slider Session Input Methods

**Feature**: `004-slider-input-methods` | **Spec**: [`spec.md`](./spec.md) | **Plan**: [`plan.md`](./plan.md)

This guide validates the feature once implemented. All commands run from the repository root unless noted; the Android module is `android/`.

## Prerequisites

- JDK 17+, Android SDK with API 37 platform, an emulator or device (API 26+)
- No network access required (the app is local-only)

## 1. Fast JVM tests (write these first — TDD)

```bash
cd android
./gradlew testDebugUnitTest
```

Expected new/updated tests and what they prove:

| Test | Proves |
|------|--------|
| `SessionSetupOptionsTest` | Option sets are exact and ascending; index↔value round-trips; `nearest()` reference vectors incl. ties/clamps (contract C1–C3) |
| `ResumeSessionViewModelTest` | `createSession` persists all four defaults; no write on drag; values flow back into defaults (contract C5 / storage Part A) |
| `CsvImporterTest` | `8` accepted, `7`/`301` rejected, `300` accepted; `8 m` round-trip (storage Part B) |

Run a single class while iterating:

```bash
./gradlew testDebugUnitTest --tests "*SessionSetupOptionsTest"
```

## 2. Instrumented UI tests (emulator/device)

```bash
./gradlew connectedDebugAndroidTest
```

`StartScreenSliderTest` asserts:

- distance/ends/arrows expose **slider** semantics (`SetProgress`) and no editable text node;
- moving each slider snaps to an option and updates the visible value label;
- with preferences `50 m / 4 ends / 6 arrows`, sliders open at `50 / 3 (snapped) / 6`;
- no soft keyboard is requested for these controls.

## 3. Lint & dependency gate

```bash
./gradlew lintDebug
./gradlew dependencyCheckAnalyze   # if the task is configured in this repo
```

Lint must report zero errors; no new dependency was added, so there must be no new CVE findings.

## 4. Manual acceptance walkthrough

1. Launch the app with no active session.
2. Open session set-up. Confirm the three controls are **sliders**, not text fields, and show `18 m`, `6`, `3` on a fresh install.
3. Drag distance through its full range: it stops on `8, 12, 18, 30, 40, 50, 70, 90` only, and the label updates live.
4. Drag ends through `1, 3, 6, 9, 12` and arrows through `1, 3, 6`. No keyboard ever appears for these.
5. Choose distance `8 m`, ends `1`, arrows `1`; start the session. It creates normally.
6. Return to session set-up (after ending the session): the sliders pre-select `8 m / 1 / 1` (FR-010 + FR-008).
7. Export sessions to CSV, delete, then import the file: the `8 m` session imports successfully and re-exports identically (FR-013).
8. (Legacy) Seed a stored default of `25 m` / `4 ends` / `2 arrows` (e.g. via an older build), reopen set-up, and confirm it snaps to `30 m / 3 / 3` (R-3).

## 5. Constitution / quality gates before merge

- [ ] Tests written before implementation (TDD evidence in commit history)
- [ ] `testDebugUnitTest` green
- [ ] `connectedDebugAndroidTest` green
- [ ] `lintDebug` zero errors
- [ ] No critical/high dependency vulnerabilities
- [ ] APK ≤ 15 MB (no new deps/assets) and cold start unaffected
- [ ] No hardcoded user-facing strings (value labels use string resources)

## Troubleshooting

| Symptom | Likely cause |
|---------|--------------|
| Sliders open at `18/6/3` despite saved prefs | R-4 not applied: form rendered before `loading == false`, so `remember` captured empty defaults |
| Thumb positions cluster low for distance | R-1 not followed: value-range slider used instead of an index-based one |
| Saved `25 m` shows `18 m` | `nearest()` tie/rounding or index lookup bug — check contract C3 vectors |
| CSV import rejects an `8 m` session | R-6 not applied — importer still uses `10..300` |
