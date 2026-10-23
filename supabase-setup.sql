-- milo. cloud sync: run once in Supabase > SQL editor
create table if not exists public.milo_sync (
  user_id uuid primary key default auth.uid() references auth.users(id) on delete cascade,
  data jsonb not null default '{}'::jsonb,
  updated_at timestamptz not null default now()
);
alter table public.milo_sync enable row level security;
drop policy if exists "milo read own" on public.milo_sync;
drop policy if exists "milo insert own" on public.milo_sync;
drop policy if exists "milo update own" on public.milo_sync;
drop policy if exists "milo delete own" on public.milo_sync;
create policy "milo read own" on public.milo_sync for select using (auth.uid() = user_id);
create policy "milo insert own" on public.milo_sync for insert with check (auth.uid() = user_id);
create policy "milo update own" on public.milo_sync for update using (auth.uid() = user_id) with check (auth.uid() = user_id);
create policy "milo delete own" on public.milo_sync for delete using (auth.uid() = user_id);

-- ======================= milo. accounts, friends, sharing and chat =======================
-- Run this whole file once in Supabase > SQL editor. Safe to run again.
-- Tip: Authentication > Providers > Email > turn OFF "Confirm email" so people can start right after signing up.

create table if not exists public.profiles (
  id uuid primary key references auth.users(id) on delete cascade,
  username text not null unique check (username ~ '^[a-z0-9_]{3,20}$'),
  name text not null default '',
  av jsonb not null default '{}'::jsonb check (pg_column_size(av) < 90000),
  terms_v int not null default 0,
  terms_at timestamptz,
  share_pl boolean not null default true,
  share_np boolean not null default true,
  created_at timestamptz not null default now()
);
-- optional profile cover photo (used by miloCHAT)
alter table public.profiles add column if not exists cover text check (cover is null or length(cover) < 70000);
alter table public.profiles enable row level security;
drop policy if exists "profiles read" on public.profiles;
drop policy if exists "profiles insert own" on public.profiles;
drop policy if exists "profiles update own" on public.profiles;
create policy "profiles read" on public.profiles for select to authenticated using (true);
create policy "profiles insert own" on public.profiles for insert to authenticated with check (id = auth.uid());
create policy "profiles update own" on public.profiles for update to authenticated using (id = auth.uid()) with check (id = auth.uid());

-- create the profile automatically when someone signs up with a username
create or replace function public.handle_new_user() returns trigger language plpgsql security definer set search_path = public as $$
declare u text := lower(coalesce(new.raw_user_meta_data->>'username',''));
begin
  if u ~ '^[a-z0-9_]{3,20}$' then
    insert into public.profiles(id, username, name) values (new.id, u, coalesce(nullif(new.raw_user_meta_data->>'name',''), u));
  end if;
  return new;
end $$;
drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created after insert on auth.users for each row execute function public.handle_new_user();

create or replace function public.username_free(u text) returns boolean language sql security definer set search_path = public as
$$ select not exists (select 1 from public.profiles where username = lower(u)) $$;
grant execute on function public.username_free(text) to anon, authenticated;

create or replace function public.find_user(u text) returns table(id uuid, username text, name text, av jsonb) language sql security definer set search_path = public as
$$ select p.id, p.username, p.name, p.av from public.profiles p where p.username = lower(u) limit 1 $$;
grant execute on function public.find_user(text) to authenticated;

-- friendships: a asks b; b accepts
create table if not exists public.friendships (
  a uuid not null references auth.users(id) on delete cascade,
  b uuid not null references auth.users(id) on delete cascade,
  status text not null default 'pending' check (status in ('pending','accepted')),
  created_at timestamptz not null default now(),
  primary key (a, b),
  check (a <> b)
);
create unique index if not exists friendships_pair on public.friendships (least(a, b), greatest(a, b));
alter table public.friendships enable row level security;
drop policy if exists "fr read" on public.friendships;
drop policy if exists "fr ask" on public.friendships;
drop policy if exists "fr accept" on public.friendships;
drop policy if exists "fr delete" on public.friendships;
create policy "fr read" on public.friendships for select to authenticated using (auth.uid() in (a, b));
create policy "fr ask" on public.friendships for insert to authenticated with check (a = auth.uid() and status = 'pending');
create policy "fr accept" on public.friendships for update to authenticated using (b = auth.uid()) with check (b = auth.uid() and status = 'accepted');
create policy "fr delete" on public.friendships for delete to authenticated using (auth.uid() in (a, b));

create or replace function public.are_friends(x uuid, y uuid) returns boolean language sql stable security definer set search_path = public as
$$ select exists (select 1 from public.friendships f where f.status = 'accepted' and ((f.a = x and f.b = y) or (f.a = y and f.b = x))) $$;

create or replace function public.friend_cap() returns trigger language plpgsql security definer set search_path = public as $$
begin
  if (select count(*) from public.friendships where a = new.a or b = new.a) >= 200 then raise exception 'friend_limit'; end if;
  return new;
end $$;
drop trigger if exists friend_cap_t on public.friendships;
create trigger friend_cap_t before insert on public.friendships for each row execute function public.friend_cap();

-- shared: now playing + playlists, stored as song DNA (metadata and a stream source, never audio files)
create table if not exists public.shared (
  user_id uuid primary key references auth.users(id) on delete cascade,
  np jsonb, np_at timestamptz,
  pl jsonb check (pl is null or pg_column_size(pl) < 400000), pl_at timestamptz
);
alter table public.shared enable row level security;
drop policy if exists "shared read" on public.shared;
drop policy if exists "shared insert" on public.shared;
drop policy if exists "shared update" on public.shared;
drop policy if exists "shared delete" on public.shared;
create policy "shared read" on public.shared for select to authenticated using (user_id = auth.uid() or public.are_friends(auth.uid(), user_id));
create policy "shared insert" on public.shared for insert to authenticated with check (user_id = auth.uid());
create policy "shared update" on public.shared for update to authenticated using (user_id = auth.uid()) with check (user_id = auth.uid());
create policy "shared delete" on public.shared for delete to authenticated using (user_id = auth.uid());

-- messages: disappear after 24 hours, each chat holds 5 MB, oldest removed first
create table if not exists public.messages (
  id bigint generated always as identity primary key,
  chat text not null,
  sender uuid not null default auth.uid() references auth.users(id) on delete cascade,
  rcpt uuid not null references auth.users(id) on delete cascade,
  kind text not null check (kind in ('text','song','img')),
  body text not null,
  bytes int not null default 0,
  created_at timestamptz not null default now()
);
create index if not exists messages_chat_time on public.messages (chat, created_at desc);
create index if not exists messages_rcpt_time on public.messages (rcpt, created_at desc);
-- read receipts, replies, reactions and a short preview (so the chat list never downloads photos)
-- voice notes (miloCHAT)
alter table public.messages drop constraint if exists messages_kind_check;
alter table public.messages add constraint messages_kind_check check (kind in ('text','song','img','voice'));
alter table public.messages add column if not exists read_at timestamptz;
alter table public.messages add column if not exists reply_to bigint;
alter table public.messages add column if not exists rx jsonb not null default '{}'::jsonb;
alter table public.messages add column if not exists pv text generated always as (case when kind = 'text' then left(body, 80) when kind = 'song' then left(body, 300) else null end) stored;
alter table public.messages enable row level security;
drop policy if exists "msg read" on public.messages;
drop policy if exists "msg send" on public.messages;
create policy "msg read" on public.messages for select to authenticated using (auth.uid() in (sender, rcpt) and created_at > now() - interval '1 day');
create policy "msg send" on public.messages for insert to authenticated with check (sender = auth.uid() and public.are_friends(sender, rcpt));
drop policy if exists "msg update" on public.messages;
drop policy if exists "msg unsend" on public.messages;
create policy "msg update" on public.messages for update to authenticated using (auth.uid() in (sender, rcpt)) with check (auth.uid() in (sender, rcpt));
create policy "msg unsend" on public.messages for delete to authenticated using (sender = auth.uid());

create or replace function public.msg_before() returns trigger language plpgsql security definer set search_path = public as $$
begin
  new.sender := auth.uid();
  new.chat := least(new.sender::text, new.rcpt::text) || ':' || greatest(new.sender::text, new.rcpt::text);
  new.bytes := octet_length(new.body) + 64;
  new.read_at := null;
  new.rx := '{}'::jsonb;
  if new.reply_to is not null and not exists (select 1 from public.messages where id = new.reply_to and chat = new.chat) then new.reply_to := null; end if;
  if new.bytes > 400000 then raise exception 'message_too_large'; end if;
  if (select count(*) from public.messages where sender = new.sender and created_at > now() - interval '1 minute') >= 30 then raise exception 'rate_limited'; end if;
  return new;
end $$;
drop trigger if exists msg_before_t on public.messages;
create trigger msg_before_t before insert on public.messages for each row execute function public.msg_before();

create or replace function public.msg_after() returns trigger language plpgsql security definer set search_path = public as $$
begin
  delete from public.messages where chat = new.chat and created_at < now() - interval '1 day';
  delete from public.messages where id in (
    select id from (select id, sum(bytes) over (order by created_at desc, id desc) as run from public.messages where chat = new.chat) s where s.run > 5242880);
  return null;
end $$;
drop trigger if exists msg_after_t on public.messages;
create trigger msg_after_t after insert on public.messages for each row execute function public.msg_after();

-- on update, only read_at (by the recipient, once) and your own reaction can change
create or replace function public.msg_guard() returns trigger language plpgsql security definer set search_path = public as $$
begin
  if auth.uid() is null then return new; end if; -- server-side edits (dashboard, service role) are not restricted
  if new.id <> old.id or new.chat <> old.chat or new.sender <> old.sender or new.rcpt <> old.rcpt or new.kind <> old.kind or new.body <> old.body
     or new.bytes <> old.bytes or new.created_at <> old.created_at or new.reply_to is distinct from old.reply_to then raise exception 'message_locked'; end if;
  if new.read_at is distinct from old.read_at then
    if auth.uid() is distinct from old.rcpt or old.read_at is not null then new.read_at := old.read_at; end if;
  end if;
  if new.rx is distinct from old.rx then
    if pg_column_size(new.rx) > 1500 then raise exception 'rx_too_large'; end if;
    if (new.rx - coalesce(auth.uid()::text, '')) <> (old.rx - coalesce(auth.uid()::text, '')) then raise exception 'rx_locked'; end if;
  end if;
  return new;
end $$;
drop trigger if exists msg_guard_t on public.messages;
create trigger msg_guard_t before update on public.messages for each row execute function public.msg_guard();

-- live updates (instant messages, friend requests, sync between devices)
do $$ declare t text; begin
  foreach t in array array['messages','friendships','shared','milo_sync'] loop
    begin execute format('alter publication supabase_realtime add table public.%I', t);
    exception when duplicate_object then null; when undefined_object then null; end;
  end loop;
end $$;

-- removing a friend deletes the chat
create or replace function public.fr_gone() returns trigger language plpgsql security definer set search_path = public as $$
begin
  delete from public.messages where chat = least(old.a::text, old.b::text) || ':' || greatest(old.a::text, old.b::text);
  return old;
end $$;
drop trigger if exists fr_gone_t on public.friendships;
create trigger fr_gone_t after delete on public.friendships for each row execute function public.fr_gone();

-- delete my account and everything under it
create or replace function public.delete_me() returns void language plpgsql security definer set search_path = public, auth as $$
begin
  delete from auth.users where id = auth.uid();
end $$;
grant execute on function public.delete_me() to authenticated;

-- push notifications (new messages and friend requests while the app is closed)
create table if not exists public.push_subs (
  id bigint generated always as identity primary key,
  user_id uuid not null references auth.users(id) on delete cascade,
  kind text not null check (kind in ('web','fcm')),
  endpoint text not null unique,
  keys jsonb,
  created_at timestamptz not null default now()
);
create index if not exists push_subs_user on public.push_subs (user_id);
alter table public.push_subs enable row level security;
drop policy if exists "push own" on public.push_subs;
create policy "push own" on public.push_subs for select to authenticated using (user_id = auth.uid());
create or replace function public.push_save(k text, e text, x jsonb) returns void language plpgsql security definer set search_path = public as $$
begin
  if auth.uid() is null or k not in ('web','fcm') or length(e) > 2000 then return; end if;
  delete from public.push_subs where endpoint = e;
  delete from public.push_subs where id in (select id from public.push_subs where user_id = auth.uid() and kind = k order by created_at desc offset 4);
  insert into public.push_subs(user_id, kind, endpoint, keys) values (auth.uid(), k, e, x);
end $$;
grant execute on function public.push_save(text, text, jsonb) to authenticated;
create or replace function public.push_drop(e text) returns void language sql security definer set search_path = public as
$$ delete from public.push_subs where endpoint = e and user_id = auth.uid() $$;
grant execute on function public.push_drop(text) to authenticated;

-- ===== group chats (miloCHAT): groups, members, messages (disappear after 24 hours, 5 MB per group) =====
create table if not exists public.groups (
  id uuid primary key default gen_random_uuid(),
  name text not null check (char_length(name) between 1 and 40),
  owner uuid not null default auth.uid() references auth.users(id) on delete cascade,
  created_at timestamptz not null default now()
);
create table if not exists public.group_members (
  gid uuid not null references public.groups(id) on delete cascade,
  uid uuid not null references auth.users(id) on delete cascade,
  added_at timestamptz not null default now(),
  primary key (gid, uid)
);
create index if not exists group_members_uid on public.group_members (uid);
create or replace function public.is_member(g uuid, u uuid) returns boolean language sql stable security definer set search_path = public as
$$ select exists (select 1 from public.group_members where gid = g and uid = u) $$;
create or replace function public.is_group_owner(g uuid, u uuid) returns boolean language sql stable security definer set search_path = public as
$$ select exists (select 1 from public.groups where id = g and owner = u) $$;
alter table public.groups enable row level security;
alter table public.group_members enable row level security;
drop policy if exists "grp read" on public.groups;
drop policy if exists "grp insert" on public.groups;
drop policy if exists "grp update" on public.groups;
drop policy if exists "grp delete" on public.groups;
create policy "grp read" on public.groups for select to authenticated using (owner = auth.uid() or public.is_member(id, auth.uid()));
create policy "grp insert" on public.groups for insert to authenticated with check (owner = auth.uid());
create policy "grp update" on public.groups for update to authenticated using (owner = auth.uid()) with check (owner = auth.uid());
create policy "grp delete" on public.groups for delete to authenticated using (owner = auth.uid());
drop policy if exists "gm read" on public.group_members;
drop policy if exists "gm add" on public.group_members;
drop policy if exists "gm remove" on public.group_members;
create policy "gm read" on public.group_members for select to authenticated using (public.is_member(gid, auth.uid()));
create policy "gm add" on public.group_members for insert to authenticated with check (public.is_group_owner(gid, auth.uid()) and (uid = auth.uid() or public.are_friends(auth.uid(), uid)));
create policy "gm remove" on public.group_members for delete to authenticated using (uid = auth.uid() or public.is_group_owner(gid, auth.uid()));
create or replace function public.grp_after() returns trigger language plpgsql security definer set search_path = public as $$
begin insert into public.group_members(gid, uid) values (new.id, new.owner) on conflict do nothing; return null; end $$;
drop trigger if exists grp_after_t on public.groups;
create trigger grp_after_t after insert on public.groups for each row execute function public.grp_after();
create or replace function public.gm_before() returns trigger language plpgsql security definer set search_path = public as $$
begin if (select count(*) from public.group_members where gid = new.gid) >= 20 then raise exception 'group_full'; end if; return new; end $$;
drop trigger if exists gm_before_t on public.group_members;
create trigger gm_before_t before insert on public.group_members for each row execute function public.gm_before();

create table if not exists public.group_msgs (
  id bigint generated always as identity primary key,
  gid uuid not null references public.groups(id) on delete cascade,
  sender uuid not null default auth.uid() references auth.users(id) on delete cascade,
  kind text not null check (kind in ('text','song','img','voice')),
  body text not null,
  bytes int not null default 0,
  created_at timestamptz not null default now(),
  reply_to bigint,
  rx jsonb not null default '{}'::jsonb,
  pv text generated always as (case when kind = 'text' then left(body, 80) when kind = 'song' then left(body, 300) else null end) stored
);
create index if not exists group_msgs_time on public.group_msgs (gid, created_at desc);
alter table public.group_msgs enable row level security;
drop policy if exists "gmsg read" on public.group_msgs;
drop policy if exists "gmsg send" on public.group_msgs;
drop policy if exists "gmsg update" on public.group_msgs;
drop policy if exists "gmsg unsend" on public.group_msgs;
create policy "gmsg read" on public.group_msgs for select to authenticated using (public.is_member(gid, auth.uid()) and created_at > now() - interval '1 day');
create policy "gmsg send" on public.group_msgs for insert to authenticated with check (sender = auth.uid() and public.is_member(gid, auth.uid()));
create policy "gmsg update" on public.group_msgs for update to authenticated using (public.is_member(gid, auth.uid())) with check (public.is_member(gid, auth.uid()));
create policy "gmsg unsend" on public.group_msgs for delete to authenticated using (sender = auth.uid());
create or replace function public.gmsg_before() returns trigger language plpgsql security definer set search_path = public as $$
begin
  new.sender := auth.uid();
  new.bytes := octet_length(new.body) + 64;
  new.rx := '{}'::jsonb;
  if new.reply_to is not null and not exists (select 1 from public.group_msgs where id = new.reply_to and gid = new.gid) then new.reply_to := null; end if;
  if new.bytes > 400000 then raise exception 'message_too_large'; end if;
  if (select count(*) from public.group_msgs where sender = new.sender and created_at > now() - interval '1 minute') >= 30 then raise exception 'rate_limited'; end if;
  return new;
end $$;
drop trigger if exists gmsg_before_t on public.group_msgs;
create trigger gmsg_before_t before insert on public.group_msgs for each row execute function public.gmsg_before();
create or replace function public.gmsg_after() returns trigger language plpgsql security definer set search_path = public as $$
begin
  delete from public.group_msgs where gid = new.gid and created_at < now() - interval '1 day';
  delete from public.group_msgs where id in (
    select id from (select id, sum(bytes) over (order by created_at desc, id desc) as run from public.group_msgs where gid = new.gid) s where s.run > 5242880);
  return null;
end $$;
drop trigger if exists gmsg_after_t on public.group_msgs;
create trigger gmsg_after_t after insert on public.group_msgs for each row execute function public.gmsg_after();
create or replace function public.gmsg_guard() returns trigger language plpgsql security definer set search_path = public as $$
begin
  if auth.uid() is null then return new; end if;
  if new.id <> old.id or new.gid <> old.gid or new.sender <> old.sender or new.kind <> old.kind or new.body <> old.body
     or new.bytes <> old.bytes or new.created_at <> old.created_at or new.reply_to is distinct from old.reply_to then raise exception 'message_locked'; end if;
  if new.rx is distinct from old.rx then
    if pg_column_size(new.rx) > 3000 then raise exception 'rx_too_large'; end if;
    if (new.rx - coalesce(auth.uid()::text, '')) <> (old.rx - coalesce(auth.uid()::text, '')) then raise exception 'rx_locked'; end if;
  end if;
  return new;
end $$;
drop trigger if exists gmsg_guard_t on public.group_msgs;
create trigger gmsg_guard_t before update on public.group_msgs for each row execute function public.gmsg_guard();
do $$ declare t text; begin
  foreach t in array array['groups','group_members','group_msgs'] loop
    begin execute format('alter publication supabase_realtime add table public.%I', t);
    exception when duplicate_object then null; when undefined_object then null; end;
  end loop;
end $$;

-- optional but recommended: also purge expired messages every hour (Database > Extensions > enable pg_cron first)
-- select cron.schedule('milo-purge', '0 * * * *', $$ delete from public.messages where created_at < now() - interval '1 day' $$);
