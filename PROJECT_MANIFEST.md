# EasyTrain — Project Manifest

> Snapshot of everything built so far, section by section, plus the status of every roadmap
> phase. Claude updates this file in the same PR as any change it describes.
> Last updated: 2026-09-12, project bootstrapped — no code yet; ET-001 is next.

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
| 0 | ET-001 | Repo bootstrap: modules, convention plugins, catalog, CI, Supabase init | ⬜ not started |
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

Fill in one subsection per module as it is created. Template:

### `app`
| File | What it does |
|---|---|
| `EasyTrainApp.kt` | — |
| `MainActivity.kt` | — |
| `EasyTrainNavHost.kt` | — |

### `core/common` · `core/model` · `core/database` · `core/network` · `core/data` · `core/sync` · `core/notifications` · `core/designsystem` · `core/ui` · `core/testing`
_Not created yet._

### `feature/onboarding` · `feature/coach/hub` · `feature/coach/trainee` · `feature/plans` · `feature/workout` · `feature/sessions` · `feature/chat` · `feature/profile`
_Not created yet._

### `build-logic/convention`
_Not created yet._ Planned plugins: `easytrain.android.application`, `easytrain.android.library`,
`easytrain.android.library.compose`, `easytrain.android.feature`, `easytrain.hilt`, `easytrain.android.room`.

---

## 4. Supabase Schema

One row per table as migrations land. Template:

| Table | Migration | Writer | Policies (summary) | Realtime | pgTAP file |
|---|---|---|---|---|---|
| _none yet_ | | | | | |

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

---

## 5. Sync Engine

_Not built yet (ET-003)._ When built, document: registered tables in push/pull order, cursor
storage, worker names, realtime channel naming, upload queue (ET-010).

---

## 6. CI/CD

| Workflow | Trigger | Jobs | Status |
|---|---|---|---|
| `ci.yml` | PR / push to master | lint · unit-test · build · db-test (conditional) · ci-ok | planned (ET-001) |
| `release.yml` | tag `v*.*.*` | signed `bundleRelease` → GitHub Release | planned (ET-011) |

Composite actions: `.github/actions/gradle-setup`, `.github/actions/supabase-setup` (planned).

Required secrets (document when added): `SUPABASE_URL`, `SUPABASE_PUBLISHABLE_KEY` (CI dummy
values acceptable), `GOOGLE_SERVICES_JSON_B64`, `EASYTRAIN_KEYSTORE_B64`, `EASYTRAIN_KEYSTORE_PASSWORD`,
`EASYTRAIN_KEY_ALIAS`, `EASYTRAIN_KEY_PASSWORD`.

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
