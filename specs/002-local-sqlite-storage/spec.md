# Feature Specification: Local-Only Storage (Remove Supabase)

**Feature Branch**: `002-local-sqlite-storage`

**Created**: 2026-09-11

**Status**: Draft

**Input**: User description: "After app initial usage, I have realized supabase is not really necessary. Change Supabase by an internal SQL Lite database to store score data and generate statistics. This will eliminate the need for sync. Update constitution accordingly after this change."

## Clarifications

### Session 2026-09-11

- Q: Since the app becomes device-only, how should users protect against or recover from data loss? → A: Add the option to import the CSV export, enabling a full CSV round-trip (export scores to CSV, re-import that CSV to restore data on the device)

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Record a Session Fully On-Device (Priority: P1)

An archer opens the app to record their practice round. The app opens directly to the home screen with no sign-in step, no account prompts, and no network requirement. The archer creates a session, enters scores for every arrow, sees the running total, and finishes the session. All data is saved to the device itself, and no sync or cloud upload happens at any point.

**Why this priority**: Local, friction-free score recording is the core value of the app. Removing the account and sync dependency is the reason for this change.

**Independent Test**: Can be tested fully offline (airplane mode) by creating a session, entering scores, finishing it, and verifying the finished session appears in history after restarting the app — with no sync indicators ever shown.

**Acceptance Scenarios**:

1. **Given** a device with no internet connectivity and no existing account, **When** the user launches the app and records a session, **Then** every score is saved successfully on the device and no sync/upload indicator is shown
2. **Given** the user has never signed in or created an account, **When** they open the app, **Then** they reach the score recording screen directly without any sign-in, sign-up, or account-required flow
3. **Given** a session was recorded on-device, **When** the app is closed and reopened, **Then** the session and its scores are still present and intact

---

### User Story 2 - View History from Local Data (Priority: P2)

An archer wants to review past sessions. They open the history screen and see all previously recorded sessions, ordered newest first, with correct totals and per-end breakdowns. All of it comes from data stored on the device; nothing depends on a connection or remote server.

**Why this priority**: Reviewing past performance is the second most valuable capability and depends on the local data from Story 1.

**Independent Test**: Can be tested offline by recording multiple sessions, then verifying they appear in history with correct details and open correctly in the detail view.

**Acceptance Scenarios**:

1. **Given** the device is offline, **When** the user opens history, **Then** all recorded sessions are listed newest first with correct totals
2. **Given** a session is in the history list, **When** the user taps it, **Then** the detailed breakdown is shown from on-device data alone
3. **Given** there are no recorded sessions, **When** the user opens history, **Then** an encouraging empty state is shown

---

### User Story 3 - Statistics Computed Locally (Priority: P2)

An archer wants to see their performance trends. They open the statistics screen and see average score, best session, and improvement trend computed across all completed sessions stored on the device. No remote data is consulted or required.

**Why this priority**: Statistics are a key feature and must work identically when computed purely from local data.

**Independent Test**: Can be tested offline by recording sessions with known scores, then verifying statistics are accurate and match calculations from the stored data.

**Acceptance Scenarios**:

1. **Given** multiple completed sessions are stored on the device, **When** the user opens statistics while offline, **Then** average, best session, and trend are shown and match the locally stored scores
2. **Given** fewer than 3 sessions exist, **When** the user opens statistics, **Then** the app explains more data is needed for meaningful trends
3. **Given** a device with no data connections, **When** the user accesses statistics, **Then** statistics load within the existing performance budget without any network wait

---

### User Story 4 - Clean Offline-First Experience (Priority: P3)

Across every screen the user sees no account, cloud, synchronization, or "pending upload" concept. The app behaves as a simple on-device scorebook with no external dependencies, making it more private and simpler to understand.

**Why this priority**: Removing the sync/cloud surface is the explicit goal and touches the whole app, but delivers value only on top of Stories 1–3.

**Independent Test**: Can be tested by walking through record, history, statistics, and settings while offline and confirming no sync/account UI appears anywhere and no network permission activity is observable.

**Acceptance Scenarios**:

1. **Given** the user uses the app on a device with no connectivity, **When** they navigate record, history, and statistics, **Then** no sync, pending-upload, or network error messaging is shown
2. **Given** an existing recorded session from a prior version, **When** the user upgrades to this version, **Then** their recorded sessions remain available
3. **Given** the user wants to export their data, **When** they use the export feature offline, **Then** the CSV export still works from on-device data
4. **Given** the user has a CSV export of their scores, **When** they import that CSV, **Then** the exported sessions and scores are restored on the device

---

### Edge Cases

- What happens to data that was previously uploaded to the cloud server when a user upgrades? (See Assumptions — cloud data is not migrated; local on-device sessions are retained.)
- What happens if the device storage is nearly full while saving a session?
- What happens if the user records a session with no previous sessions / no history (fresh install)?
- What happens if the app is force-closed mid-session — is the in-progress session preserved locally as before?
- What happens if the on-device database is empty or corrupted — does the app recover gracefully instead of crashing?
- What happens if the user starts a new session while one is already in progress — is the single-active-session invariant still enforced locally?
- What happens if the user imports a malformed, truncated, or incompatible CSV file — does the app reject it cleanly without corrupting existing data?

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST allow users to record, finish, resume, edit, and delete archery sessions using only on-device storage, with no account, authentication, or cloud service required
- **FR-002**: System MUST store all session data (sessions, ends, arrows, preferences) exclusively in the device's internal on-device database and read all data from it
- **FR-003**: System MUST compute history and statistics solely from data in the on-device database
- **FR-004**: System MUST NOT perform any cloud upload, remote read, or background sync work
- **FR-005**: System MUST NOT display any sync status indicators (synced, pending, error) anywhere in the UI
- **FR-006**: System MUST NOT require or attempt any form of sign-in, sign-up, or user identity and MUST launch directly to the home screen
- **FR-007**: System MUST preserve existing locally recorded sessions when a user upgrades from a prior version that used cloud sync
- **FR-008**: System MUST keep all core features (record, history, statistics, export) fully functional with no internet connectivity
- **FR-009**: System MUST export sessions to CSV from on-device data via the Android share sheet, requiring no connection
- **FR-010**: System MUST enforce the single-active-session invariant using only local data
- **FR-011**: System MUST preserve an in-progress session across app restarts and force-closes using local storage
- **FR-012**: System MUST handle a missing or corrupted local database gracefully (e.g., empty state / recovery) without crashing
- **FR-013**: Project governance documentation (the project constitution) MUST be updated to reflect on-device-first storage and the removal of cloud sync before this change is merged
- **FR-014**: System MUST allow users to import an exported CSV file back into the app, restoring the sessions and scores it contains, so data can be recovered from a CSV backup
- **FR-015**: System MUST validate imported CSV files and reject malformed or incompatible files with a clear, non-crashing error message, leaving existing data unchanged

### Key Entities *(include if feature involves data)*

- **Session**: An archery practice round stored on the device. Contains metadata (date, distance, discipline, notes) and a collection of ends. Lifecycle: active (resumable) → complete; completed sessions remain editable with confirmation
- **End**: A group of arrows shot together (typically 3 or 6 arrows). Belongs to one session and contains individual arrow scores
- **Arrow**: A single shot score within an end. Contains the numeric score value
- **User Preferences**: App settings (default round types, display preferences) stored on the device
- **No remote identity entity** is required; data is not namespaced by user or account

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Users can record, finish, and later review a complete session with zero network access; 100% of recorded scores are present after app restart
- **SC-002**: 100% of app launches reach the home screen without a sign-in, account, sync, or network prompt
- **SC-003**: Users can access history and statistics within the existing performance budget (history ≤ 2s, statistics ≤ 1s for up to 500 sessions) with no connectivity
- **SC-004**: No sync status or cloud-related messaging appears in the app UI
- **SC-005**: 100% of sessions recorded by existing users of the prior version remain available after upgrading
- **SC-006**: The app performs no observable network/cloud activity during normal use (record, history, statistics, export)
- **SC-007**: CSV export succeeds offline in 100% of export attempts
- **SC-008**: The project constitution (governance document) accurately reflects on-device-only storage, and all principle gates referencing cloud sync pass review
- **SC-009**: 100% of sessions and scores contained in a valid CSV export can be restored by importing that CSV back into the app

## Assumptions

- Data that was previously uploaded to the cloud server will not be migrated back onto the device; the app is single-user, initial usage is light, and cloud-held data is considered out of scope
- Sessions already stored in the device's local database from the prior version are retained and continue to work
- The app targets a single user on a single device; multi-device synchronization is not needed and is intentionally eliminated
- The existing on-device SQLite database already present in the app is the storage mechanism; it becomes the sole and authoritative store rather than a cache
- Removal of cloud dependencies should also remove the associated account/sign-in surfaces (there is no value in an account without cloud)
- No internet connectivity is expected or required for any app feature
- Performance, scoring-rule, and UI/UX requirements from the original archery spec remain unchanged and are inherited