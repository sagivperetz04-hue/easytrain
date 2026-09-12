-- Local stack only: `supabase db reset` re-runs this file, so everything here is idempotent.
-- The global exercise library and dev users land in ET-005.

-- pgTAP helpers. They live in the seed rather than a migration so they can never reach a hosted
-- project: they fabricate auth users and impersonate them, which is exactly what tests need and
-- exactly what production must not have.
create schema if not exists tests;

create or replace function tests.create_user(identifier text)
returns uuid
language plpgsql
security definer
as $$
declare
  user_id uuid := gen_random_uuid();
begin
  insert into auth.users (
    id, instance_id, aud, role, email, encrypted_password,
    email_confirmed_at, created_at, updated_at, raw_app_meta_data, raw_user_meta_data
  )
  values (
    user_id, '00000000-0000-0000-0000-000000000000', 'authenticated', 'authenticated',
    identifier || '@test.local', 'x',
    now(), now(), now(), '{"provider":"email","providers":["email"]}'::jsonb, '{}'::jsonb
  );
  return user_id;
end;
$$;

create or replace function tests.user_id(identifier text)
returns uuid
language sql
security definer
as $$
  select id from auth.users where email = identifier || '@test.local';
$$;

-- Reading auth.users needs definer rights (the caller may already be impersonating someone), but
-- switching roles cannot happen inside a definer function — hence the split.
create or replace function tests.claims_for(identifier text)
returns text
language plpgsql
security definer
as $$
declare
  u auth.users%rowtype;
begin
  select * into u from auth.users where email = identifier || '@test.local';
  if u.id is null then
    raise exception 'tests.claims_for: no user %', identifier;
  end if;

  return json_build_object(
    'sub', u.id::text,
    'role', 'authenticated',
    'email', u.email,
    'app_metadata', u.raw_app_meta_data
  )::text;
end;
$$;

-- Impersonate a user for the rest of the transaction, with the claim shape GoTrue issues, so
-- auth.uid() and public.auth_role() behave exactly as they do in the app.
create or replace function tests.authenticate_as(identifier text)
returns void
language plpgsql
as $$
declare
  claims text := tests.claims_for(identifier);
begin
  perform set_config('role', 'authenticated', true);
  perform set_config('request.jwt.claims', claims, true);
end;
$$;

create or replace function tests.clear_authentication()
returns void
language plpgsql
as $$
begin
  perform set_config('role', 'postgres', true);
  perform set_config('request.jwt.claims', null, true);
end;
$$;

grant usage on schema tests to authenticated, anon, service_role;
grant execute on all functions in schema tests to authenticated, anon, service_role;
