---
name: supabase-dev
description: Operate the local Supabase stack for EasyTrain — start/status, create and apply migrations, reset the local DB, run pgTAP RLS tests, serve edge functions, inspect data with psql, and point the Android emulator at it. Use when a task touches supabase/ or when the app needs a running backend for manual testing.
allowed-tools: Bash(supabase start) Bash(supabase status *) Bash(supabase stop) Bash(supabase migration new *) Bash(supabase migration list *) Bash(supabase db reset) Bash(supabase db diff *) Bash(supabase db lint *) Bash(supabase test db *) Bash(supabase functions serve *) Bash(supabase functions new *) Bash(psql *) Bash(docker ps *)
---

# Local Supabase workflow

Everything here targets the **local** Docker stack. Anything with `--linked`, `db push`,
`secrets set`, or `link` touches the hosted project and is on the destructive list in `CLAUDE.md`:
show it, don't run it.

## Start / inspect

```bash
supabase start                # first run pulls images; needs Docker Desktop
supabase status               # prints API URL (http://127.0.0.1:54321), DB URL, Studio URL, keys
supabase status -o env        # machine-readable; copy SUPABASE_URL + publishable key into local.properties
```

Emulator reaches the stack at `http://10.0.2.2:54321` — put **that** in `local.properties`
(`SUPABASE_URL=http://10.0.2.2:54321`), and the publishable key from `supabase status`.
Studio at `http://127.0.0.1:54323` for browsing rows and the Auth users list.

## Migrations

```bash
supabase migration new create_workouts        # → supabase/migrations/<ts>_create_workouts.sql
supabase db reset                             # drop + re-run all migrations + seed.sql (LOCAL)
supabase migration list                       # what's applied locally vs remote
supabase db lint                              # plpgsql_check on functions
supabase db diff --schema public -f <name>    # only to double-check hand-written SQL; never as the source of a migration
```

Rules from `standards.md §4`: one concern per file, append-only, RLS in the same file as the
table, `set search_path = ''` in every `security definer` function.

## RLS tests (pgTAP)

```bash
supabase test db                              # runs supabase/tests/*.sql
supabase test db supabase/tests/workouts.test.sql
```

`supabase/tests/helpers.sql` defines `tests.create_user(email, role)` and
`tests.authenticate_as(email)` (sets `request.jwt.claims` incl. `app_metadata.role`) — use them;
never test policies as `postgres`. Structure of a test file:

```sql
begin;
select plan(6);
\i supabase/tests/helpers.sql
select tests.create_user('coach@t.local', 'coach');
select tests.create_user('trainee@t.local', 'trainee');
select tests.link('coach@t.local', 'trainee@t.local');

select tests.authenticate_as('trainee@t.local');
select lives_ok($$ insert into public.workouts (id, trainee_id, coach_id, started_at, status)
  values (gen_random_uuid(), tests.uid('trainee@t.local'), tests.uid('coach@t.local'), now(), 'in_progress') $$,
  'trainee can insert own workout');

select tests.authenticate_as('coach@t.local');
select is((select count(*)::int from public.workouts), 1, 'linked coach can read');
select throws_ok($$ update public.workouts set notes = 'x' $$, '42501', null, 'coach cannot write trainee rows');
-- … stranger cases …
select * from finish();
rollback;
```

## Seed data

`supabase/seed.sql` runs after migrations on `db reset`. It must be idempotent. It seeds the
global exercise library and, guarded by `current_setting('app.env', true) = 'local'`, two dev users
(`coach@dev.local` / `trainee@dev.local`, password `password`) already linked, so manual testing
needs no sign-up.

## Edge functions

```bash
supabase functions new send-push
supabase functions serve --env-file supabase/functions/.env.local     # hot reload
curl -s -X POST http://127.0.0.1:54321/functions/v1/send-push \
  -H "Authorization: Bearer <publishable key>" -H "x-webhook-secret: local" \
  -d @supabase/functions/send-push/fixture.json
```
`.env.local` is gitignored; document required secret names in the manifest.

## Quick data checks

```bash
psql "$(supabase status -o env | grep DB_URL | cut -d= -f2- | tr -d '"')" \
  -c "select id, trainee_id, status, updated_at from public.workouts order by updated_at desc limit 5"
```

## When the app "doesn't sync"

1. `supabase status` — is realtime running? (`Realtime` is disabled by default on new **hosted** projects; enable it in the dashboard.)
2. Is the table in the publication? `select * from pg_publication_tables where pubname = 'supabase_realtime';`
3. RLS: run the failing query as the user in a pgTAP scratch file — a 0-row SELECT is almost always a policy, not the engine.
4. Check `sync_cursors` in Room (App Inspection) — a cursor in the future means a device-clock write slipped in.
