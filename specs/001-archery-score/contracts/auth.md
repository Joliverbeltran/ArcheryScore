# Contract: Authentication

**Spec ref**: FR-017 (user identity for ownership) | Assumption: "Users will authenticate using email/password"

## Supabase Auth

- Provider: Supabase GoTrue (`auth-kt` module of supabase-kt).
- Method: **Email + password** (built on supabase `auth.signUpWith` / `auth.signInWith`).
- `user_id` for all app data = `auth.currentUserOrNull()?.id` (Supabase `auth.uid()` in RLS).

## Client flows

| Flow | Trigger | Behavior |
|------|---------|----------|
| Sign up | User taps "Sign up" | `signUpWith(Email) { email = ...; password = ... }` → verify success; session persists via `SessionStatus.PERSISTED` (default with supabase-kt when server persists session). |
| Sign in | User taps "Sign in" | `signInWith(EmailPassword)` → restore into local store. |
| Restore | App launch | If persisted session exists → auto-authenticate; refresh user in Room `Preferences`. |
| Sign out | User taps "Sign out" | `auth.signOut()` → clear local data cache (keep offline outbox? No — data belongs to the signed-in user; clear on sign-out). |

## Validation rules

- Email: non-empty, valid format (android `Patterns.EMAIL_ADDRESS`).
- Password: ≥ 8 chars (server-enforced minimum).
- Errors map to UI as: `invalid_credentials`, `email_taken`, `weak_password`, `network`.

## Error contract

All auth errors are surfaced as `AuthResult` (success / typed failure) from the `AuthRepository`; UI shows one inline error, never a crash.

## Acceptance link

- TDD: unit tests on `AuthViewModel` (state machine), integration tests with Supabase test project for sign-up → sign-in → restore → sign-out.