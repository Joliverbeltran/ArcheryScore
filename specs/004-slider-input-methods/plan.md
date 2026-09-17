# Implementation Plan: Step-Slider Session Input Methods

**Branch**: `004-slider-input-methods` | **Date**: 2026-09-17 | **Spec**: [`spec.md`](./spec.md)

**Input**: Feature specification from `/specs/004-slider-input-methods/spec.md`

## Summary

Replace the three free-text numeric fields on the session set-up screen (distance, ends, arrows per end) with discrete **step sliders** whose values are limited to the option sets archers actually use. Sliders remove the numeric keyboard surface, eliminate out-of-range/invalid entries, and make the common case one swipe. Slider option sets: distance `8, 12, 18, 30, 40, 50, 70, 90 m`; arrows per end `1, 3, 6`; ends `1, 3, 6, 9, 12`.

Technical approach: a pure, unit-tested `SessionSetupOptions` domain object owns the step lists and an **index ↔ value** mapping (so non-linear distance values are evenly spaced on the control and snap deterministically). A small reusable Material 3 `OptionSlider` composable renders each control with a live value label and no text input. Creating a session now also persists the last-used distance/ends/arrows as the new stored defaults (FR-010), read back and snapped to the nearest valid step on the next set-up (FR-008). The CSV importer's distance lower bound is relaxed from `10` to `8 m` so slider-created sessions round-trip (FR-013). No Room schema change and no new dependency.

## Technical Context

**Language/Version**: Kotlin 2.3.21, Gradle 9.x (Kotlin DSL), AGP 9.2.0

**Primary Dependencies**: Jetpack Compose (BOM 2026.08.00) + Material 3 — the `Slider` with `steps` already ships in Material 3, so **no new dependency is added**; Hilt (DI), Room 2.8.4, DataStore Preferences, Compose Navigation

**Storage**: On-device only. No Room schema change (still v3). The three existing DataStore preference keys (`default_distance_m`, `default_end_count`, `default_arrows_per_end`) now receive last-used values; CSV import validation bound changes to `8..300`

**Testing**: JUnit5 + Mockk + Turbine for unit tests (new `SessionSetupOptionsTest`, extended `ResumeSessionViewModelTest`); JUnit5 for CSV (`CsvImporterTest`); Compose UI test (`createComposeRule`) for slider behavior/labels in `androidTest`. **Strict TDD — tests are written first (Constitution I).**

**Target Platform**: Android — minSdk 26 (Android 8.0), targetSdk 36, compileSdk 37

**Project Type**: Mobile app — single Gradle module `:app` (`android/`)

**Performance Goals**: No regression to cold start < 2 s or 60 fps UI. Index-based slider arithmetic is O(1); nearest-step lookup is O(n) with n ≤ 8.

**Constraints**: Fully offline, no network surface; APK ≤ 15 MB (unchanged — no new libs/assets); no hardcoded strings (string resources); session set-up completable in < 15 s (SC-002)

**Scale/Scope**: Small, presentation + preferences + one validation bound. Touches 1 new domain file, 1 new composable, 3 existing production files, 1 resource file, plus tests. No migration, no navigation change.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| # | Principle / Gate | Status | How satisfied |
|---|------------------|--------|---------------|
| I | Test-Driven Development (NON-NEGOTIABLE) | ✅ PASS | Every task is test-first: `SessionSetupOptionsTest` (step sets, index mapping, nearest/tie snapping), `ResumeSessionViewModelTest` (auto-save of all four defaults), `CsvImporterTest` (8 m accepted; 7 m / 301 m rejected; 8 m round-trip), Compose `StartScreenSliderTest` (snap, labels, pre-seed, keyboard-free). No production code before a failing test. |
| II | Security & Dependency Hygiene | ✅ PASS | No new dependency (Material 3 `Slider` already present); no CVE delta. |
| III | On-Device-First Storage | ✅ PASS | Room unchanged; defaults stay in DataStore on-device; no remote surface. |
| IV | Native Android with Modern Stack | ✅ PASS | Kotlin + Compose Material 3 + MVVM + Hilt; minSdk 26 / targetSdk 36 / compileSdk 37. |
| V | Local-Only Operation | ✅ PASS | No network; CSV remains the sole exchange path (bound widened only). |
| C1 | APK ≤ 15 MB | ✅ PASS | No new dependencies or assets. |
| C2 | Cold start < 2 s | ✅ PASS | No startup-path work added; sliders run only on the set-up screen. |
| C3 | No hardcoded strings | ✅ PASS | Value labels added as string resources (e.g., `%1$d m`). |
| Q1 | Lint + unit + integration tests | ✅ PASS | Plan includes lint and the full local/instrumented suite. |

**Result: PASS — no violations; Complexity Tracking not required.**

## Project Structure

### Documentation (this feature)

```text
specs/004-slider-input-methods/
├── plan.md              # This file (/speckit.plan output)
├── research.md          # Phase 0 output (R-1..R-7)
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/
│   ├── session-setup.md # Step sets, snapping, auto-save, keyboard-free
│   └── storage.md       # DataStore default semantics + CSV distance bound
├── checklists/
│   └── requirements.md  # /speckit.specify + /speckit.clarify output
└── tasks.md             # Phase 2 output (/speckit.tasks — NOT created here)
```

### Source Code (repository root)

```text
android/app/src/main/kotlin/com/archeryscore/app/
├── domain/model/
│   ├── SessionSetupOptions.kt          # NEW: step lists + index<->value + nearest snapping
│   └── UserPreferences.kt              # unchanged fields (contract documents semantics)
├── ui/start/
│   ├── StartScreen.kt                  # EDIT: 3 text fields -> 3 OptionSliders; seed from defaults
│   └── OptionSlider.kt                 # NEW: reusable discrete slider (label + value + steps)
├── ui/resume/
│   └── ResumeSessionViewModel.kt       # EDIT: createSession auto-saves distance/ends/arrows
├── data/csv/
│   └── CsvImporter.kt                  # EDIT: distance bound 10..300 -> 8..300
└── res/values/
    └── strings.xml                     # EDIT: value label formats + content descriptions

android/app/src/test/kotlin/com/archeryscore/app/
├── domain/model/SessionSetupOptionsTest.kt        # NEW (TDD first)
├── ui/resume/ResumeSessionViewModelTest.kt        # EDIT: auto-save assertions
└── data/csv/CsvImporterTest.kt                    # EDIT: 8 m / boundaries / round-trip

android/app/src/androidTest/kotlin/com/archeryscore/app/ui/start/
└── StartScreenSliderTest.kt                       # NEW: snap, labels, pre-seed, keyboard-free
```

**Structure Decision**: Single-module Android app (`:app`) under `android/`, following the existing MVVM + Clean Architecture layering. The discrete-option logic goes in `domain/model` (pure, no Android imports) so it is unit-testable; the Compose control lives in `ui/start` next to the screen that owns it. No new Gradle modules.

## Complexity Tracking

> No Constitution Check violations — this section is intentionally empty.
