-- milo. security hardening. Safe to run more than once, in the Supabase SQL editor.
-- Why: Postgres lets everyone (including signed-out visitors) run a function unless that is revoked. These functions only make sense for signed-in users.

do $$ declare f text; begin
  foreach f in array array[
    'find_user(text)', 'delete_me()', 'push_save(text,text,jsonb)', 'push_drop(text)', 'delete_my_account()',
    'are_friends(uuid,uuid)'
  ] loop
    begin
      execute format('revoke all on function public.%s from public, anon', f);
      execute format('grant execute on function public.%s to authenticated', f);
    exception when undefined_function then null; end;
  end loop;
end $$;

-- sign-up needs this one while signed out (checks if a username is free); it only answers yes/no
do $$ begin
  begin
    revoke all on function public.username_free(text) from public;
    grant execute on function public.username_free(text) to anon, authenticated;
  exception when undefined_function then null; end;
end $$;

-- trigger functions are never called directly by clients
do $$ declare f text; begin
  foreach f in array array['handle_new_user()', 'friend_cap()', 'msg_before()', 'msg_after()', 'msg_guard()', 'fr_gone()'] loop
    begin execute format('revoke all on function public.%s from public, anon, authenticated', f);
    exception when undefined_function then null; end;
  end loop;
end $$;

-- text cleaner used by Sessions and shared playlists: strip control characters (safe form)
create or replace function public.milo_clean(p text, n int) returns text language sql immutable as $$
  select left(regexp_replace(coalesce(p, ''), '[[:cntrl:]]', '', 'g'), n)
$$;
