# Phase 0 Research: Current Stable Android Stack

**Date**: 2026-09-08
**Scope**: Verify latest stable, vulnerability-free versions of every dependency required by the constitution, maximize SDK level compatibility, and select a mutually compatible toolchain.

## Decisions

| Concern | Selected | Rationale |
|---------|----------|-----------|
| Language | Kotlin **2.3.21** | Latest stable is 2.4.10, but Dagger 2.60.x is built against Kotlin 2.3.x. 2.3.21 guarantees Hilt/KSP compatibility with zero risk. |
| Build system | Gradle **9.4.1+** + AGP **9.2.0** | AGP 9.4.0 shipped Sep 2026 (too new); 9.0.1 (Jan) and 9.2.0 (Apr) are stable. 9.2.0 is the conservative-but-current choice. AGP 9 requires Gradle 9.x. |
| CI/toolchain SDK | compileSdk **37**, targetSdk **36** | Compose BOM 2026.08.00 (Compose 1.12.0) requires compileSdk 37. Play policy for Aug 2026 requires target ≥ 36. minSdk stays 26 (Android 8.0, per SC-006). |
| UI | Compose BOM **2026.08.00** | Stable (Aug 12, 2026), ships Compose 1.12.0, Material 3, Navigation Compose 2.10.0. |
| DI | Dagger/Hilt **2.60.1** + `androidx.hilt:hilt-navigation-compose:1.4.0` | Latest stable (Jul 2026); requires AGP 9.0+. |
| Navigation | Navigation Compose **2.10.0** | Latest stable AndroidX nav; Navigation3 1.1.7 exists but 2.10.0 is the conservative stable line. |
| Local cache | Room **2.8.4** | Stable line (Nov 2025); `androidx.room` is the documented offline-first companion. Room 3.0.2 exists but is a major line without a track record for this project. |
| Preferences | DataStore **1.2.1** | Latest stable (Jul 2026). |
| Background sync | WorkManager **2.11.0+** | Latest release window Mar 25, 2026. |
| Networking (Supabase) | supabase-kt BOM **3.8.0** (`io.github.jan-tennert.supabase:bom`) | Latest supabase-kt (Aug 27, 2026). Modules: `auth-kt`, `postgrest-kt`, `realtime-kt`, `storage-kt`, `functions-kt`. Requires minSdk 26 — matches constitution. |
| HTTP client (transitive) | Ktor **3.4.0** (`ktor-client-okhttp`) | Latest stable (Jan 2026). |
| Testing | JUnit5 + MockK (latest, ≥ 1.14.6) + Turbine (1.3.x) + Room testing | MockK latest release May 2026. Turbine latest 1.3.x line. |
| AndroidX core | core-ktx **1.19.0**, activity-compose **1.13.0**, lifecycle-runtime-ktx **2.10.0** | Latest stable AndroidX. |

## Notes / Open items

1. **Constitution conflict (flagged)**: constitution v1.0.0 pins Target SDK **34** and Gradle 8.x, but the current AndroidX/AGP ecosystem (2026) requires compileSdk 37 and Gradle 9.x. Planning decision: adopt compileSdk 37 / targetSdk 36 / Gradle 9.4.1+. Requires a constitution amendment (PATCH) before implementation is merged. See `CONSTITUTION_AMENDMENT_FOLLOWUP` in plan.md.
2. **Hilt + KSP**: Dagger 2.60.x supports KSP (incl. KSP2). KSP version must match AGP 9.2.0 (com.google.devtools.ksp) — verify exact KSP release at implementation time.
3. **X-ring scoring** (FR-011): arrow stores `score` (1-10) plus `is_X_ring` flag; total counts X separately in stats. Keep in Arrow entity.
4. **CVE scanning**: All selected versions are the latest stable lines as of 2026-09-08. Before merge, run `dependencyCheck` (or `org.owasp:dependency-check-gradle`) as the quality gate (#4). Any critical/high CVE without patch blocks the build per constitution principle II.
5. **APK ≤ 15MB**: Single `:app` module, R8/proguard with Compose optimizations keep size in budget (constitution constraint).

## Version compatibility matrix (selected)

| Component | Version |
|-----------|---------|
| Gradle | 9.4.1+ |
| AGP | 9.2.0 |
| Kotlin | 2.3.21 |
| Compose BOM | 2026.08.00 |
| Hilt | 2.60.1 |
| navigation-compose | 2.10.0 |
| supabase-kt BOM | 3.8.0 |
| Room | 2.8.4 |
| DataStore | 1.2.1 |
| WorkManager | 2.11.0+ |
| core-ktx | 1.19.0 |
| activity-compose | 1.13.0 |
| lifecycle-runtime-ktx | 2.10.0 |
| Ktor | 3.4.0 |
| MockK | ≥ 1.14.6 |
| Turbine | 1.3.x |
| minSdk / targetSdk / compileSdk | 26 / 36 / 37 |