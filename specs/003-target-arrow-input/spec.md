# Feature Specification: Visual Target Arrow Placement

**Feature Branch**: `003-target-arrow-input`

**Created**: 2026-09-15

**Status**: Draft

**Input**: User description: "Change input method to show an image of the target and press to place where the arrow has landed. Tapping in the screen will set the arrow position. User can move with the finger until reaching exact position and then click OK button to validate arrow placement. Repeat for all arrows in same end. Include target options in front menu (122 cm, 80 cm, 60 cm, 40 cm, triple vertical, triple triangular). Selected target will be shown when placing the arrows."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Place Arrows Visually on a Target Face (Priority: P1)

An archer finishes shooting an end and enters their scores. Instead of typing a number, they see an image of the target face on screen. They tap where the arrow landed, drag the marker with their finger until it sits exactly where the arrow struck, and confirm with an OK button. The app automatically works out the score from that position and moves on to the next arrow in the end, without the archer ever typing a score.

**Why this priority**: Visual placement is the entire point of this feature — it replaces the current numeric entry method for recording scores.

**Independent Test**: Can be fully tested by starting a new session, placing all arrows of one end by tap-and-drag on the target, tapping OK each time, and verifying each arrow is stored with the correct score and the running total updates.

**Acceptance Scenarios**:

1. **Given** an active end with a target face displayed, **When** the user taps the target, **Then** an arrow marker appears at the tapped position
2. **Given** an arrow marker is placed on the target, **When** the user drags their finger, **Then** the marker follows the finger so the user can reach the exact landing position
3. **Given** the marker is at the desired landing position, **When** the user taps OK, **Then** the arrow is permanently recorded, scored from its position, and the next arrow slot in the end is activated
4. **Given** a marker has not yet been confirmed with OK, **When** the user moves it to a new position, **Then** the previously shown position is not persisted — only the confirmed position counts
5. **Given** all arrows in an end have been confirmed, **When** the last arrow is placed, **Then** the end is completed and the session's running total updates immediately

---

### User Story 2 - Choose the Target Type Up Front (Priority: P1)

Before or during score entry, the archer chooses which target they shot at from the front menu: 122 cm, 80 cm, 60 cm, 40 cm, triple vertical, or triple triangular. The selected target is shown whenever arrows are being placed, so the on-screen face always matches the real target.

**Why this priority**: The correct face must be shown or the tap positions would be scored against the wrong rings. Selection is a prerequisite for Story 1.

**Independent Test**: Can be tested by selecting each of the six options in the front menu and confirming the corresponding face appears during placement and that scores map to that face.

**Acceptance Scenarios**:

1. **Given** the front menu is open, **When** the user opens the target options, **Then** exactly six options are shown: 122 cm, 80 cm, 60 cm, 40 cm, triple vertical, and triple triangular
2. **Given** the user has selected a target type, **When** they start or continue score entry, **Then** the selected target face is displayed for placing arrows
3. **Given** the triple vertical or triple triangular target is selected, **When** the user taps one of the three spots, **Then** the arrow is placed on that spot and scored against that spot's rings
4. **Given** a target type was chosen for a session, **When** the user places arrows throughout that session, **Then** the same target face is shown consistently and the selection persists for the session

---

### User Story 3 - Automatic and Correct Scoring from Placement (Priority: P2)

The archer relies on the app to translate a position on the target into a score. When they confirm an arrow, the app scores it by which ring the marker sits in, applying standard archery conventions: arrows that touch a ring line score the higher value, and arrows outside the target face score a miss.

**Why this priority**: Score correctness is what makes visual placement usable; it can be built and verified once placement exists.

**Independent Test**: Can be tested by placing markers at known positions (inside a specific ring, on a ring boundary, and completely outside the face) and verifying the confirmed score matches standard archery rules for each.

**Acceptance Scenarios**:

1. **Given** a marker is confirmed inside a scoring ring, **When** the arrow is recorded, **Then** its score equals the value of that ring, consistent with the session's configured scoring type
2. **Given** a marker lands touching a ring boundary line, **When** the arrow is recorded, **Then** the arrow scores the higher (inner) of the two ring values, per standard archery rules
3. **Given** a marker is confirmed outside the entire target face, **When** the arrow is recorded, **Then** the arrow is stored as a miss scoring 0
4. **Given** previously placed arrows in the current end exist, **When** the user is placing the next arrow, **Then** the earlier markers remain visible on the target so the archer can compare grouping

---

### Edge Cases

- What happens when the user taps or drags completely outside the target face — does it record a miss (0), and can they still drag back inside before confirming?
- What happens when a tap lands exactly on the boundary between two rings — which score applies?
- What happens when a tap on a triple target lands between or outside all three spots — which spot is the arrow assigned to?
- What happens if the user wants to correct a marker they have already confirmed (misplacement within the same end)?
- What happens when the user starts a new session without changing the target type — what is the default?
- What happens when the target face does not fit the screen comfortably (small screens / rotated device) — is the full face still reachable and accurately tappable?
- What happens to existing sessions recorded with the old numeric input — are they and their scores unaffected?

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST display the session's selected target face whenever arrows are being placed for an end
- **FR-002**: System MUST present six target options in the front menu: 122 cm, 80 cm, 60 cm, 40 cm, triple vertical, and triple triangular
- **FR-003**: System MUST set the arrow marker position at the point where the user taps the target face
- **FR-004**: System MUST allow the user to move the arrow marker by dragging their finger, refining the position until the exact landing spot is reached
- **FR-005**: System MUST NOT persist an arrow's position or score until the user explicitly confirms it with an OK action; moving the marker before confirmation changes only the pending, unconfirmed position
- **FR-006**: System MUST sequence placement through all arrow slots in the current end automatically, activating the next slot after each OK confirmation, until the end is complete
- **FR-007**: System MUST derive each arrow's score from the confirmed marker position against the displayed target's scoring rings, consistent with the session's configured scoring type
- **FR-008**: System MUST record an arrow confirmed outside all scoring rings as a miss scoring 0
- **FR-009**: System MUST score an arrow touching a ring boundary line as the higher (inner) value, following standard archery conventions
- **FR-010**: System MUST support triple vertical and triple triangular faces, assigning a placed arrow to the specific spot it was tapped on (nearest spot when the tap falls between or outside the three spots) and scoring it against that spot's rings
- **FR-011**: System MUST update the session's running total immediately after each OK-confirmed arrow, preserving the existing real-time total behavior
- **FR-012**: System MUST keep previously confirmed arrows of the current end visible on the target while placing subsequent arrows
- **FR-013**: System MUST retain the chosen target type for the duration of a session and use it consistently for all placement in that session
- **FR-014**: System MUST preserve existing sessions and their scores recorded with the previous numeric entry method, leaving history and statistics unaffected
- **FR-015**: System MUST allow the user to correct a confirmed arrow within the current end before the end is completed (e.g., re-place it) with a clear confirmation step
- **FR-016**: System MUST remember the last selected target type and pre-select it as the default when a new session is started

### Key Entities *(include if feature involves data)*

- **Target Type**: The face configuration used for a session — one of 122 cm, 80 cm, 60 cm, 40 cm, triple vertical, triple triangular — together with its ring layout and scoring map. Belongs to a session and drives how an on-screen position is converted to a score
- **Arrow Marker**: The on-screen, draggable placement representing where the arrow landed. Exists only during placement; a confirmed arrow is stored purely as its numeric score so existing arrow data and storage remain unchanged
- **Session / End / Arrow**: Existing entities remain unchanged. The arrow keeps its numeric score value; the visual placement is the input method, not a new stored attribute

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A user can record a full end (e.g., 6 arrows) using only tap, drag, and OK in under 45 seconds without typing any number
- **SC-002**: 100% of OK-confirmed placements result in a valid stored score and require no numeric entry
- **SC-003**: 100% of a verified test set of placements (inside ring, ring boundary, off-face miss) are scored according to standard archery rules
- **SC-004**: At session setup, the user can choose the target type with at most 2 interactions from the front menu, and all six options are always available; the chosen type is fixed for the duration of the session (FR-013)
- **SC-005**: 100% of sessions retain the selected target face consistently during arrow placement, and the running total updates instantly after each OK
- **SC-006**: Existing sessions, history, and statistics are fully preserved — 0% regression after this change
- **SC-007**: Users can correct a misplacement within a current end without losing previously stored arrows

## Assumptions

- The "front menu" is the screen used to set up a session before or while recording scores (where end count, arrows per end, distance, and discipline are configured); the target options are added to that same menu
- The target face scales to fit the available screen area; accuracy comes from finger dragging and the placement marker, not from pinch-zoom (zoom is out of scope for this change)
- The X (inner-ten) ring scores 10, consistent with the existing scoring behavior, and ring layers displayed on the face reflect the session's configured scoring type
- For triple targets, the tap location determines the spot: the spot whose face region contains the tap, or the nearest spot when the tap falls between or outside the three spots
- The visual placement method replaces numeric entry for new score recording; correcting scores in already-completed historical sessions keeps the existing edit flow
- The on-device score storage model (numeric arrow scores) is unchanged, so data recorded visually is compatible with history, statistics, and CSV export from prior specs
- The target type is chosen at session setup and is not changeable mid-session; starting a new session re-opens the choice, pre-selected from the last-used type (FR-016)
- Performance, accessibility, and UI/UX requirements from the original archery and local-storage specs are inherited and remain in force