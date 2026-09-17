# Feature Specification: Slider Input for Session Setup

**Feature Branch**: `004-slider-input-methods`

**Created**: 2026-09-15

**Status**: Draft

**Input**: User description: "Change input methods. Use slider to set distance (8, 12,18,30,40,50,70,90 meters). Use slider to select number of arrows (1,3,6). Use a slider to select number of ends (1,3,6,9,12). This will improve input method by removing the need of keyboard and aligns with standard archary tournament practices."

## Clarifications

### Session 2026-09-17

- Q: Default-preference behavior after session creation → A: Auto-save the last used slider values as the new stored preference defaults (Option A)
- Q: CSV round-trip for the new 8 m distance value → A: Relax the importer lower distance bound to accept ≥ 8 m so all slider values round-trip (Option A)

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Set Distance with a Slider (Priority: P1)

An archer opens the session setup screen to configure a new round. Instead of typing a distance into a text field, they see a slider with the standard archery distances (8, 12, 18, 30, 40, 50, 70, 90 meters). The archer drags the slider to the desired distance and sees the selected value displayed clearly. No keyboard appears at any point during this interaction.

**Why this priority**: Distance selection is the first and most frequently adjusted setting. Replacing the text field with a slider immediately removes the keyboard requirement for this field and is the core change requested.

**Independent Test**: Can be fully tested by opening the session setup screen, interacting with the distance slider, verifying only the 8 predefined values are selectable, and confirming the selected value is reflected in the session configuration without any keyboard appearing.

**Acceptance Scenarios**:

1. **Given** the session setup screen is open, **When** the user views the distance setting, **Then** a slider is displayed with the currently selected distance value shown as a label
2. **Given** the distance slider is at a current value, **When** the user drags the slider left or right, **Then** the slider snaps to the nearest predefined distance step (8, 12, 18, 30, 40, 50, 70, or 90 meters) and does not allow values between steps
3. **Given** the distance slider is at any position, **When** the user taps or drags to a new position, **Then** no keyboard appears on screen
4. **Given** the user has selected a distance via the slider, **When** they start a new session, **Then** the session is created with the selected distance value
5. **Given** the user has previously set a default distance in preferences, **When** the session setup screen opens, **Then** the slider is pre-positioned at the saved default distance (or the closest valid step if the saved value is not a valid step)

---

### User Story 2 - Set Arrows Per End with a Slider (Priority: P1)

The archer configures how many arrows are shot per end. Instead of typing a number, they use a slider with three standard options: 1, 3, or 6 arrows. The slider clearly shows which value is selected, and the interaction is quick and keyboard-free.

**Why this priority**: Arrows per end is a fundamental session parameter that determines the structure of the round. It is equally critical to distance and should be changed in the same interaction.

**Independent Test**: Can be fully tested by opening the session setup screen, interacting with the arrows-per-end slider, verifying only the 3 predefined values are selectable, and confirming the session uses the selected value.

**Acceptance Scenarios**:

1. **Given** the session setup screen is open, **When** the user views the arrows-per-end setting, **Then** a slider is displayed with the currently selected value shown as a label
2. **Given** the arrows-per-end slider is at a current value, **When** the user drags the slider, **Then** the slider snaps to one of the three predefined steps (1, 3, or 6) and does not allow intermediate values
3. **Given** the arrows-per-end slider is at any position, **When** the user interacts with it, **Then** no keyboard appears on screen
4. **Given** the user has selected an arrows-per-end value via the slider, **When** they start a new session, **Then** the session is created with the selected arrows-per-end value
5. **Given** the user has previously set a default arrows-per-end in preferences, **When** the session setup screen opens, **Then** the slider is pre-positioned at the saved default (or the closest valid step)

---

### User Story 3 - Set Number of Ends with a Slider (Priority: P1)

The archer configures how many ends the round will contain. Instead of typing a number, they use a slider with standard options: 1, 3, 6, 9, or 12 ends. The slider interaction is consistent with the other two sliders on the same screen.

**Why this priority**: End count defines the session length and is the third parameter that completes the session configuration trio. All three sliders work together as a unified input pattern.

**Independent Test**: Can be fully tested by opening the session setup screen, interacting with the ends slider, verifying only the 5 predefined values are selectable, and confirming the session uses the selected value.

**Acceptance Scenarios**:

1. **Given** the session setup screen is open, **When** the user views the ends setting, **Then** a slider is displayed with the currently selected value shown as a label
2. **Given** the ends slider is at a current value, **When** the user drags the slider, **Then** the slider snaps to one of the five predefined steps (1, 3, 6, 9, or 12) and does not allow intermediate values
3. **Given** the ends slider is at any position, **When** the user interacts with it, **Then** no keyboard appears on screen
4. **Given** the user has selected an end count via the slider, **When** they start a new session, **Then** the session is created with the selected end count
5. **Given** the user has previously set a default end count in preferences, **When** the session setup screen opens, **Then** the slider is pre-positioned at the saved default (or the closest valid step)

---

### User Story 4 - Keyboard-Free Session Setup (Priority: P2)

The archer completes an entire session setup without ever seeing a keyboard. All three numeric parameters (distance, arrows per end, ends) are configured via sliders. The only remaining text input is the optional notes field. The setup flow feels faster and more aligned with tournament practices.

**Why this priority**: This is the holistic outcome of Stories 1–3 working together. It validates that the overall session setup experience achieves the stated goal of removing keyboard dependency.

**Independent Test**: Can be tested by starting a new session, configuring all three slider settings, and confirming that no keyboard appears at any point during the configuration (only the notes field, if used, would trigger a keyboard).

**Acceptance Scenarios**:

1. **Given** the session setup screen is open, **When** the user configures distance, arrows per end, and ends using the sliders, **Then** no keyboard appears during any of these interactions
2. **Given** all three slider values have been set, **When** the user taps the start session button, **Then** a valid session is created with all three configured values
3. **Given** the session setup screen is open with all sliders at their defaults, **When** the user adjusts any slider and starts a session, **Then** the session uses the slider values, not the old text-field defaults
4. **Given** the user creates a session with specific slider values, **When** they open the session setup screen again for a new session, **Then** all three sliders are pre-positioned at the last-used values without any manual re-entry

---

### Edge Cases

- What happens when a user's previously saved default distance is not one of the 8 predefined slider steps (e.g., a value from an older version like 25m)? The slider snaps to the closest valid step.
- What happens when the app is upgraded from a version that used free-text input with custom values not in the new slider steps? The slider defaults to the closest valid step for any out-of-range saved preference.
- What happens if the user wants a distance not in the predefined list (e.g., 50m is available but 55m is not)? The system only offers the 8 predefined distances; custom distances are not supported through the slider.
- What happens to sessions already created with the old text-field input? They remain unchanged and unaffected; the slider change only affects new session setup.
- What happens when an 8 m session is exported and re-imported? The CSV importer accepts distances of 8 m and above, so the session round-trips without error.
- What happens when the notes text field is focused? The keyboard appears for notes only, which is expected and acceptable since notes require free-text input.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST replace the free-text distance input with a step slider offering exactly 8 discrete values: 8, 12, 18, 30, 40, 50, 70, and 90 meters
- **FR-002**: System MUST replace the free-text arrows-per-end input with a step slider offering exactly 3 discrete values: 1, 3, and 6 arrows
- **FR-003**: System MUST replace the free-text ends input with a step slider offering exactly 5 discrete values: 1, 3, 6, 9, and 12 ends
- **FR-004**: System MUST display the currently selected value as a visible label alongside each slider so the user always knows what value is selected
- **FR-005**: System MUST snap each slider to the nearest valid step when the user drags it; intermediate values between steps MUST NOT be selectable
- **FR-006**: System MUST NOT display a keyboard when the user interacts with any of the three sliders
- **FR-007**: System MUST persist the selected slider values as session configuration (distance, arrows per end, end count) when the session is created
- **FR-008**: System MUST pre-populate each slider with the user's saved default preference when the session setup screen opens, snapping to the nearest valid step if the saved value is not a valid step
- **FR-009**: System MUST preserve existing sessions and their configuration created with the previous text-field input method; historical data MUST NOT be affected
- **FR-010**: System MUST auto-save the last selected slider values (distance, arrows per end, end count) into the stored preference defaults when a session is created, so the next new session opens pre-positioned at those values (replacing the previous defaults)
- **FR-011**: System MUST allow the optional notes text field to continue functioning as a free-text input with keyboard; this is the only field where a keyboard may appear
- **FR-012**: System MUST keep all existing session setup options (discipline dropdown, round type dropdown, target type dropdown) unchanged; only the three numeric inputs are converted to sliders
- **FR-013**: System MUST accept distances of 8 m and above when validating imported CSV data, so any session created via the distance slider survives a CSV export/import round-trip

### Key Entities *(include if feature involves data)*

- **Session Configuration**: The set of parameters used to create a new session — distance (meters), arrows per end, end count, discipline, round type, target type, and notes. The slider change affects how the first three parameters are input but does not change the entity model itself.
- **User Preferences**: Stored defaults for session setup parameters. The distance, arrows-per-end, and end-count defaults now correspond to valid slider step values rather than arbitrary integers. Existing preference values outside the new step sets are handled by snapping to the nearest valid step. On each session creation, the selected slider values auto-save as the new defaults (FR-010).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Users can configure distance, arrows per end, and end count without any keyboard interaction — 0 keyboard appearances for these three settings
- **SC-002**: Users can complete a full session setup (all three slider settings plus any dropdown selections) in under 15 seconds from screen open to session start
- **SC-003**: 100% of slider interactions snap to valid predefined steps — no intermediate or out-of-range values can be selected
- **SC-004**: The selected value for each slider is always visible as a label, eliminating any ambiguity about what value is currently set
- **SC-005**: Existing sessions created with the old text-field input are fully preserved with 0% data regression
- **SC-006**: The three predefined step sets cover all standard archery tournament configurations: distances (8–90m), arrows (1, 3, 6), and ends (1–12)
- **SC-007**: After a session is created, the next new session's sliders open pre-positioned at the last-used values (auto-saved defaults) without any additional user action
- **SC-008**: 100% of sessions created with any slider distance value (including 8 m) export to CSV and re-import successfully with all values intact

## Assumptions

- The 8 distance values (8, 12, 18, 30, 40, 50, 70, 90 meters) represent the standard competition distances used in World Archery and national federation tournaments; custom distances outside this set are not needed for the slider
- The 3 arrows-per-end values (1, 3, 6) cover the standard tournament end sizes: 1 arrow for single-arrow ends, 3 arrows for half-end practice, and 6 arrows for full WA standard ends
- The 5 end-count values (1, 3, 6, 9, 12) cover common round lengths; the maximum of 12 ends aligns with standard 72-arrow qualifying rounds (12 ends x 6 arrows)
- Users who previously entered custom values via text fields (e.g., distance of 25m or 99m) will have their defaults snapped to the nearest valid slider step upon upgrade; this is acceptable because such non-standard values were rare and the slider steps cover all tournament-standard configurations
- The CSV importer's distance validation is aligned to the slider's range (minimum 8 m) while keeping the existing maximum, so exported data remains importable and legacy files with distances ≥ 10 m are unaffected
- The notes text field remains as free-text input; requiring keyboard-free input for notes is impractical since notes are inherently unstructured text
- The three slider controls follow the app's existing visual design language so the setup screen feels consistent, with no introduced third-party components
- Performance, accessibility, and UI/UX requirements from the original archery and local-storage specs are inherited and remain in force
