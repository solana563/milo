-- milo. shared (collaborative) playlists. Run ONCE in the Supabase SQL editor.
-- Same security model as Sessions: Row Level Security ON with NO policies, so the app never touches the tables directly.
-- Every action goes through a function that checks who you are and your role (owner / editor / viewer).
-- Only online (YouTube) songs can be in a shared playlist: songs stored on one phone can't play on another.

create or replace function public.milo_now() returns bigint language sql stable as $$ select (extract(epoch from clock_timestamp()) * 1000)::bigint $$;
create or replace function public.milo_clean(p text, n int) returns text language sql immutable as $$
  select left(regexp_replace(coalesce(p, ''), '[\x00-\x1f]', '', 'g'), n)
$$;

create table if not exists public.milo_cpl (
  id uuid primary key default gen_random_uuid(),
  owner uuid not null references auth.users(id) on delete cascade,
  name text not null,
  code text not null unique,
  is_public boolean not null default false,
  default_role text not null default 'editor' check (default_role in ('editor', 'viewer')),   -- what people get when they join
  created bigint not null,
  updated bigint not null
);
create table if not exists public.milo_cpl_members (
  pl uuid not null references public.milo_cpl(id) on delete cascade,
  uid uuid not null references auth.users(id) on delete cascade,
  name text not null,
  role text not null check (role in ('owner', 'editor', 'viewer')),
  joined bigint not null,
  primary key (pl, uid)
);
create table if not exists public.milo_cpl_items (
  id bigint generated always as identity primary key,
  pl uuid not null references public.milo_cpl(id) on delete cascade,
  track jsonb not null,
  track_key text not null,
  pos double precision not null,
  added_by uuid not null references auth.users(id) on delete cascade,
  by_name text not null default '',
  created bigint not null
);
create table if not exists public.milo_cpl_history (
  id bigint generated always as identity primary key,
  pl uuid not null references public.milo_cpl(id) on delete cascade,
  uid uuid not null references auth.users(id) on delete cascade,
  name text not null default '',
  action text not null,
  detail text not null default '',
  created bigint not null
);
alter table public.milo_cpl enable row level security;
alter table public.milo_cpl_members enable row level security;
alter table public.milo_cpl_items enable row level security;
alter table public.milo_cpl_history enable row level security;
create index if not exists milo_cpl_items_pl on public.milo_cpl_items(pl, pos);
create index if not exists milo_cpl_history_pl on public.milo_cpl_history(pl, id);

-- the caller's role in a playlist, or an error
create or replace function public.cpl_role(p_pl uuid) returns text
language plpgsql stable security definer set search_path = public as $$
declare r text;
begin
  select role into r from public.milo_cpl_members where pl = p_pl and uid = auth.uid();
  if r is null then raise exception 'not in this playlist'; end if;
  return r;
end $$;

create or replace function public.cpl_log(p_pl uuid, p_name text, p_action text, p_detail text) returns void
language sql security definer set search_path = public as $$
  insert into public.milo_cpl_history(pl, uid, name, action, detail, created) values (p_pl, auth.uid(), p_name, p_action, left(p_detail, 120), public.milo_now());
$$;

create or replace function public.cpl_create(p_name text, p_public boolean, p_default_role text, p_my_name text) returns jsonb
language plpgsql security definer set search_path = public as $$
declare pid uuid; c text; alphabet text := 'ABCDEFGHJKMNPQRSTUVWXYZ23456789'; i int;
begin
  if auth.uid() is null then raise exception 'sign in first'; end if;
  if (select count(*) from public.milo_cpl where owner = auth.uid()) >= 20 then raise exception 'you own too many shared playlists'; end if;
  loop
    c := ''; for i in 1..6 loop c := c || substr(alphabet, 1 + floor(random() * length(alphabet))::int, 1); end loop;
    exit when not exists (select 1 from public.milo_cpl where code = c);
  end loop;
  insert into public.milo_cpl(owner, name, code, is_public, default_role, created, updated)
    values (auth.uid(), coalesce(nullif(milo_clean(p_name, 60), ''), 'Shared playlist'), c, coalesce(p_public, false),
            case when p_default_role = 'viewer' then 'viewer' else 'editor' end, milo_now(), milo_now()) returning id into pid;
  insert into public.milo_cpl_members(pl, uid, name, role, joined) values (pid, auth.uid(), coalesce(nullif(milo_clean(p_my_name, 24), ''), 'Owner'), 'owner', milo_now());
  return jsonb_build_object('id', pid, 'code', c);
end $$;

create or replace function public.cpl_join(p_code text, p_my_name text) returns jsonb
language plpgsql security definer set search_path = public as $$
declare p public.milo_cpl; existed boolean; nm text;
begin
  if auth.uid() is null then raise exception 'sign in first'; end if;
  select * into p from public.milo_cpl where code = upper(trim(p_code));
  if not found then raise exception 'no such playlist'; end if;
  existed := exists (select 1 from public.milo_cpl_members where pl = p.id and uid = auth.uid());
  if not existed and (select count(*) from public.milo_cpl_members where pl = p.id) >= 50 then raise exception 'this playlist is full'; end if;
  nm := coalesce(nullif(milo_clean(p_my_name, 24), ''), 'Listener');
  insert into public.milo_cpl_members(pl, uid, name, role, joined) values (p.id, auth.uid(), nm, p.default_role, milo_now()) on conflict (pl, uid) do update set name = excluded.name;
  if not existed then perform public.cpl_log(p.id, nm, 'join', ''); end if;
  return jsonb_build_object('id', p.id, 'code', p.code);
end $$;

create or replace function public.cpl_leave(p_pl uuid) returns void
language plpgsql security definer set search_path = public as $$
begin
  if public.cpl_role(p_pl) = 'owner' then raise exception 'the owner can delete the playlist instead'; end if;
  delete from public.milo_cpl_members where pl = p_pl and uid = auth.uid();
end $$;

create or replace function public.cpl_list() returns jsonb
language sql stable security definer set search_path = public as $$
  select coalesce(jsonb_agg(jsonb_build_object('id', p.id, 'name', p.name, 'code', p.code, 'role', m.role, 'public', p.is_public,
           'count', (select count(*) from public.milo_cpl_items i where i.pl = p.id), 'updated', p.updated) order by p.updated desc), '[]'::jsonb)
  from public.milo_cpl_members m join public.milo_cpl p on p.id = m.pl where m.uid = auth.uid();
$$;

create or replace function public.cpl_public() returns jsonb
language sql stable security definer set search_path = public as $$
  select coalesce(jsonb_agg(jsonb_build_object('name', p.name, 'code', p.code, 'owner', (select name from public.milo_cpl_members o where o.pl = p.id and o.role = 'owner'),
           'count', (select count(*) from public.milo_cpl_items i where i.pl = p.id)) order by p.updated desc), '[]'::jsonb)
  from (select * from public.milo_cpl where is_public order by updated desc limit 30) p where auth.uid() is not null;
$$;

create or replace function public.cpl_get(p_pl uuid) returns jsonb
language plpgsql security definer set search_path = public as $$
declare p public.milo_cpl; r text;
begin
  r := public.cpl_role(p_pl);
  select * into p from public.milo_cpl where id = p_pl;
  return jsonb_build_object(
    'pl', jsonb_build_object('id', p.id, 'name', p.name, 'code', p.code, 'public', p.is_public, 'default_role', p.default_role, 'owner', p.owner),
    'me', jsonb_build_object('uid', auth.uid(), 'role', r),
    'members', (select coalesce(jsonb_agg(jsonb_build_object('uid', m.uid, 'name', m.name, 'role', m.role) order by m.joined), '[]'::jsonb) from public.milo_cpl_members m where m.pl = p_pl),
    'items', (select coalesce(jsonb_agg(jsonb_build_object('id', i.id, 'track', i.track, 'by', i.by_name, 'uid', i.added_by, 'at', i.created) order by i.pos), '[]'::jsonb) from public.milo_cpl_items i where i.pl = p_pl),
    'history', (select coalesce(jsonb_agg(jsonb_build_object('name', h.name, 'action', h.action, 'detail', h.detail, 'at', h.created) order by h.id desc), '[]'::jsonb)
                from (select * from public.milo_cpl_history where pl = p_pl order by id desc limit 50) h)
  );
end $$;

create or replace function public.cpl_add(p_pl uuid, p_track jsonb) returns void
language plpgsql security definer set search_path = public as $$
declare r text; nm text; k text; np double precision;
begin
  r := public.cpl_role(p_pl);
  if r = 'viewer' then raise exception 'you can only view this playlist'; end if;
  k := coalesce(p_track ->> 'uri', '');
  if k not like 'yt:%' then raise exception 'only online songs can be shared'; end if;
  if exists (select 1 from public.milo_cpl_items where pl = p_pl and track_key = k) then raise exception 'already in the playlist'; end if;
  if (select count(*) from public.milo_cpl_items where pl = p_pl) >= 500 then raise exception 'playlist is full (500 songs)'; end if;
  if exists (select 1 from public.milo_cpl_items where pl = p_pl and added_by = auth.uid() and created > milo_now() - 800) then raise exception 'slow down'; end if;
  select name into nm from public.milo_cpl_members where pl = p_pl and uid = auth.uid();
  select coalesce(max(pos), 0) + 1 into np from public.milo_cpl_items where pl = p_pl;
  insert into public.milo_cpl_items(pl, track, track_key, pos, added_by, by_name, created) values (p_pl, p_track, k, np, auth.uid(), nm, milo_now());
  update public.milo_cpl set updated = milo_now() where id = p_pl;
  perform public.cpl_log(p_pl, nm, 'add', coalesce(p_track ->> 'title', ''));
end $$;

-- owner removes anything; an editor removes only what they added
create or replace function public.cpl_remove(p_item bigint) returns void
language plpgsql security definer set search_path = public as $$
declare i public.milo_cpl_items; r text; nm text;
begin
  select * into i from public.milo_cpl_items where id = p_item;
  if not found then return; end if;
  r := public.cpl_role(i.pl);
  if r = 'viewer' or (r = 'editor' and i.added_by <> auth.uid()) then raise exception 'not allowed'; end if;
  select name into nm from public.milo_cpl_members where pl = i.pl and uid = auth.uid();
  delete from public.milo_cpl_items where id = p_item;
  update public.milo_cpl set updated = milo_now() where id = i.pl;
  perform public.cpl_log(i.pl, nm, 'remove', coalesce(i.track ->> 'title', ''));
end $$;

-- put p_item right after p_after (null = first). Owners and editors can reorder.
create or replace function public.cpl_move(p_item bigint, p_after bigint) returns void
language plpgsql security definer set search_path = public as $$
declare i public.milo_cpl_items; r text; nm text; a double precision; b double precision; np double precision;
begin
  select * into i from public.milo_cpl_items where id = p_item;
  if not found then return; end if;
  r := public.cpl_role(i.pl);
  if r = 'viewer' then raise exception 'not allowed'; end if;
  if p_after is null then
    select coalesce(min(pos), 1) - 1 into np from public.milo_cpl_items where pl = i.pl and id <> p_item;
  else
    select pos into a from public.milo_cpl_items where id = p_after and pl = i.pl;
    if a is null then raise exception 'bad position'; end if;
    select min(pos) into b from public.milo_cpl_items where pl = i.pl and id <> p_item and pos > a;
    np := case when b is null then a + 1 else (a + b) / 2 end;
  end if;
  update public.milo_cpl_items set pos = np where id = p_item;
  update public.milo_cpl set updated = milo_now() where id = i.pl;
  select name into nm from public.milo_cpl_members where pl = i.pl and uid = auth.uid();
  perform public.cpl_log(i.pl, nm, 'move', coalesce(i.track ->> 'title', ''));
end $$;

create or replace function public.cpl_set_role(p_pl uuid, p_uid uuid, p_role text) returns void
language plpgsql security definer set search_path = public as $$
begin
  if public.cpl_role(p_pl) <> 'owner' then raise exception 'owner only'; end if;
  if p_uid = auth.uid() then raise exception 'not yourself'; end if;
  if p_role = 'remove' then delete from public.milo_cpl_members where pl = p_pl and uid = p_uid;
  elsif p_role in ('editor', 'viewer') then update public.milo_cpl_members set role = p_role where pl = p_pl and uid = p_uid;
  else raise exception 'bad role'; end if;
end $$;

create or replace function public.cpl_settings(p_pl uuid, p_name text, p_public boolean, p_default_role text) returns void
language plpgsql security definer set search_path = public as $$
declare nm text;
begin
  if public.cpl_role(p_pl) <> 'owner' then raise exception 'owner only'; end if;
  update public.milo_cpl set name = coalesce(nullif(milo_clean(p_name, 60), ''), name), is_public = coalesce(p_public, is_public),
         default_role = case when p_default_role in ('editor', 'viewer') then p_default_role else default_role end, updated = milo_now() where id = p_pl;
  select name into nm from public.milo_cpl_members where pl = p_pl and uid = auth.uid();
  perform public.cpl_log(p_pl, nm, 'settings', milo_clean(p_name, 60));
end $$;

create or replace function public.cpl_delete(p_pl uuid) returns void
language plpgsql security definer set search_path = public as $$
begin
  if public.cpl_role(p_pl) <> 'owner' then raise exception 'owner only'; end if;
  delete from public.milo_cpl where id = p_pl;
end $$;

do $$ declare f text; begin
  foreach f in array array['cpl_role(uuid)','cpl_log(uuid,text,text,text)','cpl_create(text,boolean,text,text)','cpl_join(text,text)','cpl_leave(uuid)','cpl_list()','cpl_public()',
    'cpl_get(uuid)','cpl_add(uuid,jsonb)','cpl_remove(bigint)','cpl_move(bigint,bigint)','cpl_set_role(uuid,uuid,text)','cpl_settings(uuid,text,boolean,text)','cpl_delete(uuid)']
  loop execute format('revoke all on function public.%s from public, anon', f); execute format('grant execute on function public.%s to authenticated', f); end loop;
end $$;
