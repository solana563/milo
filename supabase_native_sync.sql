-- Run this ONCE in your Supabase project (SQL editor). It adds a private table for the native app's
-- likes, playlists and saved stations. It does not touch the web app's milo_sync table.
create table if not exists public.milo_native_sync (
  uid uuid primary key default auth.uid(),
  data jsonb not null default '{}'::jsonb,
  ts bigint not null default 0
);
alter table public.milo_native_sync enable row level security;

drop policy if exists "own row read" on public.milo_native_sync;
drop policy if exists "own row insert" on public.milo_native_sync;
drop policy if exists "own row update" on public.milo_native_sync;
drop policy if exists "own row delete" on public.milo_native_sync;
create policy "own row read"   on public.milo_native_sync for select using (auth.uid() = uid);
create policy "own row insert" on public.milo_native_sync for insert with check (auth.uid() = uid);
create policy "own row update" on public.milo_native_sync for update using (auth.uid() = uid) with check (auth.uid() = uid);
create policy "own row delete" on public.milo_native_sync for delete using (auth.uid() = uid);

-- Used by "Delete account" in the native app: removes the signed-in user's own data and login.
create or replace function public.delete_my_account() returns void
language plpgsql security definer set search_path = public, auth as $$
begin
  if auth.uid() is null then raise exception 'not signed in'; end if;
  delete from public.milo_native_sync where uid = auth.uid();
  delete from public.messages where sender = auth.uid() or rcpt = auth.uid();
  delete from public.friendships where a = auth.uid() or b = auth.uid();
  delete from public.profiles where id = auth.uid();
  delete from auth.users where id = auth.uid();
end $$;
revoke all on function public.delete_my_account() from public, anon;
grant execute on function public.delete_my_account() to authenticated;
