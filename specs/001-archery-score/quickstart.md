# Quickstart — ArcheryScore

**Phase 1 output** | Set up the dev environment, run tests, build the APK.

## 1. Requirements

- JDK 21 (AGP 9 requires JDK 17+; 21 recommended)
- Android Studio latest stable (or CLI: `sdkmanager`)
- Android SDK: platform 37, build-tools 37, platform-tools
- A Supabase project (URL + anon key) — see contract 4 below

## 2. Environment

1. Copy local config and fill in your Supabase credentials (git-ignored, constitution II):
   ```bash
   cp local.properties.example local.properties
   ```
   Add:
   ```properties
   sdk.dir=/path/to/Android/sdk
   SUPABASE_URL=https://<project-ref>.supabase.co
   SUPABASE_ANON_KEY=<anon-key>
   ```
2. Apply the database schema to your Supabase project:
   ```bash
   supabase db push   # from supabase/ (uses migrations/*.sql)
   ```
3. Enable **Email + Password** in Supabase Auth settings.

## 3. Validate

```bash
gradle wrapper   # first run generates wrapper from declared Gradle 9.4.1+
./gradlew testDebugUnitTest        # unit tests (TDD: run often)
./gradlew connectedDebugAndroidTest  # instrumented tests (emulator/device)
./gradlew lintDebug                # lint gate (zero errors)
./gradlew dependencyCheckAggregate # CVE scan (no critical/high) — standalone gradle
```

Quality gates map exactly to constitution "Quality Gates": tests pass, lint zero errors, no critical/high vulnerabilities.

## 4. Build the APK (FR-013)

```bash
./gradlew assembleDebug
# output: app/build/outputs/apk/debug/app-debug.apk
./gradlew assembleRelease  # after signing: app/build/outputs/apk/release/app-release.apk
```

Verify the APK installs on an Android 8.0+ (API 26+) device:
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```
APK must stay ≤ 15MB (constitution constraint) — `assembleRelease` with R8 is the size gate; check with `ls -lh` / bundletool if needed.

## 5. TDD workflow

- Write failing test → run it (RED) → implement minimal code → run (GREEN) → refactor. Evidence per commit.
- Test layers: unit (ViewModel/repository/domain), integration (Supabase flows), UI (critical journeys: score entry, session management) — per constitution principle I.

## 6. Useful commands

| Task | Command |
|------|---------|
| Run app | `./gradlew installDebug` |
| Unit tests | `./gradlew testDebugUnitTest` |
| All checks | `./gradlew check` |
| Dep updates | `./gradlew dependencyUpdates` |

## Troubleshooting

- **KSP/AGP mismatch**: keep KSP version aligned to AGP 9.2.0 (see research.md note 2).
- **Compose 1.12 needs compileSdk 37**: if SDK 37 platform missing, `sdkmanager "platforms;android-37"`.
- **Auth "user already registered"**: use a fresh email in the Supabase test project or use the "confirm" flow toggle.