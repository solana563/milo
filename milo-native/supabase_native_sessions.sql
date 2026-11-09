-- milo. Sessions: listening rooms. Run ONCE in the Supabase SQL editor.
--
-- Security model: every table has Row Level Security ON and NO policies, so the app can never read or write a table directly.
-- All access goes through the functions below (security definer), and each one checks who is calling, whether they are in the room,
-- whether they are the host, muted or banned, and rate-limits writes. Private rooms are only reachable with their code.

create table if not exists public.milo_rooms (
  id uuid primary key default gen_random_uuid(),
  host uuid not null references auth.users(id) on delete cascade,
  name text not null,
  code text not null unique,
  is_public boolean not null default false,
  open_queue boolean not null default false,      -- true: members add straight to the queue; false: they suggest, host approves
  state jsonb not null default '{}'::jsonb,       -- what the host is playing: {t: track json, pos, pl, at}
  host_seen bigint not null default 0,
  created bigint not null,
  ended boolean not null default false
);
create table if not exists public.milo_room_members (
  room uuid not null references public.milo_rooms(id) on delete cascade,
  uid uuid not null references auth.users(id) on delete cascade,
  name text not null,
  role text not null default 'member',
  muted boolean not null default false,
  banned boolean not null default false,
  joined bigint not null,
  last_seen bigint not null,
  primary key (room, uid)
);
create table if not exists public.milo_room_queue (
  id bigint generated always as identity primary key,
  room uuid not null references public.milo_rooms(id) on delete cascade,
  added_by uuid not null references auth.users(id) on delete cascade,
  by_name text not null default '',
  track jsonb not null,
  status text not null default 'suggested',       -- suggested | queued | played
  created bigint not null
);
create table if not exists public.milo_room_events (
  id bigint generated always as identity primary key,
  room uuid not null references public.milo_rooms(id) on delete cascade,
  uid uuid not null references auth.users(id) on delete cascade,
  name text not null default '',
  kind text not null,                              -- react | msg | join | leave
  body text not null default '',
  created bigint not null
);
create table if not exists public.milo_reports (
  id bigint generated always as identity primary key,
  reporter uuid not null references auth.users(id) on delete cascade,
  room uuid references public.milo_rooms(id) on delete set null,
  target uuid not null,
  reason text not null default '',
  created bigint not null
);
alter table public.milo_rooms enable row level security;
alter table public.milo_room_members enable row level security;
alter table public.milo_room_queue enable row level security;
alter table public.milo_room_events enable row level security;
alter table public.milo_reports enable row level security;
create index if not exists milo_room_events_room on public.milo_room_events(room, id);
create index if not exists milo_room_queue_room on public.milo_room_queue(room, id);

create or replace function public.milo_now() returns bigint language sql stable as $$ select (extract(epoch from clock_timestamp()) * 1000)::bigint $$;

create or replace function public.milo_clean(p text, n int) returns text language sql immutable as $$
  select left(regexp_replace(coalesce(p, ''), '[\x00-\x1f]', '', 'g'), n)
$$;

-- caller must be an unbanned member of the room
create or replace function public.milo_member(p_room uuid) returns public.milo_room_members
language plpgsql stable security definer set search_path = public as $$
declare m public.milo_room_members;
begin
  select * into m from public.milo_room_members where room = p_room and uid = auth.uid() and not banned;
  if not found then raise exception 'not in this session'; end if;
  return m;
end $$;

create or replace function public.create_room(p_name text, p_public boolean, p_open_queue boolean, p_my_name text) returns jsonb
language plpgsql security definer set search_path = public as $$
declare rid uuid; c text; alphabet text := 'ABCDEFGHJKMNPQRSTUVWXYZ23456789'; i int;
begin
  if auth.uid() is null then raise exception 'sign in first'; end if;
  update public.milo_rooms set ended = true where host = auth.uid() and not ended;       -- one live room per host
  loop
    c := ''; for i in 1..6 loop c := c || substr(alphabet, 1 + floor(random() * length(alphabet))::int, 1); end loop;
    exit when not exists (select 1 from public.milo_rooms where code = c);
  end loop;
  insert into public.milo_rooms(host, name, code, is_public, open_queue, created, host_seen)
    values (auth.uid(), coalesce(nullif(milo_clean(p_name, 40), ''), 'Listening room'), c, coalesce(p_public, false), coalesce(p_open_queue, false), milo_now(), milo_now())
    returning id into rid;
  insert into public.milo_room_members(room, uid, name, role, joined, last_seen)
    values (rid, auth.uid(), coalesce(nullif(milo_clean(p_my_name, 24), ''), 'Host'), 'host', milo_now(), milo_now());
  return jsonb_build_object('id', rid, 'code', c);
end $$;

create or replace function public.join_room(p_code text, p_my_name text) returns jsonb
language plpgsql security definer set search_path = public as $$
declare r public.milo_rooms; m public.milo_room_members; nm text; existed boolean;
begin
  if auth.uid() is null then raise exception 'sign in first'; end if;
  select * into r from public.milo_rooms where code = upper(trim(p_code)) and not ended;
  if not found then raise exception 'no such session'; end if;
  select * into m from public.milo_room_members where room = r.id and uid = auth.uid();
  existed := found;
  if existed and m.banned then raise exception 'not allowed'; end if;
  nm := coalesce(nullif(milo_clean(p_my_name, 24), ''), 'Listener');
  if (select count(*) from public.milo_room_members where room = r.id and last_seen > milo_now() - 20000) >= 50 and not existed then raise exception 'session is full'; end if;
  insert into public.milo_room_members(room, uid, name, joined, last_seen) values (r.id, auth.uid(), nm, milo_now(), milo_now())
    on conflict (room, uid) do update set last_seen = milo_now(), name = excluded.name;
  if not existed or m.last_seen < milo_now() - 20000 then
    insert into public.milo_room_events(room, uid, name, kind, created) values (r.id, auth.uid(), nm, 'join', milo_now());
  end if;
  return jsonb_build_object('id', r.id, 'code', r.code);
end $$;

create or replace function public.leave_room(p_room uuid) returns void
language plpgsql security definer set search_path = public as $$
declare m public.milo_room_members;
begin
  select * into m from public.milo_room_members where room = p_room and uid = auth.uid();
  if not found then return; end if;
  if m.role = 'host' then update public.milo_rooms set ended = true where id = p_room; return; end if;
  delete from public.milo_room_members where room = p_room and uid = auth.uid() and not banned;
  insert into public.milo_room_events(room, uid, name, kind, created) values (p_room, auth.uid(), m.name, 'leave', milo_now());
end $$;

-- everything the room screen needs in ONE call; also counts as "I'm here"
create or replace function public.room_snapshot(p_room uuid, p_since bigint) returns jsonb
language plpgsql security definer set search_path = public as $$
declare m public.milo_room_members; r public.milo_rooms; n bigint := milo_now();
begin
  select * into m from public.milo_room_members where room = p_room and uid = auth.uid();
  if not found then return jsonb_build_object('gone', 'left'); end if;
  if m.banned then return jsonb_build_object('gone', 'removed'); end if;
  select * into r from public.milo_rooms where id = p_room;
  if not found or r.ended then return jsonb_build_object('gone', 'ended'); end if;
  update public.milo_room_members set last_seen = n where room = p_room and uid = auth.uid();
  return jsonb_build_object(
    'room', jsonb_build_object('id', r.id, 'name', r.name, 'code', r.code, 'public', r.is_public, 'open', r.open_queue, 'state', r.state, 'host_seen', r.host_seen, 'host', r.host),
    'me', jsonb_build_object('uid', m.uid, 'role', m.role, 'muted', m.muted),
    'members', coalesce((select jsonb_agg(jsonb_build_object('uid', x.uid, 'name', x.name, 'role', x.role, 'muted', x.muted, 'on', x.last_seen > n - 15000) order by x.joined)
                         from public.milo_room_members x where x.room = p_room and not x.banned and (x.last_seen > n - 60000 or x.role = 'host')), '[]'::jsonb),
    'queue', coalesce((select jsonb_agg(jsonb_build_object('id', q.id, 'by', q.by_name, 'uid', q.added_by, 'track', q.track, 'status', q.status) order by q.id)
                       from public.milo_room_queue q where q.room = p_room and q.status <> 'played'
                         and (q.status = 'queued' or m.role = 'host' or q.added_by = auth.uid())), '[]'::jsonb),
    'events', coalesce((select jsonb_agg(jsonb_build_object('id', e.id, 'uid', e.uid, 'name', e.name, 'kind', e.kind, 'body', e.body, 'at', e.created) order by e.id)
                        from (select * from public.milo_room_events where room = p_room and id > p_since order by id desc limit 40) e), '[]'::jsonb)
  );
end $$;

create or replace function public.host_push(p_room uuid, p_state jsonb) returns void
language plpgsql security definer set search_path = public as $$
begin
  update public.milo_rooms set state = coalesce(p_state, '{}'::jsonb), host_seen = milo_now() where id = p_room and host = auth.uid() and not ended;
end $$;

create or replace function public.suggest_track(p_room uuid, p_track jsonb) returns void
language plpgsql security definer set search_path = public as $$
declare m public.milo_room_members; r public.milo_rooms;
begin
  m := public.milo_member(p_room);
  if m.muted then raise exception 'you are muted'; end if;
  select * into r from public.milo_rooms where id = p_room and not ended;
  if not found then raise exception 'session ended'; end if;
  if exists (select 1 from public.milo_room_queue where room = p_room and added_by = auth.uid() and created > milo_now() - 3000) then raise exception 'slow down'; end if;
  if (select count(*) from public.milo_room_queue where room = p_room and status <> 'played') >= 100 then raise exception 'queue is full'; end if;
  if (select count(*) from public.milo_room_queue where room = p_room and added_by = auth.uid() and status = 'suggested') >= 10 then raise exception 'too many waiting suggestions'; end if;
  insert into public.milo_room_queue(room, added_by, by_name, track, status, created)
    values (p_room, auth.uid(), m.name, p_track, case when m.role = 'host' or r.open_queue then 'queued' else 'suggested' end, milo_now());
end $$;

create or replace function public.queue_action(p_id bigint, p_action text) returns void
language plpgsql security definer set search_path = public as $$
declare q public.milo_room_queue;
begin
  select * into q from public.milo_room_queue where id = p_id;
  if not found then return; end if;
  if not exists (select 1 from public.milo_rooms where id = q.room and host = auth.uid()) then raise exception 'host only'; end if;
  if p_action = 'approve' then update public.milo_room_queue set status = 'queued' where id = p_id;
  elsif p_action = 'played' then update public.milo_room_queue set status = 'played' where id = p_id;
  elsif p_action = 'remove' then delete from public.milo_room_queue where id = p_id;
  end if;
end $$;

create or replace function public.post_event(p_room uuid, p_kind text, p_body text) returns void
language plpgsql security definer set search_path = public as $$
declare m public.milo_room_members;
begin
  m := public.milo_member(p_room);
  if m.muted then raise exception 'you are muted'; end if;
  if p_kind not in ('react', 'msg') then raise exception 'bad kind'; end if;
  if p_kind = 'react' and p_body not in ('fire', 'heart', 'clap', 'laugh', 'music', 'wow') then raise exception 'bad reaction'; end if;
  if exists (select 1 from public.milo_room_events where room = p_room and uid = auth.uid() and created > milo_now() - 700) then raise exception 'slow down'; end if;
  insert into public.milo_room_events(room, uid, name, kind, body, created) values (p_room, auth.uid(), m.name, p_kind, milo_clean(p_body, 140), milo_now());
end $$;

create or replace function public.moderate(p_room uuid, p_target uuid, p_action text) returns void
language plpgsql security definer set search_path = public as $$
begin
  if not exists (select 1 from public.milo_rooms where id = p_room and host = auth.uid()) then raise exception 'host only'; end if;
  if p_target = auth.uid() then raise exception 'not yourself'; end if;
  if p_action = 'mute' then update public.milo_room_members set muted = true where room = p_room and uid = p_target;
  elsif p_action = 'unmute' then update public.milo_room_members set muted = false where room = p_room and uid = p_target;
  elsif p_action = 'kick' then delete from public.milo_room_members where room = p_room and uid = p_target;
  elsif p_action = 'ban' then update public.milo_room_members set banned = true where room = p_room and uid = p_target;
  end if;
end $$;

create or replace function public.end_room(p_room uuid) returns void
language plpgsql security definer set search_path = public as $$
begin update public.milo_rooms set ended = true where id = p_room and host = auth.uid(); end $$;

create or replace function public.report_user(p_room uuid, p_target uuid, p_reason text) returns void
language plpgsql security definer set search_path = public as $$
begin
  perform public.milo_member(p_room);
  if exists (select 1 from public.milo_reports where reporter = auth.uid() and target = p_target and created > milo_now() - 60000) then return; end if;
  insert into public.milo_reports(reporter, room, target, reason, created) values (auth.uid(), p_room, p_target, milo_clean(p_reason, 200), milo_now());
end $$;

create or replace function public.public_rooms() returns jsonb
language sql stable security definer set search_path = public as $$
  select coalesce(jsonb_agg(jsonb_build_object('name', r.name, 'code', r.code, 'host', (select name from public.milo_room_members h where h.room = r.id and h.role = 'host'),
         'listeners', (select count(*) from public.milo_room_members x where x.room = r.id and not x.banned and x.last_seen > milo_now() - 20000),
         'now', r.state -> 't' ->> 'title') order by r.host_seen desc), '[]'::jsonb)
  from public.milo_rooms r where r.is_public and not r.ended and r.host_seen > milo_now() - 120000 and auth.uid() is not null;
$$;

revoke all on function public.milo_member(uuid) from public, anon;
grant execute on function public.milo_member(uuid) to authenticated;
-- only signed-in users may call these; nobody else
do $$ declare f text; begin
  foreach f in array array['create_room(text,boolean,boolean,text)','join_room(text,text)','leave_room(uuid)','room_snapshot(uuid,bigint)','host_push(uuid,jsonb)',
    'suggest_track(uuid,jsonb)','queue_action(bigint,text)','post_event(uuid,text,text)','moderate(uuid,uuid,text)','end_room(uuid)','report_user(uuid,uuid,text)','public_rooms()']
  loop execute format('revoke all on function public.%s from public, anon', f); execute format('grant execute on function public.%s to authenticated', f); end loop;
end $$;
-- reports are read by you in the Supabase table editor (public.milo_reports): the app cannot read them.
