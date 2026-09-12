# Roadmap — Phased Build Instructions

How to use this file: each phase is one or more `ET-NNN` tickets. Start a phase by pasting its
**Prompt** into Claude Code (it already tells Claude to read `CLAUDE.md`, `context.md` and
`standards.md`). A phase is done when every line of its **Done when** list is true, the PR is
merged, and `PROJECT_MANIFEST.md` reflects it. Do not start the next phase on the same branch.

Phases are ordered by dependency, not by importance. The sync engine (Phase 2) comes before any
feature that stores data so that no feature is ever written "online-only and fixed later".

Ticket numbering: `ET-001` … Branch: `feature/ET-NNN-short-description`.

---

## Phase 0 — Repository bootstrap (ET-001)

**Goal:** an empty-but-complete skeleton that builds, lints, tests and deploys nothing yet.

**Deliverables**
- Gradle multi-module layout from `context.md` → *Repository Layout*, with `build-logic/convention`
  plugins: `easytrain.android.application`, `easytrain.android.library`,
  `easytrain.android.library.compose`, `easytrain.android.feature`, `easytrain.hilt`,
  `easytrain.android.room`. Empty `core/*` and `feature/*` modules exist with a placeholder
  Kotlin file each, so later phases only add code.
- `gradle/libs.versions.toml` with **live-resolved** versions (Kotlin, AGP, Compose BOM, Compose
  compiler plugin, Hilt, KSP, Room, WorkManager, Navigation, supabase-kt BOM, Ktor OkHttp engine,
  Coil, kotlinx-serialization, coroutines, Turbine, Robolectric, ktlint plugin).
- `app`: `EasyTrainApp` (`@HiltAndroidApp`), `MainActivity` (single activity, edge-to-edge,
  splash screen API), `EasyTrainNavHost` with a placeholder `Auth` destination, `EasyTrainTheme`
  from `core/designsystem` (Material 3 dynamic color off; brand palette from `/easytrain-design`).
- `BuildConfig.SUPABASE_URL` / `SUPABASE_PUBLISHABLE_KEY` wired from `local.properties`/env, with a
  clear failure when missing. `local.properties.example` committed.
- `supabase init` → `supabase/config.toml` (auth: email confirmations **off** for local, site URL
  `easytrain://auth-callback`), empty `migrations/`, `seed.sql`, `tests/` with a smoke pgTAP test.
- `.github/workflows/ci.yml` per `standards.md §8` (lint, unit-test, build, db-test, ci-ok), SHA-pinned.
  `.github/actions/gradle-setup` composite action.
- `.gitignore` (Android Studio, Gradle, `local.properties`, `*.jks`, `google-services.json`,
  `supabase/.temp`, `.env*`). `.editorconfig` for ktlint.
- `PROJECT_MANIFEST.md` filled in (sections for every module and for `supabase/`), `README.md` with
  the 10-line "how to run locally" only.

**Done when**
- `./gradlew ktlintCheck lintDebug testDebugUnitTest :app:assembleDebug` passes locally and in CI.
- `supabase start && supabase test db` passes.
- The app launches to a themed placeholder screen on an emulator.
- Every version in the catalog was printed from Maven metadata during the session (visible in the PR description).

**Prompt**
```
Read CLAUDE.md, .claude/context.md, .claude/standards.md and .claude/roadmap.md. Implement Phase 0
(ET-001) on branch feature/ET-001-bootstrap. Before writing gradle/libs.versions.toml run
/resolve-versions for every library and for every GitHub Action. Follow the module layout in
context.md exactly. Stop and ask before adding any dependency not listed in the Tech Stack table.
When done, run the full Done-when checklist, update PROJECT_MANIFEST.md, and open a PR.
```

---

## Phase 1 — Auth, profiles, role onboarding (ET-002)

**Goal:** a user can sign up, pick a role once, and land on a role-specific (still empty) home.

**Deliverables**
- Migration `create_profiles`: `profiles` table (see data model), trigger `handle_new_user()` on
  `auth.users` insert, trigger `sync_role_to_jwt()` that copies `profiles.role` into
  `auth.users.raw_app_meta_data->>'role'`, helper `auth_role()`, RLS + pgTAP tests
  (`profiles.test.sql`: role can be set once, cannot be changed, strangers can't read).
- `core/network`: `SupabaseClientProvider` (Hilt singleton; installs `Auth`, `Postgrest`,
  `Realtime`, `Storage`, `Functions`; OkHttp engine; `Auth` configured with the deep-link scheme and
  encrypted session storage on Android).
- `core/data`: `AuthRepository` (`sessionStatus: Flow<SessionState>`, `signUp`, `signIn`, `signOut`,
  `sendPasswordReset`), `ProfileRepository` (`observeMe()`, `setRole()`, `updateProfile()`).
  Profiles are the **first synced table**: `ProfileEntity`, `ProfileDao`, `ProfileDto` — but the
  sync engine arrives in Phase 2, so this phase writes through directly (`remote → local`) and is
  refactored onto the engine in Phase 2 (call this out in the manifest).
- `feature/onboarding`: `SignIn`, `SignUp`, `ForgotPassword`, `ChooseRole`, `ProfileSetup` screens
  with ViewModels + tests. `EnterInviteCode` is a stub that says "coming in ET-003".
- `app`: start-destination logic (no session → Auth; session, no role → ChooseRole; coach → `CoachHome`
  placeholder; trainee → `TraineeHome` placeholder). Deep-link intent filter for
  `easytrain://auth-callback`.
- Sign-out clears local data (even though it is just profiles for now).

**Done when**
- Fresh install → sign up → choose "coach" → coach placeholder; kill app → reopen → still coach placeholder (session persisted).
- Attempting to set the role a second time is rejected by RLS (pgTAP proves it) and the app never offers it.
- `AuthRepository`/`ProfileRepository`/all ViewModels have tests; CI green.

**Prompt**
```
Read CLAUDE.md, .claude/context.md, .claude/standards.md and .claude/roadmap.md. Implement Phase 1
(ET-002) on branch feature/ET-002-auth-and-roles. Use /add-synced-table for profiles (skip the
SyncRegistry step — the engine does not exist yet; write the remote→local read-through instead and
record the follow-up in the manifest). Use /compose-screen for every screen. Write the pgTAP tests
before the policies. Update PROJECT_MANIFEST.md and open a PR.
```

---

## Phase 2 — Sync engine + coach↔trainee linking + coach hub (ET-003, ET-004)

**Goal:** the generic offline-first machinery exists and the first real relationship flows through it.

### ET-003 — Sync engine
- `core/database`: `SyncState` enum + `SyncedEntity` interface; `sync_cursors` table.
- `core/sync`: `SyncEngine` (push/pull per `context.md` → *Sync Contract*), `SyncWorker`
  (`@HiltWorker`), `SyncScheduler` (unique expedited one-time + 15-min periodic), `ConnectivityObserver`,
  `RealtimePoker` (one channel; subscribes for each registered table; poke → `requestPull(table)`),
  `SyncInitializer` (App Startup) — start on foreground.
- `core/data`: `SyncRegistry` + `SyncedTable<Entity, Dto>` descriptor; migrate `profiles` onto it.
- Tests listed in `standards.md §6` for the engine, with a `FakeSupabaseTable` in `core/testing`.
- Migration `create_sync_helpers`: `set_updated_at()` trigger function, `assert_parent_owner()`
  trigger function (child rows must carry the parent's owner columns), `is_coach_of()`,
  `is_trainee_of()`, `my_coach_ids()`; attach `set_updated_at` to `profiles`.
- Also create the **post-MVP tables now** (`body_metrics`, `nutrition_targets`, `nutrition_logs`)
  with RLS + tests, registered in the engine but without UI — so every later phase has the full
  schema and no migration ever needs to backfill.

### ET-004 — Linking + coach hub
- Migration `create_coach_trainees_and_invites`: `coach_trainees`, `invites`, `conversations`
  (created by the link trigger), RPCs `create_invite()` and `accept_invite(p_code text)`
  (SECURITY DEFINER, `search_path = ''`; `accept_invite` also backfills `coach_id` on the
  trainee's existing `workouts` / `workout_sets` / `body_metrics` / `nutrition_logs` rows — a no-op
  until those tables exist, so write it as a separate `backfill_coach_id(p_trainee, p_coach)`
  function that later migrations extend), RLS, realtime publication, pgTAP tests.
- `core/data`: `CoachTraineeRepository` (`observeMyTrainees()` for coach, `observeMyCoach()` for
  trainee, `createInvite()`, `acceptInvite(code)` — RPC calls are online-only by nature; surface a
  clear "you're offline" error).
- `feature/coach/hub`: `CoachHub` (list of trainee cards with the fields from `context.md`; the
  adherence/last-workout/next-session values are placeholders `—` until Phases 4–6 fill them),
  `InviteTrainee` bottom sheet (code + share sheet + expiry).
- `feature/onboarding`: real `EnterInviteCode`; `feature/profile`: coach link status + "unlink".
- `app`: coach bottom nav (Trainees · Calendar · Messages · Profile) and trainee bottom nav
  (Today · Plan · Calendar · Messages · Profile) with placeholder tabs.

**Done when**
- Two emulators (coach + trainee): coach creates a code → trainee enters it → the trainee appears on the hub **without** the coach restarting the app (realtime poke → pull).
- Airplane mode on the trainee: the hub still renders from Room; on reconnect it refreshes.
- `SyncEngine` tests pass, including "pull never overwrites PENDING".

**Prompt**
```
Read CLAUDE.md, .claude/context.md (especially the Sync Contract), .claude/standards.md and
.claude/roadmap.md. Implement Phase 2 as two PRs: ET-003 (sync engine, branch
feature/ET-003-sync-engine) then ET-004 (linking + coach hub, branch feature/ET-004-linking-hub).
Do not start ET-004 until ET-003 is merged. Use /add-synced-table and /compose-screen. Ask before
deviating from the sync contract in any way.
```

---

## Phase 3 — Exercise library + program builder (ET-005)

**Goal:** a coach can build a weekly program from an exercise library and assign it to a trainee.

**Deliverables**
- Migration `create_exercises_and_programs`: `exercises`, `programs`, `program_days`,
  `program_exercises` (with denormalized `coach_id`/`trainee_id` on children), partial unique index
  "one active program per trainee", RLS, realtime, pgTAP. `seed.sql`: ~150 global exercises
  (name, muscle group, equipment) — generate from a compact Kotlin-free SQL `values` list.
- Repositories: `ExerciseRepository` (search by name/muscle, create custom), `ProgramRepository`
  (`observeProgram(id)` as an aggregate `Program` with days and exercises; `saveProgram`, `assign(traineeId)`,
  `archive`, `duplicateAsTemplate`).
- `feature/plans` (coach): `ProgramEditor` — 7-day column list; each day: name + ordered exercises;
  add/remove/reorder (drag handle); per-exercise target sheet (sets, rep range, weight, RPE, rest).
  `ExercisePicker` — search, filter chips by muscle group, "create custom exercise" inline.
- `feature/coach/trainee`: `TraineeDetail` scaffold with tabs; **Plan** tab shows the active program
  or "Assign a program" (choose template / start blank).

**Done when**
- Coach builds a 3-day program offline, comes online, trainee's device receives it (Phase 4 shows it; for now assert via Room inspector or a temporary debug screen that is removed before merge).
- Reordering and target edits survive process death (they're in Room, PENDING).
- pgTAP: trainee cannot insert into `programs`; coach A cannot read coach B's program.

**Prompt**
```
Read CLAUDE.md, .claude/context.md, .claude/standards.md and .claude/roadmap.md. Implement Phase 3
(ET-005) on branch feature/ET-005-program-builder. Use /add-synced-table for the four tables (in
one migration) and /compose-screen for ProgramEditor, ExercisePicker and the TraineeDetail scaffold.
Design the editor with /easytrain-design before coding it. Update the manifest, open a PR.
```

---

## Phase 4 — Trainee plan view + workout logger + coach log view (ET-006)

**Goal:** the core loop. Trainee sees today's workout, logs sets offline, coach sees the results live.

**Deliverables**
- Migration `create_workouts`: `workouts`, `workout_sets` (denormalized `trainee_id`, `coach_id`),
  RLS, realtime, pgTAP (coach of trainee can read, other coach cannot; trainee cannot write another trainee's sets).
- `WorkoutRepository`: `observeToday()`, `startWorkout(programDayId?)`, `observeWorkout(id)` (aggregate with sets),
  `logSet(...)`, `completeWorkout`, `observeHistory(traineeId)`, `previousSetsFor(exerciseId)` (auto-fill).
- `feature/workout`: `Today` (today's program day → start/continue; recent workouts; targets placeholder),
  `WorkoutLogger` (per exercise: target line + set rows "prev · target · actual"; tapping the check completes
  the set; weight/reps inputs with numeric keyboard and ± steppers sized for the gym; rest timer with
  notification when backgrounded; finish → summary), `History`, `WorkoutDetail`.
- `feature/plans` (trainee): `Plan` week view.
- `feature/coach/trainee`: **Log** tab (workouts list, live) → `WorkoutDetail` (read-only).
- `core/designsystem`: `SetRow`, `RestTimer`, `NumericField` components.

**Done when**
- Trainee logs a full workout in airplane mode, closes the app, reconnects → within seconds the coach's Log tab shows it with all sets.
- The logger never shows a spinner for local actions.
- Rest timer survives backgrounding and fires a notification.
- All ViewModels + repository + engine-registration tests pass; Robolectric test for the logger renders 3 exercises with sets.

**Prompt**
```
Read CLAUDE.md, .claude/context.md, .claude/standards.md and .claude/roadmap.md. Implement Phase 4
(ET-006) on branch feature/ET-006-workout-logger. The logger is the most important screen in the
app: design it first with /easytrain-design (one-handed use, sweaty fingers, poor signal), then
build with /compose-screen. Use /add-synced-table for workouts and workout_sets. Update the
manifest, open a PR.
```

---

## Phase 5 — Targets, trainee overview, progress (ET-007)

**Goal:** the coach sets targets; both sides see progress computed from real logs.

**Deliverables**
- Migration `create_targets` (RLS, realtime, pgTAP).
- `TargetRepository`; `ComputeTargetProgressUseCase` (`lift_1rm` → best Epley 1RM from `workout_sets`;
  `weekly_workouts` → completed workouts this ISO week; `bodyweight` → latest `body_metrics` (empty until post-MVP)).
- `feature/coach/trainee`: **Overview** tab — targets with progress bars, weekly adherence ring
  (`AdherenceRing` in designsystem), `TargetEditor`.
- `feature/workout` `Today`: targets progress section. `History`: per-exercise best-set line chart
  (Compose `Canvas`, no library).
- Coach hub cards now show real adherence + last workout.

**Done when**
- Setting a `lift_1rm` target of 100 kg on bench and logging 80×8 shows ~101 kg (Epley) → "achieved" suggestion on the coach side.
- Charts render with 0, 1 and 50 data points without crashing (tests).

**Prompt**
```
Read CLAUDE.md, .claude/context.md, .claude/standards.md and .claude/roadmap.md. Implement Phase 5
(ET-007) on branch feature/ET-007-targets-progress. Use /add-synced-table for targets and
/compose-screen for the Overview tab and TargetEditor. Keep charts as plain Compose Canvas. Update
the manifest, open a PR.
```

---

## Phase 6 — Scheduled sessions, calendars, push notifications (ET-008, ET-009)

**Goal:** coach schedules sessions; both see calendars; phones get pushed for what matters.

### ET-008 — Sessions + calendars
- Migration `create_sessions` (RLS, realtime, pgTAP; check `ends_at > starts_at`).
- `SessionRepository` (`observeUpcoming`, `observeMonth(yearMonth)`, `save`, `cancel`, `complete`).
- `feature/sessions`: `CoachCalendar` (month grid + day agenda across trainees, color per trainee),
  `TraineeCalendar`, `SessionEditor`; `TraineeDetail` **Sessions** tab; `Today` shows next session;
  hub cards show next session.

### ET-009 — Push notifications
- Migration `create_notifications`: `device_tokens`, `notifications`, trigger functions that insert
  notifications on: session created/updated/cancelled (→ trainee), program assigned (→ trainee),
  workout completed (→ coach), and a `pg_cron` job every 5 min inserting "session in 60 min"
  reminders (idempotent via a `reminded_at` column on `sessions`). RLS + pgTAP.
- Edge function `supabase/functions/send-push/index.ts`: verifies the webhook secret, re-reads the
  notification with the service-role client, loads the recipient's `device_tokens`, sends via FCM
  HTTP v1 (service-account JSON from `Deno.env`), prunes tokens FCM reports as unregistered.
  Database webhook on `notifications` INSERT → the function (documented as a manual dashboard step
  in the manifest; local dev uses `supabase functions serve` + a `curl` fixture).
- `core/notifications`: `EasyTrainMessagingService` (token → `device_tokens` upsert on refresh and
  on sign-in; delete on sign-out), notification channels (`sessions`, `messages`, `training`), deep-link
  from `data.route`. Firebase: only `firebase-messaging` + `google-services` plugin; `google-services.json`
  gitignored, injected in CI from a secret.
- `feature/profile`: `NotificationInbox` (synced `notifications`; mark read).

**Done when**
- Coach schedules a session → trainee phone receives a push within ~5 s; tapping opens the session.
- Session reminder arrives ~60 min before (test by inserting a session 61 min out and running the cron function manually).
- Uninstall on the trainee device → next push prunes the dead token (verified in the function log).

**Prompt**
```
Read CLAUDE.md, .claude/context.md, .claude/standards.md and .claude/roadmap.md. Implement Phase 6
as two PRs: ET-008 (feature/ET-008-sessions-calendar) then ET-009 (feature/ET-009-push). For ET-009,
stop and confirm with me before adding the Firebase Gradle plugin, and show me — do not run — the
Supabase dashboard/CLI steps for the webhook and the function secrets. Use /add-synced-table and
/compose-screen. Update the manifest, open PRs.
```

---

## Phase 7 — Chat (ET-010)

**Goal:** coach and trainee message each other, with photo/video attachments.

**Deliverables**
- Migration `create_messages`: `messages` (RLS per `context.md`; UPDATE limited to `read_at` by the
  recipient via a trigger that rejects other column changes), trigger updating
  `conversations.last_message_at`, notification trigger (→ recipient, throttled to one push per
  conversation per 2 min), realtime, pgTAP. Storage bucket `chat-media` (private) + `storage.objects`
  policies keyed on the conversation-id path prefix.
- `MessageRepository` (`observeConversations()`, `observeMessages(conversationId)` paged from Room,
  `send(text)`, `sendAttachment(uri)` → local copy + Storage upload in the sync engine's
  **upload queue** (new: `pending_uploads` table; upload before pushing the message row),
  `markRead`). Signed URLs (1 h) cached in memory for rendering.
- `feature/chat`: `Messages` list (unread counts), `Chat` thread (bubbles, day separators,
  attachment picker via Photo Picker, upload progress, retry on failure), `TraineeDetail` **Chat** tab
  reuses the thread.

**Done when**
- Message sent offline shows as "sending", syncs on reconnect; the recipient sees it live and gets a push.
- A 20 MB video attachment uploads in the background, survives app kill, and renders for the recipient via a signed URL.
- Stranger with a guessed conversation id gets nothing (pgTAP + storage policy test).

**Prompt**
```
Read CLAUDE.md, .claude/context.md, .claude/standards.md and .claude/roadmap.md. Implement Phase 7
(ET-010) on branch feature/ET-010-chat. Extend the sync engine with the upload queue exactly as
described; ask before changing the engine's public API. Use /add-synced-table and /compose-screen.
Update the manifest, open a PR.
```

---

## Phase 8 — Release hardening (ET-011)

**Goal:** a signed build a real coach can install from the Play Console internal track.

**Deliverables**
- `release.yml` per `standards.md §8`; keystore generation instructions shown to the user (not run).
- R8 rules verified: release build passes the full manual smoke script (`docs/` is not created —
  the script lives in the manifest's "Release checklist" section).
- App icon, splash, `values-iw` Hebrew strings for all modules, RTL audit.
- Baseline Profile for startup + logger scrolling (`androidx.benchmark` macrobenchmark module —
  approved addition for this phase only).
- Privacy: data-safety answers drafted in the manifest (health data, messages, media).
- Optional (needs approval): Firebase Crashlytics.

**Done when**
- `git tag v0.1.0 && git push --tags` produces a signed AAB on the GitHub Release.
- Internal-track install works on a physical device; sign-up → link → plan → log → chat loop verified.

**Prompt**
```
Read CLAUDE.md, .claude/context.md, .claude/standards.md and .claude/roadmap.md. Implement Phase 8
(ET-011) on branch feature/ET-011-release. Show me every signing/Play command instead of running it.
Use /release for the version bump and tag steps. Update the manifest, open a PR.
```

---

## Post-MVP backlog (not scheduled)

| Ticket | Item | Notes |
|---|---|---|
| ET-012 | Body metrics UI (trainee log, coach trend chart) | schema exists since ET-003 |
| ET-013 | Nutrition targets + daily log/adherence UI | schema exists since ET-003 |
| ET-014 | Google sign-in | supabase-kt `compose-auth` |
| ET-015 | Program templates library + copy between trainees | `programs.trainee_id = null` already supported |
| ET-016 | Coach analytics dashboard (adherence across roster, PR feed) | |
| ET-017 | Trainee can accept/decline sessions | small RLS change on `sessions.status` |
| ET-018 | Multi-coach per trainee / coach teams | changes the single-writer assumptions — design first |
| ET-019 | Realtime at scale: switch pokes from `postgres_changes` to `realtime.broadcast_changes` triggers + private channels | only if the roster grows large |
| ET-020 | Wear OS / Health Connect import | |
