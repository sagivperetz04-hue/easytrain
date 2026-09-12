# Project Context

## What This Project Is

**EasyTrain** is an Android app for personal trainers and their clients.

Two roles, chosen once at first launch:

- **Coach** — sees a hub of all their trainees. Opens any trainee to see their profile, targets,
  assigned training plan, logged workouts, scheduled sessions and a chat thread. Builds plans,
  schedules sessions, sets targets.
- **Trainee** — sees *today*: the workout the coach assigned, the next session, progress toward
  targets. Logs each set (weight × reps) as they train; the log appears on the coach's side within
  seconds when online, or as soon as the phone reconnects.

Everything the two sides share (plans, logs, sessions, targets, messages) syncs both ways through
Supabase. Body-weight/measurements and diet are in the data model from day one but their UI is a
post-MVP phase (see `roadmap.md`).

The app is a real product, not a DevOps vehicle: correctness of the sync and a fast, reliable
logging experience in the gym (often with poor signal) are the primary goals.

---

## Architecture

Native Android (Kotlin + Jetpack Compose), **offline-first**, with Supabase as the backend.
There is no custom API server: the app talks to Supabase (PostgREST, Auth, Realtime, Storage)
directly, and **Row-Level Security in Postgres is the authorization layer**. Server-side logic that
must not run on the client (invite acceptance, notification fan-out, push delivery) lives in SQL
functions/triggers and Edge Functions.

```
┌──────────────────────────── Android (single APK, role-aware UI) ────────────────────────────┐
│  feature/*  Composables ──events──▶ ViewModel ──▶ Repository (core/data) ◀──Flow── Room DB  │
│                                                       │                          ▲          │
│                                                       │ write-through            │ SSOT     │
│                                                       ▼                          │          │
│                                              core/sync SyncEngine ───────────────┘          │
│                                                 │ push pending rows (PostgREST upsert)      │
│                                                 │ pull deltas (updated_at > cursor)         │
│                                                 │ realtime "poke" → pull                    │
└─────────────────────────────────────────────────┼───────────────────────────────────────────┘
                                                  ▼
┌──────────────────────────────────── Supabase project ─────────────────────────────────────┐
│  Auth (email+password)   PostgREST ──▶ Postgres (RLS on every table, updated_at triggers)  │
│  Realtime (postgres_changes, filtered per user)   Storage (chat-media bucket, RLS)         │
│  Edge Function send-push ◀── DB webhook on notifications INSERT ──▶ FCM HTTP v1 ──▶ device │
│  pg_cron: session reminders → notifications                                                │
└───────────────────────────────────────────────────────────────────────────────────────────┘
```

### Layers (per Android's recommended architecture)

| Layer | Module(s) | Rules |
|---|---|---|
| UI | `feature/*`, `core/designsystem`, `core/ui` | Composables are stateless; a `ViewModel` per screen exposes one `StateFlow<UiState>` and takes events via functions. No Supabase or Room types here. |
| Data | `core/data` (repositories), `core/database` (Room), `core/network` (Supabase DTOs + remote data sources) | Repositories are the only public API of the data layer. They read from Room (`Flow`), write to Room, and hand sync to `core/sync`. One repository per aggregate (`ProgramRepository`, `WorkoutRepository`, …). |
| Sync | `core/sync` | `SyncEngine` + `SyncWorker` (WorkManager). Generic over `SyncedTable` registrations; features never write sync code. |
| Domain | `core/model` | Pure Kotlin data classes and enums shared by all layers. No Android imports. Use-case classes only where logic is reused by ≥2 ViewModels (e.g. `ComputeAdherenceUseCase`). |

**Three model types per table** (Now-in-Android pattern): `XxxDto` (`@Serializable`, `core/network`)
↔ `XxxEntity` (`@Entity`, `core/database`) ↔ `Xxx` (domain, `core/model`), with explicit mapper
functions. Feature modules see only domain models.

### Module graph

```
app ──▶ feature:* ──▶ core:data ──▶ core:database, core:network, core:sync
                 └──▶ core:designsystem, core:ui, core:model, core:common
core:sync ──▶ core:database, core:network
core:notifications (FCM service, token registration) ──▶ core:data
core:testing (fakes, rules, test dispatchers) — testImplementation only
build-logic/convention — `easytrain.android.application`, `.android.library`, `.android.feature`,
                          `.android.library.compose`, `.hilt`, `.android.room`
```

Feature modules never depend on each other; cross-feature navigation goes through typed routes in
`app`.

---

## Roles & Identity

- One Supabase Auth user = one `profiles` row = **exactly one role** (`coach` | `trainee`), chosen at
  onboarding and immutable afterwards (a second account is needed to switch — MVP simplification).
- A trainee is linked to **one coach**; a coach has many trainees (`coach_trainees`).
- Linking: coach generates an **invite code** (8 chars, 7-day expiry) → trainee enters it →
  `accept_invite(code)` RPC (SECURITY DEFINER) creates the link. Trainees can also use the app
  unlinked (self-coached: no plan, but can log ad-hoc workouts).
- The role is duplicated into the JWT (`app_metadata.role`) by a trigger on `profiles` so RLS can
  check it without a table lookup.

---

## Data Model

`supabase/migrations/*.sql` is the source of truth. Every synced table has the **sync columns**:

```
id          uuid primary key            -- generated on the client (kotlin.uuid.Uuid.random())
created_at  timestamptz not null default now()
updated_at  timestamptz not null default now()   -- set by trigger set_updated_at() on every write
deleted_at  timestamptz null            -- soft delete; rows are never hard-deleted by the app
```

and an index on `(owner column, updated_at)` for delta pulls.

| Table | Owner / writer | Key columns | Notes |
|---|---|---|---|
| `profiles` | self | `id` (= auth.users.id), `role`, `display_name`, `avatar_path`, `units` (`kg`/`lb`), `timezone` | Created by trigger on `auth.users` insert. `role` is settable once (null → value). |
| `coach_trainees` | RPC only | `coach_id`, `trainee_id`, `status` (`active`/`ended`), `started_at` | Unique `(coach_id, trainee_id)`. Read by both sides. |
| `invites` | coach | `code` (unique), `coach_id`, `expires_at`, `used_by`, `used_at` | Codes generated by `create_invite()` RPC. |
| `exercises` | coach (custom) / seed (global) | `owner_id` (null = global), `name`, `muscle_group`, `equipment`, `video_url`, `instructions` | ~150 global rows in `seed.sql`. Trainees see global + their coach's custom ones. |
| `programs` | coach | `coach_id`, `trainee_id` (nullable = template), `name`, `description`, `start_date`, `weeks`, `status` (`draft`/`active`/`archived`) | One `active` program per trainee (partial unique index). |
| `program_days` | coach | `program_id`, `coach_id`, `trainee_id` (both denormalized from the program), `day_index` (1..7, weekday), `name`, `notes` | e.g. Mon = "Push A". |
| `program_exercises` | coach | `program_day_id`, `coach_id`, `trainee_id` (denormalized), `exercise_id`, `order_index`, `target_sets`, `target_reps_min`, `target_reps_max`, `target_weight_kg`, `target_rpe`, `rest_seconds`, `notes` | Targets are what the trainee sees while logging. |
| `workouts` | trainee | `trainee_id`, `coach_id` (denormalized for RLS/realtime; **nullable** — unlinked trainees log ad-hoc), `program_day_id` (nullable = ad-hoc), `started_at`, `completed_at`, `notes`, `status` (`in_progress`/`completed`/`skipped`) | |
| `workout_sets` | trainee | `workout_id`, `trainee_id`, `coach_id` (nullable, denormalized), `exercise_id`, `program_exercise_id` (nullable), `set_index`, `reps`, `weight_kg`, `rpe`, `completed_at` | Weight always stored in kg; unit conversion is a UI concern. |
| `targets` | coach | `coach_id`, `trainee_id`, `kind` (`bodyweight`/`lift_1rm`/`weekly_workouts`/`custom`), `exercise_id` (for `lift_1rm`), `value`, `unit`, `due_date`, `status` (`open`/`achieved`/`dropped`) | Progress toward `lift_1rm` uses Epley `w × (1 + reps/30)` over `workout_sets`. |
| `sessions` | coach | `coach_id`, `trainee_id`, `starts_at`, `ends_at`, `kind` (`in_person`/`online`), `location`, `notes`, `status` (`scheduled`/`completed`/`cancelled`) | Reminder 60 min before via pg_cron → `notifications`. |
| `conversations` | RPC / trigger | `coach_id`, `trainee_id`, `last_message_at` | One per link, created by the same trigger that creates `coach_trainees`. |
| `messages` | either participant | `conversation_id`, `sender_id`, `body`, `attachment_path`, `attachment_kind` (`image`/`video`), `read_at` | Attachments in Storage bucket `chat-media/<conversation_id>/<uuid>`. |
| `body_metrics` | trainee | `trainee_id`, `coach_id`, `measured_on` (date), `weight_kg`, `body_fat_pct`, `waist_cm`, `notes` | Schema in Phase 2; UI post-MVP. |
| `nutrition_targets` | coach | `coach_id`, `trainee_id`, `calories`, `protein_g`, `carbs_g`, `fat_g`, `notes`, `active_from` | Schema in Phase 2; UI post-MVP. |
| `nutrition_logs` | trainee | `trainee_id`, `coach_id`, `logged_on`, `calories`, `protein_g`, `carbs_g`, `fat_g`, `adherence` (1-5), `notes` | Schema in Phase 2; UI post-MVP. |
| `device_tokens` | self | `user_id`, `fcm_token` (unique), `platform`, `last_seen_at` | Not synced to Room; written directly on token refresh. |
| `notifications` | triggers only | `recipient_id`, `kind`, `title`, `body`, `data` (jsonb deep-link payload), `read_at` | INSERT fires the `send-push` webhook. Pulled to Room for the in-app inbox. |

**Single-writer rule:** each table has exactly one writing role (see "Owner"). This is what makes
last-write-wins conflict resolution safe: the two sides never edit the same row. The only shared
write is `messages.read_at` (recipient marks read) — acceptable, idempotent.

**Denormalization rule:** child tables (`program_days`, `program_exercises`, `workout_sets`) carry
the same owner columns as their parent so that RLS policies, delta-pull indexes and realtime
filters never need a join. The client copies them from the parent on insert; a `check`-style
trigger `assert_parent_owner()` rejects mismatches. `coach_id` is nullable on trainee-owned tables
(`workouts`, `workout_sets`, `body_metrics`, `nutrition_logs`) because an unlinked trainee can log;
when a link is created later, `accept_invite()` backfills `coach_id` on that trainee's rows.

### RLS summary

Helper functions (SQL, `SECURITY DEFINER`, `STABLE`), all indexed on `coach_trainees`:

```sql
auth_role()                         -- (auth.jwt() -> 'app_metadata' ->> 'role')
is_coach_of(p_trainee uuid)         -- exists coach_trainees where coach_id = auth.uid() and trainee_id = p_trainee and status = 'active'
is_trainee_of(p_coach uuid)         -- the mirror
my_coach_ids()                      -- set of coach_id for the current trainee
```

| Table | SELECT | INSERT / UPDATE (`WITH CHECK` mirrors `USING`) |
|---|---|---|
| `profiles` | own row, or linked coach/trainee | own row; `role` may change only from null |
| `coach_trainees` | `coach_id = auth.uid() or trainee_id = auth.uid()` | none (RPC only); coach may UPDATE `status` to `ended` |
| `invites` | `coach_id = auth.uid()` | via `create_invite()` RPC |
| `exercises` | `owner_id is null or owner_id = auth.uid() or owner_id in (my_coach_ids())` | `owner_id = auth.uid() and auth_role() = 'coach'` |
| `programs`, `program_days`, `program_exercises` | coach: `coach_id = auth.uid()`; trainee: `trainee_id = auth.uid()` (child tables join via parent — use a `program_coach_id(program_id)` helper or denormalize `coach_id`/`trainee_id` on children; **we denormalize**) | `coach_id = auth.uid() and auth_role() = 'coach'` |
| `workouts`, `workout_sets` | `trainee_id = auth.uid() or is_coach_of(trainee_id)` | `trainee_id = auth.uid()` |
| `targets`, `sessions`, `nutrition_targets` | `coach_id = auth.uid() or trainee_id = auth.uid()` | `coach_id = auth.uid() and auth_role() = 'coach'` |
| `body_metrics`, `nutrition_logs` | `trainee_id = auth.uid() or is_coach_of(trainee_id)` | `trainee_id = auth.uid()` |
| `conversations` | participant | none (trigger) |
| `messages` | participant of the conversation (`conversation_participant(conversation_id)`) | INSERT: participant and `sender_id = auth.uid()`; UPDATE: recipient may set `read_at` only |
| `device_tokens` | `user_id = auth.uid()` | `user_id = auth.uid()` |
| `notifications` | `recipient_id = auth.uid()` | UPDATE `read_at` only |
| Storage `chat-media` | path prefix = a conversation the user participates in | same, INSERT only |

Every table: `alter table … enable row level security;` in the same migration that creates it.
Every policy has a pgTAP test in `supabase/tests/` proving both the allow and the deny case.

---

## Sync Contract (core/sync)

Room is the on-device source of truth. The UI never waits on the network.

**Local entity extras** (every synced `@Entity`): `syncState` (`SYNCED` | `PENDING` | `FAILED`),
`localUpdatedAt` (epoch ms), plus the server's `updatedAt`/`deletedAt`.

**Write path** (`Repository.save(x)`):
1. Upsert into Room with `syncState = PENDING`, `localUpdatedAt = now`. UI updates instantly via `Flow`.
2. Enqueue `SyncWorker` (unique, `ExistingWorkPolicy.KEEP`, network-connected constraint, expedited).

**Push** (`SyncEngine.push()`), per registered table in dependency order (parents before children):
- Select rows `syncState = PENDING`, map to DTOs, PostgREST `upsert(onConflict = "id")` in batches of ≤200.
- Success → mark `SYNCED` with the returned `updated_at`. Failure → `FAILED` + exponential backoff (WorkManager `Result.retry()`); 4xx from RLS is logged and surfaced as a non-blocking error banner, never retried forever.

**Pull** (`SyncEngine.pull()`), per table:
- `select * where updated_at > :cursor order by updated_at limit 500` (RLS scopes rows to the user); loop until a short page.
- Upsert into Room **except** rows whose local `syncState = PENDING` (local wins until pushed; the next pull reconciles).
- `deleted_at != null` → keep the tombstone in Room (queries filter `deletedAt IS NULL`); purge tombstones older than 30 days.
- Cursor = max server `updated_at` seen, stored in `sync_cursors(table_name, cursor)`. Server time only — never the device clock.

**Realtime**: one channel per user with `postgresChangeFlow<PostgresAction>` on each synced table,
filtered by the user's column (`trainee_id=eq.<uid>` or `coach_id=eq.<uid>`). Events are a
**poke** — the payload is ignored; the engine enqueues a pull for that table. This keeps a single
code path for applying server data. Reconnect → full pull.

**Triggers to sync**: app foreground, connectivity regained, every 15 min periodic, after each
local write, on realtime poke, pull-to-refresh.

**Conflicts**: last-write-wins by server `updated_at`. Safe because of the single-writer rule.
The engine still must never lose a `PENDING` local row.

**Auth in sync**: supabase-kt `Auth` plugin persists the session and refreshes tokens; the engine
checks `auth.sessionStatus` and skips (not fails) when signed out. Sign-out clears Room entirely.

---

## Navigation & Screens

Type-safe routes (`@Serializable` route objects) with `androidx.navigation:navigation-compose`.
`app` decides the start destination: no session → `Auth`; session but `role == null` → `ChooseRole`;
else the role's home graph.

**Onboarding (both roles)**
- `SignIn` / `SignUp` (email + password; forgot password via Supabase reset email)
- `ChooseRole` — two large cards: "I'm a coach" / "I'm a trainee"
- `EnterInviteCode` (trainee; skippable) · `Profile setup` (name, units, timezone auto)

**Coach** — bottom nav: **Trainees · Calendar · Messages · Profile**
- `CoachHub` — trainee cards: name, adherence this week (done/planned), last workout, next session, unread badge; search; "Invite trainee" → code + share sheet
- `TraineeDetail(traineeId)` — tabs: **Overview** (targets with progress, this week's plan adherence, latest body metrics), **Plan** (active program; edit / assign), **Log** (workouts list → `WorkoutDetail`), **Sessions** (list + add), **Chat**
- `ProgramEditor(programId)` — days → exercises (reorder, targets); `ExercisePicker` (search global + custom, create custom)
- `SessionEditor(sessionId?)` — date/time, kind, location, notes
- `TargetEditor(targetId?)`
- `CoachCalendar` — month grid + day agenda across all trainees
- `Messages` — conversation list → `Chat(conversationId)`

**Trainee** — bottom nav: **Today · Plan · Calendar · Messages · Profile**
- `Today` — today's workout card (start/continue), next session, targets progress, "recent workouts"
- `WorkoutLogger(workoutId)` — exercise list; per exercise a set table with target vs actual; tap to complete a set (auto-fills previous values); rest timer; finish → summary. Must work fully offline.
- `Plan` — week view of the active program; tap a day to preview
- `History` → `WorkoutDetail(workoutId)`
- `TraineeCalendar` — sessions
- `Chat(conversationId)`
- `Profile` — units, coach link status, sign out

**Shared**: `NotificationInbox`, `Settings`.

Push notification taps deep-link via `notifications.data` (`{"route":"chat","conversationId":…}`).

---

## Tech Stack Decisions

Versions below are what was current when this file was written (Sep 2026). **Always resolve the
actual latest stable via `/resolve-versions` before writing `libs.versions.toml`.**

| Layer | Choice | Version (Sep 2026) | Status |
|---|---|---|---|
| Language | Kotlin (K2), Coroutines + Flow, kotlinx-serialization | 2.4.x | Phase 0 |
| Build | AGP + Gradle + JDK 17, version catalog, convention plugins in `build-logic/` | AGP 9.4.x / Gradle 9.6 | Phase 0 |
| SDK | `minSdk 26`, `targetSdk 36`, `compileSdk 36` | | Phase 0 |
| UI | Jetpack Compose (BOM) + Material 3, Compose Compiler Gradle plugin | BOM 2026.08.00 | Phase 0 |
| Navigation | `navigation-compose` with `@Serializable` routes | 2.9.x | Phase 0 |
| DI | Hilt (KSP) | 2.5x | Phase 0 |
| Local DB | Room (KSP) | 2.8.x | Phase 2 |
| Background | WorkManager (`CoroutineWorker`, `HiltWorker`) | 2.10.x | Phase 2 |
| Backend SDK | supabase-kt (`auth-kt`, `postgrest-kt`, `realtime-kt`, `storage-kt`, `functions-kt`) via BOM; Ktor client engine `ktor-client-okhttp` | 3.8.x | Phase 1+ |
| Images | Coil 3 (Compose) — exercise thumbnails, avatars, chat media | 3.x | Phase 3 |
| Push | Firebase Cloud Messaging (only `firebase-messaging`; no other Firebase product) | BOM latest | Phase 6 |
| Backend | Supabase: Postgres 17, Auth, PostgREST, Realtime, Storage, Edge Functions (Deno/TypeScript), pg_cron | hosted + local via Supabase CLI | Phase 0+ |
| DB tests | pgTAP via `supabase test db` | | Phase 1+ |
| App tests | JUnit4, `kotlinx-coroutines-test`, Turbine, MockK-free (hand-written fakes in `core/testing`), Robolectric for Compose UI tests, Room in-memory | | Phase 0+ |
| Lint | ktlint (Gradle plugin) + Android Lint | | Phase 0 |
| CI | GitHub Actions (SHA-pinned), Gradle cache, Supabase CLI action for `supabase test db` | | Phase 0 |
| Release | signed AAB from `vX.Y.Z` tags, keystore + passwords in GitHub secrets; Play Console internal track manual until Phase 8 | | Phase 8 |
| Charts | Compose `Canvas` (no charting library) | | Phase 5 |
| Crash reporting | none in MVP (candidate: Firebase Crashlytics — needs approval) | | Post-MVP |

**Explicitly not used**: Firestore, custom REST server, KMP/Flutter, RxJava, LiveData, Fragments,
XML layouts, kapt, Realm, Retrofit (supabase-kt covers all network calls).

---

## Repository Layout

```
easytrain/
  app/src/main/kotlin/com/easytrain/app/         # EasyTrainApp, MainActivity, EasyTrainNavHost, role graphs
  core/
    common/        # Dispatchers qualifiers, Result/AppError, Clock, extensions
    model/         # domain models + enums (pure Kotlin)
    database/      # EasyTrainDatabase, entities, DAOs, migrations, sync_cursors
    network/       # SupabaseClient provider, DTOs, remote data sources, auth session bridge
    data/          # repositories (offline-first), mappers, SyncedTable registrations
    sync/          # SyncEngine, SyncWorker, RealtimePoker, ConnectivityObserver
    notifications/ # EasyTrainMessagingService, token registration, channels, deep-link parsing
    designsystem/  # Theme, tokens, typography, shared components (SetRow, RestTimer, AdherenceRing …)
    ui/            # screen-level shared composables (EmptyState, ErrorBanner, LoadingScaffold)
    testing/       # fakes, MainDispatcherRule, test data builders
  feature/
    onboarding/    # SignIn, SignUp, ChooseRole, EnterInviteCode, ProfileSetup
    coach/hub/     # CoachHub, InviteTrainee
    coach/trainee/ # TraineeDetail tabs (overview, plan, log, sessions) + editors for targets/sessions
    plans/         # ProgramEditor, ExercisePicker (coach) · Plan week view (trainee)
    workout/       # Today, WorkoutLogger, History, WorkoutDetail
    sessions/      # CoachCalendar, TraineeCalendar, SessionEditor
    chat/          # Messages list, Chat thread, attachment picker
    profile/       # Profile, Settings, NotificationInbox
  build-logic/convention/   # Gradle convention plugins
  gradle/libs.versions.toml
  supabase/
    config.toml
    migrations/    # 20260912000000_init.sql … (never edit an applied file)
    seed.sql       # global exercise library + local dev users
    functions/send-push/index.ts
    tests/         # *.sql pgTAP: one file per table's RLS
  .github/workflows/ci.yml, release.yml
  .github/actions/           # composite actions (gradle-setup, supabase-setup)
  .claude/                   # context.md (this), standards.md, roadmap.md, SKILLS.md, prompts/, skills/
  CLAUDE.md  PROJECT_MANIFEST.md
```

Local development: Android Studio + Docker Desktop (for `supabase start`). The emulator reaches
the local stack at `http://10.0.2.2:54321`; `local.properties` holds the URL and publishable key
for the local stack (the `.gitignore` already excludes it).
