# Skills Index

Project skills live in `.claude/skills/<name>/SKILL.md` and are invoked as `/<name>`. Claude may
invoke them automatically when the description matches, except those marked *user-only*.

| Skill | Invoke when | Auto-invoke |
|---|---|---|
| `/add-synced-table <table> <coach\|trainee\|self>` | Any new table or new column on a synced table: migration + RLS + trigger + indexes + realtime + pgTAP + Room + DTO + repository + `SyncRegistry` | yes |
| `/compose-screen <ScreenName> <feature-module>` | Any new screen, tab or bottom sheet: route, ViewModel, UiState, Screen + previews, strings, ViewModel test, Robolectric test, NavHost wiring | yes |
| `/resolve-versions [targets…]` | Before writing or changing any version in `gradle/libs.versions.toml`, any `uses:` in `.github/workflows`, or the Supabase CLI image tags | yes |
| `/supabase-dev` | Running the local stack, creating/applying migrations, `db reset`, pgTAP, serving edge functions, debugging "it doesn't sync" | yes |
| `/easytrain-design` | Before building or restyling any screen; when adding a component to `core/designsystem` | yes |
| `/release <version>` | Cutting a release: preconditions, version bump, changelog, tag (shown, not pushed), Play steps | **user-only** |

## Conventions

- Skills are recipes, not policy. Policy lives in `CLAUDE.md` and `standards.md`; if a skill and
  `standards.md` disagree, `standards.md` wins — fix the skill in the same PR.
- Keep each `SKILL.md` under 200 lines; move long reference material to a sibling file and link it.
- A skill that runs commands lists them in `allowed-tools`; anything on the destructive list in
  `CLAUDE.md` is never in `allowed-tools`.
- When a phase in `roadmap.md` teaches a new repeatable procedure (e.g. the upload queue in ET-010),
  add a skill for it in that PR and register it here.
