<!--
Sync Impact Report
==================
Version change: 0.0.0 → 1.0.0
Modified principles: N/A (initial constitution)
Added sections:
  - Core Principles (5 principles)
  - Technology Stack & Constraints
  - Development Workflow & Quality Gates
  - Governance
Removed sections: N/A
Templates requiring updates:
  - .specify/templates/plan-template.md ✅ aligned (mobile-app structure)
  - .specify/templates/spec-template.md ✅ aligned (user story format)
  - .specify/templates/tasks-template.md ✅ aligned (phase structure)
Follow-up TODOs: None
-->

# ArcheryScore Constitution

## Core Principles

### I. Test-Driven Development (NON-NEGOTIABLE)

All production code MUST follow strict TDD (Red-Green-Refactor cycle):

1. Write a failing test that defines expected behavior
2. Implement the minimal code to make the test pass
3. Refactor while keeping all tests green
4. Every feature, bug fix, and refactoring MUST have tests written BEFORE implementation

Test types required per feature:
- **Unit tests**: For ViewModels, repositories, utilities, domain logic
- **Integration tests**: For Supabase interactions, navigation flows
- **UI tests**: For critical user journeys (score entry, session management)

### II. Security & Dependency Hygiene

All dependencies MUST be:
- Updated to latest stable versions at project start and reviewed quarterly
- Scanned for known CVEs using `dependencyCheck` or equivalent
- Excluded from the build if any critical/high vulnerability exists with no patch
- Supabase credentials MUST be stored in local properties, never in version control

### III. Supabase-First Storage

All persistent data MUST be stored in Supabase (PostgreSQL). Local caching MAY be used for offline reads but the Supabase database is the single source of truth. Offline writes MUST queue and sync when connectivity resumes.

### IV. Native Android with Modern Stack

The app MUST be built using:
- **Language**: Kotlin (100%)
- **UI**: Jetpack Compose with Material 3
- **Architecture**: MVVM + Clean Architecture
- **DI**: Hilt
- **Navigation**: Compose Navigation
- **Gradle**: Kotlin DSL (build.gradle.kts)
- **Min SDK**: 26 (Android 8.0) | **Target SDK**: 34

### V. Offline Resilience

The app MUST handle network unavailability gracefully:
- Score entry MUST work offline
- Data MUST sync automatically when connectivity returns
- User MUST see clear indicators of sync status
- No data loss during connectivity interruptions

## Technology Stack & Constraints

| Layer | Technology | Version |
|-------|-----------|---------|
| Language | Kotlin | 2.0+ |
| UI Framework | Jetpack Compose | Latest BOM |
| Architecture | MVVM + Clean Architecture | - |
| DI | Hilt | Latest |
| Storage | Supabase Kotlin SDK | Latest |
| Build System | Gradle (Kotlin DSL) | 8.x |
| Testing | JUnit5 + Mockk + Turbine | Latest |
| Min SDK | 26 | - |
| Target SDK | 34 | - |

**Constraints**:
- APK size MUST NOT exceed 15MB
- Cold start MUST be under 2 seconds on mid-range devices
- All network calls MUST use HTTPS only
- No hardcoded strings in code; use string resources

## Development Workflow & Quality Gates

### Branch Strategy
- `main`: Production-ready code
- `develop`: Integration branch
- `feature/*`: Feature branches off develop
- `fix/*`: Bug fix branches off develop

### Quality Gates (mandatory before merge)
1. All unit tests pass
2. All integration tests pass
3. Lint check passes with zero errors
4. No dependency vulnerabilities (critical/high)
5. Code review approved
6. TDD cycle verified (test-first evidence in commit history)

### Commit Convention
- Format: `type(scope): description`
- Types: `feat`, `fix`, `test`, `refactor`, `chore`, `docs`
- Scope: feature area (e.g., `score`, `session`, `sync`, `ui`)

## Governance

This constitution supersedes all other development practices for the ArcheryScore project.

- All pull requests MUST verify compliance with these principles
- Amendments MUST be documented with rationale and version bump
- Versioning follows semantic versioning: MAJOR (principle removal/redefinition), MINOR (new principle/section), PATCH (clarifications)
- Compliance review is required before any release
- For runtime development guidance, refer to AGENTS.md

**Version**: 1.0.0 | **Ratified**: 2026-09-08 | **Last Amended**: 2026-09-08
