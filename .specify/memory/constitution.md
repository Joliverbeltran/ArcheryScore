<!--
On-Device Impact Report
=======================
Version change: 1.0.1 → 2.0.0 (MAJOR: Supabase/sync principles removed and redefined;
on-device-only architecture)
Modified principles:
  - Principle III: Supabase-First Storage → On-Device-First Storage
  - Principle V: Offline Resilience → Local-Only Operation
Modified sections:
  - Principle I: integration tests now target Room migrations and CSV round-trip
    (no Supabase interactions)
  - Principle II: removed Supabase credentials row
  - Technology Stack: Storage now SQLite (Room 2.8.4); removed Supabase Kotlin SDK
  - Constraints: removed HTTPS-only network constraint (no network surface remains)
  - Integration tests: only local/instrumented; no sync/supabase tests
Rationale: Feature 002 (local-sqlite-storage) removes all cloud/sync surface. The app
is local-only: SQLite is the single source of truth, CSV export/import is the only
data-exchange mechanism. Governed by FR-013, SC-008.
Supersedes: plan.md CONSTITUTION_AMENDMENT_FOLLOWUP and prior Sync Impact Report.
Templates requiring updates:
  - .specify/templates/plan-template.md ✅ aligned
  - .specify/templates/spec-template.md ✅ aligned
  - .specify/templates/tasks-template.md ✅ aligned
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
- **Integration tests**: For Room migrations, DAOs, and CSV export/import round-trips
- **UI tests**: For critical user journeys (score entry, session management)

### II. Security & Dependency Hygiene

All dependencies MUST be:
- Updated to latest stable versions at project start and reviewed quarterly
- Scanned for known CVEs using `dependencyCheck` or equivalent
- Excluded from the build if any critical/high vulnerability exists with no patch

### III. On-Device-First Storage

All persistent data MUST be stored on-device in SQLite (via Room), which is the ONLY
source of truth. There is no remote store and no sync queue. Data created on-device
remains on-device. The app MUST NOT depend on any network service for reads, writes,
or integrity.

### IV. Native Android with Modern Stack

The app MUST be built using:
- **Language**: Kotlin (100%)
- **UI**: Jetpack Compose with Material 3
- **Architecture**: MVVM + Clean Architecture
- **DI**: Hilt
- **Navigation**: Compose Navigation
- **Gradle**: Kotlin DSL (build.gradle.kts)
- **Min SDK**: 26 (Android 8.0) | **Target SDK**: 36 | **compileSdk**: 37

### V. Local-Only Operation

The app MUST operate fully offline by design:
- All score data lives exclusively in the on-device SQLite database
- CSV export/import is the ONLY data-exchange mechanism
- No account, upload, or sync status UI exists
- No network permission is declared; the app MUST have no network surface

## Technology Stack & Constraints

| Layer | Technology | Version |
|-------|-----------|---------|
| Language | Kotlin | 2.3.21 |
| UI Framework | Jetpack Compose | Latest BOM |
| Architecture | MVVM + Clean Architecture | - |
| DI | Hilt | Latest |
| Storage | SQLite via Room | 2.8.4 |
| CSV | Kotlin stdlib (RFC 4180) | - |
| Build System | Gradle (Kotlin DSL) | 9.x |
| Testing | JUnit5 + Mockk + Turbine | Latest |
| Min SDK | 26 | - |
| Target SDK | 36 | - |
| compileSdk | 37 (required by Compose BOM 2026.08.00) | - |

**Constraints**:
- APK size MUST NOT exceed 15MB
- Cold start MUST be under 2 seconds on mid-range devices
- No network permissions (no `INTERNET`, no `ACCESS_NETWORK_STATE`)
- No hardcoded strings in code; use string resources

## Development Workflow & Quality Gates

### Branch Strategy
- `main`: Production-ready code
- `develop`: Integration branch
- `feature/*`: Feature branches off develop
- `fix/*`: Bug fix branches off develop

### Quality Gates (mandatory before merge)
1. All unit tests pass
2. All local/instrumented integration tests pass
3. Lint check passes with zero errors
4. No dependency vulnerabilities (critical/high)
5. Code review approved
6. TDD cycle verified (test-first evidence in commit history)

### Commit Convention
- Format: `type(scope): description`
- Types: `feat`, `fix`, `test`, `refactor`, `chore`, `docs`
- Scope: feature area (e.g., `score`, `session`, `csv`, `ui`)

## Governance

This constitution supersedes all other development practices for the ArcheryScore project.

- All pull requests MUST verify compliance with these principles
- Amendments MUST be documented with rationale and version bump
- Versioning follows semantic versioning: MAJOR (principle removal/redefinition), MINOR (new principle/section), PATCH (clarifications)
- Compliance review is required before any release
- For runtime development guidance, refer to AGENTS.md

**Version**: 2.0.0 | **Ratified**: 2026-09-08 | **Last Amended**: 2026-09-14
