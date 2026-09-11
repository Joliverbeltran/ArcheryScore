# ArcheryScore — How to Use

## 1. Configure your session

Open the app and fill in the **New session** form on the Start tab:

| Field | Description |
|---|---|
| **Discipline** | Olympic Recurve, Traditional Recurve, Barebow, Longbow, or Compound |
| **Round type** | Ten zone (0–10) or Five zone (0–5) |
| **Distance (m)** | Shooting distance in meters |
| **Ends** | Number of ends to shoot (1–30) |
| **Arrows per end** | Arrows per end (1–12) |
| **Notes** | Optional notes for the session |

Tap **Start session** to begin.

## 2. Record ends

On the active session screen:

1. Tap **Add end** to create a new end with placeholder arrows (default score 10).
2. The header shows your progress (e.g. "Ends 2 of 6").
3. The running total and X count update automatically.

## 3. Set the score for each arrow

1. Tap any arrow chip to open the score dialog.
2. Select a score (0 = miss, up to the max for your round type).
3. For **Ten zone** rounds, toggle the **X ring** switch when the score is 10.
4. Tap **Save**.

Repeat for every arrow in the end.

## 4. Finish the session

The **Finish session** button becomes enabled once:
- You have created all configured ends.
- Every arrow in every end has a score.

Tap **Finish session** and confirm. The session moves to your history and can no longer be edited.

## 5. View history and export

- Open the **Recent** tab to see past sessions.
- Tap a session to view its end-by-end breakdown.
- Tap **Export CSV** to share the session data via any app (email, messaging, cloud drive).
- Tap **Delete session** to permanently remove a session and all its arrows.

## 6. View statistics

Open the **Stats** tab to see aggregate data:
- Total sessions, arrows, and cumulative score.
- Best session score, average score, and average accuracy %.
- Breakdown by discipline.

## Tips

- If you close the app with an active session, reopen it and tap **Resume active session** to continue.
- Only one active session is allowed at a time.
- Scores sync automatically to Supabase when you have a network connection.
