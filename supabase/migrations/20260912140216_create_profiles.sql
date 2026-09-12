-- One profiles row per auth user, created by a trigger the moment the user signs up. The role is
-- chosen once during onboarding and is immutable afterwards; it is mirrored into the JWT so RLS
-- policies never need a table lookup.

create or replace function public.set_updated_at()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
  new.updated_at = now();
  return new;
end;
$$;

create table public.profiles (
  id           uuid primary key references auth.users (id) on delete cascade,
  role         text check (role in ('coach', 'trainee')),
  display_name text,
  avatar_path  text,
  units        text not null default 'kg' check (units in ('kg', 'lb')),
  timezone     text,
  created_at   timestamptz not null default now(),
  updated_at   timestamptz not null default now(),
  deleted_at   timestamptz
);

alter table public.profiles enable row level security;

create trigger profiles_set_updated_at
  before update on public.profiles
  for each row execute function public.set_updated_at();

-- Delta pulls order by updated_at; every row is scoped to its owner by RLS.
create index profiles_updated_at_idx on public.profiles (updated_at);

-- The role as carried by the JWT. Policies use this instead of reading profiles.
create or replace function public.auth_role()
returns text
language sql
stable
set search_path = ''
as $$
  select auth.jwt() -> 'app_metadata' ->> 'role';
$$;

create or replace function public.handle_new_user()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
  insert into public.profiles (id, display_name)
  values (new.id, nullif(new.raw_user_meta_data ->> 'display_name', ''))
  on conflict (id) do nothing;
  return new;
end;
$$;

create trigger on_auth_user_created
  after insert on auth.users
  for each row execute function public.handle_new_user();

-- Write-once role: null -> coach/trainee and never anything else. A policy cannot express this
-- because it compares the old row with the new one.
create or replace function public.enforce_role_immutable()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
  if old.role is not null and new.role is distinct from old.role then
    raise exception 'profiles.role cannot be changed once set'
      using errcode = 'check_violation';
  end if;
  return new;
end;
$$;

create trigger profiles_role_immutable
  before update on public.profiles
  for each row execute function public.enforce_role_immutable();

create or replace function public.sync_role_to_jwt()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
  if tg_op = 'UPDATE' and new.role is not distinct from old.role then
    return new;
  end if;

  if new.role is not null then
    update auth.users
       set raw_app_meta_data =
             coalesce(raw_app_meta_data, '{}'::jsonb) || jsonb_build_object('role', new.role)
     where id = new.id;
  end if;

  return new;
end;
$$;

create trigger profiles_sync_role_to_jwt
  after insert or update of role on public.profiles
  for each row execute function public.sync_role_to_jwt();

-- ET-004 widens select to the linked coach/trainee once coach_trainees exists.
create policy "profiles select own" on public.profiles
  for select to authenticated
  using (id = (select auth.uid()));

create policy "profiles insert own" on public.profiles
  for insert to authenticated
  with check (id = (select auth.uid()));

create policy "profiles update own" on public.profiles
  for update to authenticated
  using (id = (select auth.uid()))
  with check (id = (select auth.uid()));

alter publication supabase_realtime add table public.profiles;
