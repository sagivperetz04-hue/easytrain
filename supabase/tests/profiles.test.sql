begin;

select plan(13);

select tests.create_user('coach_a');
select tests.create_user('trainee_a');
select tests.create_user('stranger');

-- Signing up.
select is(
  (select count(*)::int from public.profiles where id = tests.user_id('coach_a')),
  1,
  'signing up creates exactly one profile row'
);

select is(
  (select role from public.profiles where id = tests.user_id('coach_a')),
  null,
  'a new profile has no role until onboarding sets one'
);

-- Owner access and the write-once role.
select tests.authenticate_as('coach_a');

select is(
  (select count(*)::int from public.profiles),
  1,
  'a signed-in user sees only their own profile'
);

select lives_ok(
  $$ update public.profiles set role = 'coach' where id = tests.user_id('coach_a') $$,
  'the role can be set once'
);

select is(
  (select role from public.profiles where id = tests.user_id('coach_a')),
  'coach',
  'the role is stored'
);

select throws_ok(
  $$ update public.profiles set role = 'trainee' where id = tests.user_id('coach_a') $$,
  'profiles.role cannot be changed once set',
  'the role cannot be changed afterwards'
);

-- sync_role_to_jwt: the next token the user gets carries the role, so RLS never reads profiles.
select tests.authenticate_as('coach_a');

select is(
  public.auth_role(),
  'coach',
  'the role is mirrored into the JWT app_metadata'
);

select lives_ok(
  $$ update public.profiles set display_name = 'Dana', units = 'lb' where id = tests.user_id('coach_a') $$,
  'other profile fields stay editable'
);

-- Sync cursors come from server time only: a client-supplied updated_at must not survive.
update public.profiles set updated_at = timestamptz '2000-01-01' where id = tests.user_id('coach_a');

select ok(
  (select updated_at > timestamptz '2020-01-01' from public.profiles where id = tests.user_id('coach_a')),
  'set_updated_at overrides a client-supplied updated_at'
);

-- Soft delete only.
select is(
  (select count(*)::int from pg_policies
    where schemaname = 'public' and tablename = 'profiles' and cmd = 'DELETE'),
  0,
  'there is no DELETE policy — rows are soft-deleted'
);

select lives_ok(
  $$ update public.profiles set deleted_at = now() where id = tests.user_id('coach_a') $$,
  'the owner can soft-delete their own profile'
);

-- Strangers see nothing and change nothing.
select tests.authenticate_as('trainee_a');
update public.profiles set display_name = 'Ari' where id = tests.user_id('trainee_a');

select tests.authenticate_as('stranger');

select is(
  (select count(*)::int from public.profiles where id = tests.user_id('trainee_a')),
  0,
  'a stranger cannot read someone else''s profile'
);

update public.profiles set display_name = 'hijacked' where id = tests.user_id('trainee_a');

select tests.authenticate_as('trainee_a');

select is(
  (select display_name from public.profiles where id = tests.user_id('trainee_a')),
  'Ari',
  'a stranger''s write matches no rows and leaves the profile untouched'
);

select * from finish();

rollback;
