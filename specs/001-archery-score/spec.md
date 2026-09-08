# Feature Specification: Archery Score Tracking

**Feature Branch**: `001-archery-score`

**Created**: 2026-09-08

**Status**: Draft

**Input**: User description: "Create an archery score tracking mobile app for Android. Use a native language to generate an apk package that can be installed in device. Use only updated libraries with no vulnerabilities. Use Supabase for storage. Develop using Test Driven Design."

## Clarifications

### Session 2026-09-08

- Q: After a session is marked complete, should scores remain editable? → A: Editable with confirmation
- Q: Can an in-progress session be resumed after interruption? → A: Resumable — persists and auto-restores on next launch
- Q: How should sync conflicts between multiple devices be resolved? → A: Last-write-wins per arrow score
- Q: What session metadata should be tracked? → A: Distance + discipline per session
- Q: Which disciplines must be supported? → A: Olympic recurve, traditional recurve, barebow, longbow, compound bow
- Q: Should the app support data export? → A: CSV export via Android share sheet

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Record an Archery Round (Priority: P1)

An archer completes a round of shooting and opens the app to record their scores. They create a new session, enter scores for each arrow across all ends, and the app calculates totals and saves the session. The archer can see their running total as they enter scores.

**Why this priority**: Score recording is the core value proposition. Without this, the app has no purpose.

**Independent Test**: Can be fully tested by creating a session, entering scores, and verifying totals are calculated and persisted correctly.

**Acceptance Scenarios**:

1. **Given** the app is open on the home screen, **When** the user taps "New Session", **Then** a new empty session is created with configurable end count and arrows per end
2. **Given** a session is active, **When** the user enters a score for an arrow, **Then** the running total updates immediately and the score is saved
3. **Given** a session is active, **When** the user completes all ends, **Then** the session is marked complete and shows the final score summary
4. **Given** the user has no internet connectivity, **When** they enter scores, **Then** scores are saved locally and the user sees a pending sync indicator

---

### User Story 2 - View Score History (Priority: P2)

An archer wants to review their past shooting sessions. They open the history screen and see a list of all previous sessions with dates, total scores, and session details. They can tap any session to see a detailed breakdown.

**Why this priority**: Reviewing past performance is essential for tracking improvement over time.

**Independent Test**: Can be tested by creating multiple sessions, then verifying they appear in history with correct details and can be opened for detailed view.

**Acceptance Scenarios**:

1. **Given** the user has completed sessions, **When** they open the history screen, **Then** all past sessions are listed in reverse chronological order
2. **Given** the history list is displayed, **When** the user taps a session, **Then** a detailed view shows all ends, arrow scores, and statistics
3. **Given** the user has no internet, **When** they open history, **Then** locally cached sessions are displayed with a sync status indicator
4. **Given** the history is empty, **When** the user opens the history screen, **Then** an empty state message encourages them to record their first session

---

### User Story 3 - Track Performance Statistics (Priority: P3)

An archer wants to see their performance trends over time. They access a statistics view that shows average scores, improvement trends, and session comparisons.

**Why this priority**: Statistics provide motivation and insight but require historical data from sessions.

**Independent Test**: Can be tested by creating sessions with known scores, then verifying statistics calculations and trend displays are accurate.

**Acceptance Scenarios**:

1. **Given** the user has completed multiple sessions, **When** they open statistics, **Then** they see average score per session, best session, and improvement trend
2. **Given** statistics are displayed, **When** the user selects a date range, **Then** statistics filter to show only sessions within that range
3. **Given** the user has fewer than 3 sessions, **When** they open statistics, **Then** the app shows a message that more data is needed for meaningful trends

---

### Edge Cases

- What happens when the user enters an invalid score (negative, or above maximum for the round type)?
- How does the app handle a network failure during sync of completed sessions?
- What happens if the user force-closes the app mid-session?
- How does the app handle storage quota limits on Supabase?
- What happens when the user tries to create a session while another is in progress?

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST allow users to create new archery sessions with configurable parameters (number of ends, arrows per end, scoring type)
- **FR-002**: System MUST validate score entries against valid ranges for the configured round type
- **FR-003**: System MUST calculate and display running totals in real-time during score entry
- **FR-004**: System MUST persist all session data to cloud storage when connected
- **FR-005**: System MUST queue score entries locally when offline and sync automatically when connectivity resumes
- **FR-006**: System MUST display score history sorted by date (newest first)
- **FR-007**: System MUST show detailed breakdown per end when viewing a session
- **FR-008**: System MUST calculate statistics (average, best, trend) across completed sessions
- **FR-009**: System MUST allow users to delete individual sessions with confirmation
- **FR-010**: System MUST display clear sync status indicators (synced, pending, error)
- **FR-011**: System MUST support standard archery scoring (1-10 for 10-zone, 1-5 for 5-zone, X-ring scoring)
- **FR-012**: System MUST show an empty state when no sessions exist
- **FR-013**: System MUST generate a downloadable APK for Android device installation
- **FR-014**: System MUST allow editing of arrow scores in completed sessions, but MUST require user confirmation before any score modification
- **FR-015**: System MUST persist in-progress sessions and restore them automatically on app relaunch
- **FR-016**: System MUST resolve multi-device sync conflicts using last-write-wins per arrow score, based on edit timestamps
- **FR-017**: System MUST record shooting distance (e.g., 18m, 30m, 70m) and discipline for each session. Supported disciplines: olympic recurve, traditional recurve, barebow, longbow, compound bow
- **FR-018**: System MUST export sessions to CSV format and make it shareable via the Android share sheet

### Key Entities

- **Session**: A single archery practice or competition round. Contains metadata (date, round type, distance, discipline [olympic recurve, traditional recurve, barebow, longbow, compound bow], notes) and a collection of ends. Lifecycle: active (resumable) → complete; completed sessions remain editable with confirmation
- **End**: A group of arrows shot together (typically 3 or 6 arrows). Belongs to a session and contains individual arrow scores
- **Arrow**: A single shot score within an end. Contains the numeric score value
- **User Preferences**: App settings including default round types, display preferences, and sync configuration

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Users can record a complete archery session in under 3 minutes from opening the app
- **SC-002**: Score entry responsiveness is under 100ms from input to total update
- **SC-003**: 100% of score entries are preserved across app restarts and connectivity loss
- **SC-004**: Users can access score history within 2 seconds of tapping the history tab
- **SC-005**: Statistics calculations complete within 1 second for up to 500 sessions
- **SC-006**: The app APK installs successfully on Android 8.0+ devices
- **SC-007**: Zero data loss during offline-to-online sync transitions
- **SC-008**: All core features (record, view, statistics) work without internet connectivity after initial load

## Assumptions

- Users are individual archers tracking personal practice sessions, not tournament organizers
- Standard archery scoring rules (10-zone and 5-zone) are sufficient for v1
- Users have Android devices running Android 8.0 or higher
- Internet connectivity is available at least occasionally for data sync
- Users will authenticate using email/password
- A single user account supports one archer's data (multi-archer/competition features are out of scope for v1)
- The app targets personal use, not professional tournament management
