-- milo. Connect: see and control your own devices. Run ONCE in the Supabase SQL editor.
-- Security model: every row belongs to one signed-in account and Row Level Security only lets that account touch it.
-- Device ids are random per install and are NOT credentials. No tokens or passwords are ever stored or sent through here.

create table if not exists public.milo_devices (
  id uuid primary key,
  uid uuid not null default auth.uid() references auth.users(id) on delete cascade,
  name text not null default 'Phone',
  kind text not null default 'phone',
  last_seen bigint not null default 0,
  now jsonb not null default '{}'::jsonb
);
alter table public.milo_devices enable row level security;
drop policy if exists "own devices" on public.milo_devices;
create policy "own devices" on public.milo_devices for all using (auth.uid() = uid) with check (auth.uid() = uid);

create table if not exists public.milo_commands (
  id bigint generated always as identity primary key,
  uid uuid not null default auth.uid() references auth.users(id) on delete cascade,
  target uuid not null references public.milo_devices(id) on delete cascade,
  from_device uuid,
  cmd jsonb not null,
  created bigint not null default 0
);
create index if not exists milo_commands_target on public.milo_commands(target, id);
alter table public.milo_commands enable row level security;
drop policy if exists "own commands" on public.milo_commands;
create policy "own commands" on public.milo_commands for all using (auth.uid() = uid) with check (
  auth.uid() = uid and exists (select 1 from public.milo_devices d where d.id = target and d.uid = auth.uid())
);

-- keep the command queue small: anything older than a minute is useless
create or replace function public.milo_prune_commands() returns void
language sql security definer set search_path = public as $$
  delete from public.milo_commands where created < (extract(epoch from now()) * 1000)::bigint - 60000;
$$;
revoke all on function public.milo_prune_commands() from public, anon;
grant execute on function public.milo_prune_commands() to authenticated;
