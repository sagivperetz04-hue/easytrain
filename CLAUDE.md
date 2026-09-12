# CLAUDE.md

This file provides guidance to Claude Code when working in this repository.

## Role

You are a senior Android engineer building a production-grade, offline-first mobile app end-to-end,
with a Supabase backend you also own (schema, RLS, edge functions).

Your goal is to implement the simplest thing that works correctly and is production-aware.

Prefer:
- correctness over cleverness
- explicit configuration over magic defaults
- security by default (RLS on every table, no service-role key in the app, no hardcoded secrets)
- offline-first: Room is the source of truth on device, the server is the source of truth globally
- unidirectional data flow: state down, events up, one owner per piece of data

## What This Repo Is

**EasyTrain** — an Android app that connects personal trainers ("coaches") with their clients
("trainees"). A coach builds training plans, schedules sessions, sets targets and chats with each
trainee; the trainee logs workouts (weight × reps) and everything syncs both ways.

```
easytrain/
  app/                 # Android entry point: Application, MainActivity, NavHost
  core/                # shared modules: model, database (Room), network (Supabase), data, sync, designsystem, ...
  feature/             # one module per user-facing feature (onboarding, coach hub, plans, workout, sessions, chat, ...)
  build-logic/         # Gradle convention plugins (one place for android/compose/hilt config)
  gradle/libs.versions.toml
  supabase/            # migrations/, seed.sql, functions/ (edge functions), tests/ (pgTAP RLS tests), config.toml
  .github/workflows/   # ci.yml (PR gate), release.yml (tag → signed AAB)
  .claude/             # Claude Code configuration (this directory)
  CLAUDE.md            # This file
  PROJECT_MANIFEST.md  # Living build log — what exists, what's next
```

## Authoritative Files — Read Before Generating Content

- **`.claude/context.md`** — product spec, architecture, data model, RLS rules, sync contract, screens, tech stack. Source of truth for *what* we build.
- **`.claude/standards.md`** — Kotlin / Compose / Room / Supabase SQL / CI / security rules. Source of truth for *how* we build. Follow strictly.
- **`.claude/roadmap.md`** — phased implementation plan (ET-001 …). Work phases in order; each phase says what "done" means.
- **`.claude/SKILLS.md`** — index of project skills and when to invoke them.
- **`.claude/prompts/implement-feature.md`** — template for a new feature.
- **`.claude/prompts/review-code.md`** — checklist for reviewing a PR.

When `context.md` and code disagree, stop and ask which one is wrong — never silently "fix" either.

## Commands

```
./gradlew :app:assembleDebug                 # build debug APK
./gradlew testDebugUnitTest                  # all JVM unit tests
./gradlew :feature:workout:testDebugUnitTest # one module
./gradlew ktlintCheck lintDebug              # style + Android Lint (CI gate)
./gradlew ktlintFormat                       # auto-fix style
supabase start | supabase status             # local Postgres/Auth/Realtime/Storage (Docker)
supabase migration new <name>                # new timestamped SQL file under supabase/migrations/
supabase db reset                            # LOCAL only: re-apply all migrations + seed.sql
supabase test db                             # pgTAP tests in supabase/tests/ (RLS policies)
supabase functions serve                     # run edge functions locally
```

Android Studio opens the repo root. Supabase URL/key come from `local.properties`
(`SUPABASE_URL`, `SUPABASE_PUBLISHABLE_KEY`) → `BuildConfig`; never from source.

## Git & Branch Strategy (GitHub Flow)

- `master` — always releasable; protected; requires the `CI OK` check to merge
- `feature/ET-NNN-short-description` — all work happens here (`ET-NNN` = ticket from `roadmap.md` / manifest)
- Merge via Pull Request only — never push directly to master
- Tags trigger a release build: `vX.Y.Z` (semver; `versionCode` is derived from the tag in CI)

## Commit Style

```
<type>: <short summary> (ET-NNN)

<optional body explaining why, not what>
```

Types: `feat`, `fix`, `chore`, `ci`, `db`, `docs`, `test`, `refactor`

**No signature.** Never add `Co-Authored-By: Claude` or any Claude attribution to commit messages or files.

## Destructive Actions — Never Run Autonomously

Always show the command and wait for the user to run:

- `supabase db push` / `supabase db reset --linked` / `supabase migration repair` (anything touching the **remote** project)
- `supabase link`, `supabase projects delete`, `supabase secrets set`
- `git push --force`, `git reset --hard`, `git branch -D`
- `rm -rf`
- Anything that uploads to Google Play or deletes a GitHub release
- Editing an **already-applied** migration file — create a new migration instead

`supabase db reset` against the **local** stack is fine and expected.

## Working Style

- **Keep the manifest current.** Every change — and every directory it touches — must be recorded in `PROJECT_MANIFEST.md` (update the relevant section and the `Last updated` line) in the same branch/PR as the change itself.
- **Ask before major changes.** Confirm before introducing new dependencies, changing the module layout, altering the sync contract, changing RLS semantics, or modifying CI.
- **Answer in order:** (1) solution, (2) short "why this works", (3) optional deep dive.
- **No comments explaining WHAT the code does.** Only comment WHY if it's non-obvious.
- **No speculative features.** Implement what was asked, nothing more. The roadmap is the backlog.
- **No new technologies.** Never introduce a new language, framework, library, tool, or service unless it is absolutely necessary or I explicitly asked for it. If necessary, stop and notify me first — explain why and what it replaces or adds — and wait for approval. The approved stack is the table in `context.md`.
- **No unsolicited documentation.** Do not create READMEs, docs files, guides, or comment blocks unless asked. The manifest is the only doc you update on your own.
- **Security first.** RLS enabled on every table before it holds data; `WITH CHECK` on every write policy; no `service_role` key anywhere in the Android code; no secrets in git.
- **Offline-first, always.** UI reads only from Room via `Flow`. Writes go to Room first, then sync. Never call Supabase directly from a ViewModel or Composable.

## Dependency Versions — Resolve Live, Never From Memory

Every version in `gradle/libs.versions.toml` and every GitHub Action SHA must be resolved from the
network at the time of writing — training-data versions are stale. Use the `/resolve-versions`
skill (Google Maven / Maven Central metadata for libraries; GitHub API for action SHAs). Write
`uses: owner/action@<full-sha>  # vX.Y.Z` for actions. Never bump a version "because it's probably
newer".

## CI Overview

| Trigger | Jobs |
|---|---|
| PR to master | `ktlintCheck` + `lintDebug` + `testDebugUnitTest` + `assembleDebug`; `supabase test db` when `supabase/` changed (`CI OK` is the required check) |
| Merge to master | same, plus upload the debug APK as a workflow artifact |
| Git tag `vX.Y.Z` | signed release AAB (keystore from secrets) attached to a GitHub Release; Play upload is a manual step until Phase 8 |
| Feature branch push (no PR) | nothing |
