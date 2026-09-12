# EasyTrain — Project Manifest

> Snapshot of everything built so far, section by section, plus the status of every roadmap
> phase. Claude updates this file in the same PR as any change it describes.
> Last updated: 2026-09-12 — ET-001 (Phase 0) in progress on `feature/ET-001-bootstrap`: module
> skeleton, convention plugins, version catalog, CI and the Supabase workspace exist; no feature
> code yet. ET-002 (auth, profiles, roles) is next.

---

## 1. Architecture Overview

Native Android app (Kotlin + Jetpack Compose), offline-first, backed directly by Supabase
(Postgres + RLS, Auth, Realtime, Storage, Edge Functions). No custom API server.

```
Android  feature/* ──▶ ViewModel ──▶ core/data repositories ◀──Flow── Room (SSOT on device)
                                                │
                                          core/sync SyncEngine  ◀── realtime poke
                                                │ push PENDING (PostgREST upsert) / pull deltas (updated_at > cursor)
Supabase  Postgres (RLS, set_updated_at triggers) · Auth · Realtime · Storage(chat-media) · Edge fn send-push ─▶ FCM
```

Key decisions (details in `.claude/context.md`):
- **One role per account**, chosen once. Coach ↔ trainee linked by invite code via SECURITY DEFINER RPC.
- **Single-writer per table** → last-write-wins by server `updated_at` is safe.
- **Room is the source of truth on device**; UI never blocks on the network.
- **Realtime is a poke**, not a data path; periodic + foreground pulls keep the app correct without it.
- **RLS is the authorization layer**; the app holds only the publishable key.

---

## 2. Roadmap Status

| Phase | Ticket(s) | Scope | Status |
|---|---|---|---|
| 0 | ET-001 | Repo bootstrap: modules, convention plugins, catalog, CI, Supabase init | 🟨 feature/ET-001-bootstrap |
| 1 | ET-002 | Auth, profiles, role onboarding | ⬜ |
| 2 | ET-003, ET-004 | Sync engine · linking + coach hub (+ post-MVP schemas) | ⬜ |
| 3 | ET-005 | Exercise library + program builder | ⬜ |
| 4 | ET-006 | Trainee plan view + workout logger + coach log view | ⬜ |
| 5 | ET-007 | Targets, overview, progress charts | ⬜ |
| 6 | ET-008, ET-009 | Sessions + calendars · push notifications | ⬜ |
| 7 | ET-010 | Chat with attachments | ⬜ |
| 8 | ET-011 | Release hardening, signed AAB, Play internal track | ⬜ |

Legend: ⬜ not started · 🟨 in progress (branch name) · ✅ merged (PR #)

---

## 3. Android Modules

Kotlin sources live in `src/main/kotlin`. Every module below exists with a placeholder object so
later phases only add code; `app` and `core/designsystem` are the two that already hold real code.

### `app` — `com.easytrain.app`, applicationId `com.easytrain`
| File | What it does |
|---|---|
| `EasyTrainApp.kt` | `@HiltAndroidApp` application |
| `MainActivity.kt` | Single activity: splash screen API, edge-to-edge, `EasyTrainTheme`, `EasyTrainNavHost` |
| `navigation/EasyTrainNavHost.kt` | `NavHost` with the single `AuthRoute` destination |
| `navigation/EasyTrainRoutes.kt` | `@Serializable data object AuthRoute` (type-safe routes) |
| `ui/AuthPlaceholderScreen.kt` | Themed placeholder + light/dark previews; replaced by ET-002 |
| `src/test/.../AuthPlaceholderScreenTest.kt` | Robolectric + Compose smoke test |
| `res/xml/network_security_config.xml` | Cleartext blocked; the debug variant allows `10.0.2.2` only |

`BuildConfig.SUPABASE_URL` / `SUPABASE_PUBLISHABLE_KEY` are generated here from `local.properties`
(then the environment, for CI) by `configureSupabaseBuildConfig` in build-logic; a missing value
fails the build. **ET-002 follow-up:** `core/network` cannot read `app`'s `BuildConfig`, so the
values are handed to `SupabaseClientProvider` through a Hilt module in `app`.

### `core/*`
| Module | Namespace | Plugins | Contents |
|---|---|---|---|
| `core/common` | `com.easytrain.core.common` | library | placeholder |
| `core/model` | `com.easytrain.core.model` | library | placeholder |
| `core/database` | `com.easytrain.core.database` | library + room | placeholder; Room + KSP wired, `schemas/` exported |
| `core/network` | `com.easytrain.core.network` | library | placeholder |
| `core/data` | `com.easytrain.core.data` | library | placeholder |
| `core/sync` | `com.easytrain.core.sync` | library | placeholder |
| `core/notifications` | `com.easytrain.core.notifications` | library | placeholder |
| `core/designsystem` | `com.easytrain.core.designsystem` | library.compose | `theme/Color.kt`, `theme/Tokens.kt` (Spacing, TouchTarget, Shapes), `theme/Theme.kt` (`EasyTrainTheme`, dynamic color off) |
| `core/ui` | `com.easytrain.core.ui` | library.compose | placeholder; depends on `core:designsystem` |
| `core/testing` | `com.easytrain.core.testing` | library | placeholder |

### `feature/*`
All eight apply `easytrain.android.feature` (= library.compose + hilt + serialization + the shared
UI/nav/test dependencies) and contain a placeholder: `onboarding`, `coach/hub`, `coach/trainee`,
`plans`, `workout`, `sessions`, `chat`, `profile` — namespaces `com.easytrain.feature.<path>`.

### `build-logic/convention`
| Plugin id | Class | What it configures |
|---|---|---|
| `easytrain.android.application` | `AndroidApplicationConventionPlugin` | `com.android.application` + ktlint, common Android config, Compose, Supabase BuildConfig, `targetSdk`, R8 on release |
| `easytrain.android.library` | `AndroidLibraryConventionPlugin` | `com.android.library` + ktlint + common Android config |
| `easytrain.android.library.compose` | `AndroidLibraryComposeConventionPlugin` | the above + Compose compiler plugin, BOM and UI dependencies |
| `easytrain.android.feature` | `AndroidFeatureConventionPlugin` | library.compose + hilt + serialization + `core:model`/`designsystem`/`ui`, nav, lifecycle, Robolectric |
| `easytrain.android.room` | `AndroidRoomConventionPlugin` | `androidx.room` + KSP, `schemaDirectory`, Room dependencies |
| `easytrain.hilt` | `HiltConventionPlugin` | KSP + Hilt plugin and dependencies |

Shared helpers: `AndroidCommon.kt` (compileSdk 36 / minSdk 26 / Java 17 / unit-test options),
`AndroidCompose.kt`, `SupabaseBuildConfig.kt`, `VersionCatalog.kt`.

---

## 4. Supabase Schema

One row per table as migrations land. Template:

| Table | Migration | Writer | Policies (summary) | Realtime | pgTAP file |
|---|---|---|---|---|---|
| _none yet — the first migration is ET-002's `create_profiles`_ | | | | | |

Workspace created by ET-001: `supabase/config.toml` (project_id `easytrain`, Postgres 17, email
confirmations off locally, site URL `easytrain://auth-callback`), empty `migrations/`, idempotent
`seed.sql`, and `tests/smoke.test.sql` proving `supabase test db` runs.

### Functions / triggers / RPCs
| Name | Kind | Migration | Purpose |
|---|---|---|---|
| _none yet_ | | | |

### Edge functions
| Function | Trigger | Secrets required | Status |
|---|---|---|---|
| `send-push` | DB webhook on `notifications` INSERT | `WEBHOOK_SECRET`, `FCM_SERVICE_ACCOUNT_JSON`, `SUPABASE_SERVICE_ROLE_KEY` | planned (ET-009) |

### Manual dashboard steps (hosted project)
Record here every step that cannot be expressed as a migration (enable Realtime, create the
webhook, set function secrets, Storage bucket creation if not scripted, pg_cron enable).

| Date | Step | Status |
|---|---|---|
| 2026-09-12 | Create the hosted project in the Supabase dashboard, then run `supabase link --project-ref <ref>` (on the destructive list — the user runs it) | ⬜ pending; record the project ref here |

ET-001 targets the local stack only. Nothing has been pushed to a hosted project.

---

## 5. Sync Engine

_Not built yet (ET-003)._ When built, document: registered tables in push/pull order, cursor
storage, worker names, realtime channel naming, upload queue (ET-010).

---

## 6. CI/CD

| Workflow | Trigger | Jobs | Status |
|---|---|---|---|
| `ci.yml` | PR / push to master | changes · lint · unit-test · build · db-test (only when `supabase/**` changed) · ci-ok | built (ET-001) |
| `release.yml` | tag `v*.*.*` | signed `bundleRelease` → GitHub Release | planned (ET-011) |

Composite actions: `.github/actions/gradle-setup` (JDK 17 temurin + `gradle/actions/setup-gradle`,
cache read-only off master). `supabase-setup` was not needed — `db-test` uses `supabase/setup-cli`
directly. Every `uses:` is pinned to a full commit SHA resolved from the GitHub API in the ET-001
session; `ci-ok` is the job to require on master.

Required secrets (document when added): `SUPABASE_URL`, `SUPABASE_PUBLISHABLE_KEY` (CI dummy
values acceptable), `GOOGLE_SERVICES_JSON_B64`, `EASYTRAIN_KEYSTORE_B64`, `EASYTRAIN_KEYSTORE_PASSWORD`,
`EASYTRAIN_KEY_ALIAS`, `EASYTRAIN_KEY_PASSWORD`.

None are set yet: `ci.yml` hard-codes dummy Supabase values in `env:` because CI only compiles the
app, and the repository is public — no real key may ever be committed or echoed.

---

## 7. Local Development

```
supabase start            # Docker stack; copy URL/key from `supabase status` into local.properties (URL as http://10.0.2.2:54321 for the emulator)
./gradlew :app:installDebug
supabase db reset         # re-apply migrations + seed (dev users coach@dev.local / trainee@dev.local, password "password")
supabase test db          # RLS tests
```

---

## 8. Release History

_No releases yet._ Format: `## vX.Y.Z — YYYY-MM-DD` followed by merged tickets.

### Release checklist (manual smoke, run on a physical device before every Play upload)
1. Fresh install → sign up as coach → invite code shown.
2. Second device → sign up as trainee → enter code → appears on coach hub without restart.
3. Coach assigns a 2-day program → trainee's Plan shows it.
4. Trainee, airplane mode: start today's workout, log 3 sets, finish, kill app. Reconnect → coach Log tab shows it.
5. Coach schedules a session tomorrow → trainee gets a push; tapping opens it.
6. Trainee sends a photo in chat → coach sees it and gets a push.
7. Sign out on both → Room empty (App Inspection), no crash on next sign-in.

---

## 9. Open Questions / Decisions Log

| Date | Question | Decision |
|---|---|---|
| 2026-09-12 | Backend | Supabase (Postgres + RLS) over Firebase / custom server — relational fit, RLS, Postgres familiarity |
| 2026-09-12 | Stack | Native Kotlin + Compose (no KMP/Flutter) |
| 2026-09-12 | MVP scope | Plans + logging sync (core), targets, sessions/calendar, chat, push. Body metrics + nutrition: schema in ET-003, UI post-MVP |
| 2026-09-12 | Roles | One role per account, immutable; multi-role deferred |
| 2026-09-12 | Repo visibility | Public GitHub repo (branch protection is free) — secret hygiene is therefore mandatory, not advisory |
| 2026-09-12 | Kotlin plugin | AGP 9 enables built-in Kotlin by default, so `org.jetbrains.kotlin.android` is **not** applied anywhere; Kotlin's jvmTarget follows `compileOptions.targetCompatibility` (Java 17) |
| 2026-09-12 | KSP versioning | KSP moved to standalone semver (2.3.12) and is no longer `<kotlin>-<ksp>`; the `/resolve-versions` rule for KSP is stale and was corrected in that skill |
| 2026-09-12 | Supabase BuildConfig | Generated in `:app` only; `core/network` receives the values via Hilt (a library module cannot read the app's `BuildConfig`) |
| 2026-09-12 | compileSdk | Raised to 37 (targetSdk stays 36, minSdk 26): Compose 1.12 / BOM 2026.09.00 refuses to be consumed below 37. `context.md`'s Tech Stack row was corrected in the same PR after confirming with the user |
| 2026-09-12 | ktlint + Compose | `.editorconfig` sets `ktlint_function_naming_ignore_when_annotated_with = Composable` so PascalCase composables pass `ktlintCheck` |
| 2026-09-12 | Release signing | Not wired in ET-001 (`standards.md §7` env-based signing lands with ET-011, which is where `release.yml` appears) |
