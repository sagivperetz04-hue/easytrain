---
name: add-synced-table
description: End-to-end recipe for adding a new synced table to EasyTrain — Supabase migration with sync columns, RLS policies, updated_at trigger, indexes, realtime publication, pgTAP tests, then the Room entity/DAO, DTO, mappers, repository and SyncRegistry registration. Use whenever a feature needs a new table or a new column on a synced table.
argument-hint: "[table_name] [owner: coach|trainee|self]"
---

# Add a synced table

Table: `$0` · Writer role: `$1`

Read `.claude/context.md` → *Data Model*, *RLS summary*, *Sync Contract* before starting. Work in
this order; do not skip a step — each later step depends on the earlier one being merged in the
same PR.

## 1. Migration (`supabase migration new create_$0`)

Every synced table has exactly this shape. Copy, don't improvise:

```sql
create table public.$0 (
  id          uuid primary key,
  -- owner / relationship columns (always both when the table is shared between the two roles;
  -- coach_id is NULLABLE on trainee-written tables — an unlinked trainee can still log):
  coach_id    uuid references public.profiles(id),
  trainee_id  uuid not null references public.profiles(id),
  -- child tables copy these from the parent (see context.md → Denormalization rule)
  -- domain columns …
  created_at  timestamptz not null default now(),
  updated_at  timestamptz not null default now(),
  deleted_at  timestamptz
);

alter table public.$0 enable row level security;

create trigger set_updated_at before update on public.$0
  for each row execute function public.set_updated_at();

-- one index per column used in a policy or delta pull
create index $0_trainee_updated_idx on public.$0 (trainee_id, updated_at);
create index $0_coach_updated_idx   on public.$0 (coach_id, updated_at);

-- policies: SELECT for both sides, INSERT/UPDATE for the single writer, no DELETE policy (soft delete)
create policy "$0 select" on public.$0 for select to authenticated
  using (trainee_id = (select auth.uid()) or public.is_coach_of(trainee_id));
  -- coach-written tables use: coach_id = (select auth.uid()) or trainee_id = (select auth.uid())

create policy "$0 insert" on public.$0 for insert to authenticated
  with check (<writer>_id = (select auth.uid()) and public.auth_role() = '<writer>');

create policy "$0 update" on public.$0 for update to authenticated
  using (<writer>_id = (select auth.uid()))
  with check (<writer>_id = (select auth.uid()));

alter publication supabase_realtime add table public.$0;
```

Rules: `text` + `check` for enums; `timestamptz`; child tables denormalize the owner columns
instead of joining in policies; helper functions (`is_coach_of`, `my_coach_ids`) instead of inline
`exists`. If the table needs server-only writes (RPC/trigger), grant nothing to `authenticated`
for that path and use a `security definer` function with `set search_path = ''`.

## 2. pgTAP test (`supabase/tests/$0.test.sql`) — write it before running the migration

Cover, with `tests.authenticate_as(...)` helpers from `supabase/tests/helpers.sql`:
- writer can insert and read back
- the linked other role can read, cannot write (`throws_ok` on insert/update)
- an unlinked user reads zero rows and cannot insert
- writer cannot write a row with someone else's owner id (`with check`)
- soft delete: setting `deleted_at` is allowed; there is no DELETE policy (`throws_ok` on delete)

Run `supabase db reset && supabase test db` until green.

## 3. Room (`core/database`)

- `$0Entity` implements `SyncedEntity` (`id`, `createdAt`, `updatedAt`, `deletedAt`, `syncState`, `localUpdatedAt`) — all timestamps epoch millis.
- `$0Dao`: `observeFor(userId): Flow<List<Entity>>` filtering `deleted_at IS NULL`; `@Upsert upsertAll`; `pending(): List<Entity>` (`sync_state = 'PENDING'`); `markSynced(ids, updatedAt)`; `upsertFromServer(rows)` that skips ids currently `PENDING` (single `@Transaction` method, tested).
- Bump `@Database(version = n+1)`, add the `Migration`, export the schema, add the migration test.

## 4. DTO + mappers (`core/network` / `core/data`)

- `$0Dto` `@Serializable` with `@SerialName` snake_case fields; timestamps as ISO strings.
- `fun $0Dto.asEntity(): $0Entity`, `fun $0Entity.asDto(): $0Dto`, `fun $0Entity.asExternalModel(): $0`.
- Domain model `$0` in `core/model`, `@Immutable`.

## 5. Repository + registration (`core/data`)

- `interface $0Repository` + `OfflineFirst$0Repository` (Room read, Room write with `PENDING`, then `syncScheduler.requestSync()`).
- Register in `SyncRegistry`: table name, DAO adapter, serializer, user-filter column (`trainee_id` / `coach_id` by role), parent tables (push/pull order).
- Add the realtime subscription filter in `RealtimePoker` via the registry (no per-table code).

## 6. Tests

- Repository test: in-memory Room + `FakeSupabaseTable` — write marks PENDING and requests sync; pull with a PENDING local row keeps the local row.
- DAO migration test.

## 7. Manifest

Add the table to `PROJECT_MANIFEST.md` → *Supabase schema* (columns, writer, policies summary, test file) and the module sections you touched. Update `Last updated`.
