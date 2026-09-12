# Prompt: Implement Feature

Use this prompt when starting work that is not already a phase in `.claude/roadmap.md`.
Fill in the blanks and give it to Claude Code to plan the implementation.

---

Using the project rules and context in `CLAUDE.md` and `.claude/`:

Implement the following feature:

**Feature name:**
**Ticket / branch:**  ET-NNN · feature/ET-NNN-...

**Description:**
(What should the coach and/or trainee be able to do after this is implemented? Which role? Online, offline, or both?)

**Affected areas:**
- [ ] Supabase schema (new table / column → `/add-synced-table`)
- [ ] RLS policies or helper functions
- [ ] Edge function / trigger / pg_cron
- [ ] `core/data` repository or `core/sync`
- [ ] Feature module UI (→ `/compose-screen`) — which module:
- [ ] `core/designsystem` component (→ `/easytrain-design`)
- [ ] Navigation (`app` NavHost)
- [ ] Push notification kind
- [ ] CI / Gradle
- [ ] Other:

**Data changes (if any):**
(Tables, columns, writer role, how it syncs, realtime filter column)

**Screens / states (if any):**
(Loading / empty / content / error; what works offline)

**Tests required:**
(pgTAP for policies, repository, ViewModel, Robolectric screen, sync-engine cases)

**Definition of done:**
- [ ] Code written, `ktlintFormat` applied, `lintDebug` clean
- [ ] `testDebugUnitTest` green; `supabase test db` green if `supabase/` changed
- [ ] Works in airplane mode where the spec says it must
- [ ] `PROJECT_MANIFEST.md` updated (sections + `Last updated`)
- [ ] PR opened against master with the resolved-versions table if any version changed
