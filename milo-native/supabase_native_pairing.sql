-- milo. Connect pairing between different accounts. Run ONCE in the Supabase SQL editor (after supabase_native_connect.sql).
-- A person creates a short code (QR or typed). Someone else enters it and may then use ONLY play/pause/next/previous/seek on
-- that person's devices, until it expires or the owner removes them. No tokens or passwords are ever shared.
-- Same security model as the other native files: tables locked with Row Level Security and no policies; every action is a checked function.

create or replace function public.milo_now() returns bigint language sql stable as $$ select (extract(epoch from clock_timestamp()) * 1000)::bigint $$;
create or replace function public.milo_clean(p text, n int) returns text language sql immutable as $$
  select left(regexp_replace(coalesce(p, ''), '[[:cntrl:]]', '', 'g'), n)
$$;

create table if not exists public.milo_pair_codes (
  code text primary key,
  owner uuid not null references auth.users(id) on delete cascade,
  owner_name text not null,
  hours int not null default 24,          -- how long the link lasts once used (0 = until removed)
  expires bigint not null,
  created bigint not null
);
create table if not exists public.milo_links (
  owner uuid not null references auth.users(id) on delete cascade,
  controller uuid not null references auth.users(id) on delete cascade,
  owner_name text not null,
  controller_name text not null,
  created bigint not null,
  expires bigint not null default 0,      -- 0 = until removed
  primary key (owner, controller)
);
create table if not exists public.milo_pair_log (
  id bigint generated always as identity primary key,
  uid uuid not null references auth.users(id) on delete cascade,
  kind text not null,                     -- 'fail' (wrong code) or 'cmd' (remote command)
  at bigint not null
);
alter table public.milo_pair_codes enable row level security;
alter table public.milo_links enable row level security;
alter table public.milo_pair_log enable row level security;
create index if not exists milo_pair_log_uid on public.milo_pair_log(uid, kind, at);

create or replace function public.pair_create(p_hours int, p_my_name text) returns jsonb
language plpgsql security definer set search_path = public as $$
declare c text; alphabet text := 'ABCDEFGHJKMNPQRSTUVWXYZ23456789'; i int; exp bigint;
begin
  if auth.uid() is null then raise exception 'sign in first'; end if;
  if exists (select 1 from public.milo_pair_codes where owner = auth.uid() and created > milo_now() - 3000) then raise exception 'slow down'; end if;
  delete from public.milo_pair_codes where owner = auth.uid() or expires < milo_now();
  loop
    c := ''; for i in 1..8 loop c := c || substr(alphabet, 1 + floor(random() * length(alphabet))::int, 1); end loop;
    exit when not exists (select 1 from public.milo_pair_codes where code = c);
  end loop;
  exp := milo_now() + 5 * 60 * 1000;      -- a code works for 5 minutes, once
  insert into public.milo_pair_codes(code, owner, owner_name, hours, expires, created)
    values (c, auth.uid(), coalesce(nullif(milo_clean(p_my_name, 24), ''), 'Someone'), greatest(0, least(coalesce(p_hours, 24), 720)), exp, milo_now());
  return jsonb_build_object('code', c, 'expires', exp);
end $$;

create or replace function public.pair_redeem(p_code text, p_my_name text) returns jsonb
language plpgsql security definer set search_path = public as $$
declare k public.milo_pair_codes; nm text;
begin
  if auth.uid() is null then raise exception 'sign in first'; end if;
  if (select count(*) from public.milo_pair_log where uid = auth.uid() and kind = 'fail' and at > milo_now() - 10 * 60 * 1000) >= 8 then
    raise exception 'too many wrong codes, try again in a few minutes';
  end if;
  select * into k from public.milo_pair_codes where code = upper(trim(p_code)) and expires > milo_now();
  if not found then
    insert into public.milo_pair_log(uid, kind, at) values (auth.uid(), 'fail', milo_now());
    raise exception 'that code is wrong or has expired';
  end if;
  if k.owner = auth.uid() then raise exception 'that is your own code'; end if;
  nm := coalesce(nullif(milo_clean(p_my_name, 24), ''), 'Someone');
  insert into public.milo_links(owner, controller, owner_name, controller_name, created, expires)
    values (k.owner, auth.uid(), k.owner_name, nm, milo_now(), case when k.hours = 0 then 0 else milo_now() + k.hours * 3600000::bigint end)
    on conflict (owner, controller) do update set controller_name = excluded.controller_name, expires = excluded.expires, created = excluded.created;
  delete from public.milo_pair_codes where code = k.code;
  return jsonb_build_object('owner', k.owner, 'name', k.owner_name);
end $$;

create or replace function public.pair_list() returns jsonb
language sql stable security definer set search_path = public as $$
  select jsonb_build_object(
    'controlling', (select coalesce(jsonb_agg(jsonb_build_object('uid', l.owner, 'name', l.owner_name, 'expires', l.expires) order by l.created desc), '[]'::jsonb)
                    from public.milo_links l where l.controller = auth.uid() and (l.expires = 0 or l.expires > public.milo_now())),
    'controllers', (select coalesce(jsonb_agg(jsonb_build_object('uid', l.controller, 'name', l.controller_name, 'expires', l.expires) order by l.created desc), '[]'::jsonb)
                    from public.milo_links l where l.owner = auth.uid() and (l.expires = 0 or l.expires > public.milo_now())));
$$;

create or replace function public.pair_revoke(p_other uuid) returns void
language sql security definer set search_path = public as $$
  delete from public.milo_links where (owner = auth.uid() and controller = p_other) or (controller = auth.uid() and owner = p_other);
$$;

create or replace function public.pair_link_ok(p_owner uuid) returns boolean
language sql stable security definer set search_path = public as $$
  select exists (select 1 from public.milo_links l where l.owner = p_owner and l.controller = auth.uid() and (l.expires = 0 or l.expires > public.milo_now()))
$$;

-- the owner's online devices and what they are playing (without the song queue)
create or replace function public.pair_devices(p_owner uuid) returns jsonb
language plpgsql stable security definer set search_path = public as $$
begin
  if not public.pair_link_ok(p_owner) then raise exception 'not linked'; end if;
  return (select coalesce(jsonb_agg(jsonb_build_object('id', d.id, 'name', d.name, 'kind', d.kind, 'last_seen', d.last_seen, 'now', d.now - 'q') order by d.last_seen desc), '[]'::jsonb)
          from public.milo_devices d where d.uid = p_owner and d.last_seen > public.milo_now() - 5 * 60 * 1000);
end $$;

-- only these commands can come from another account: no loading songs, nothing else
create or replace function public.pair_send(p_owner uuid, p_device uuid, p_cmd jsonb) returns void
language plpgsql security definer set search_path = public as $$
declare t text := p_cmd ->> 't'; nm text; c jsonb;
begin
  if not public.pair_link_ok(p_owner) then raise exception 'not linked'; end if;
  if t is null or t not in ('toggle', 'play', 'pause', 'next', 'prev', 'seekby') then raise exception 'command not allowed'; end if;
  if not exists (select 1 from public.milo_devices where id = p_device and uid = p_owner) then raise exception 'no such device'; end if;
  if (select count(*) from public.milo_pair_log where uid = auth.uid() and kind = 'cmd' and at > milo_now() - 10000) >= 20 then raise exception 'slow down'; end if;
  select controller_name into nm from public.milo_links where owner = p_owner and controller = auth.uid();
  c := jsonb_build_object('t', t, 'by', nm);
  if t = 'seekby' then c := c || jsonb_build_object('ms', greatest(-60000, least(60000, coalesce((p_cmd ->> 'ms')::bigint, 0)))); end if;
  insert into public.milo_commands(uid, target, from_device, cmd, created) values (p_owner, p_device, null, c, milo_now());
  insert into public.milo_pair_log(uid, kind, at) values (auth.uid(), 'cmd', milo_now());
  delete from public.milo_pair_log where at < milo_now() - 3600000;
end $$;

do $$ declare f text; begin
  foreach f in array array['pair_create(int,text)', 'pair_redeem(text,text)', 'pair_list()', 'pair_revoke(uuid)', 'pair_link_ok(uuid)', 'pair_devices(uuid)', 'pair_send(uuid,uuid,jsonb)']
  loop execute format('revoke all on function public.%s from public, anon', f); execute format('grant execute on function public.%s to authenticated', f); end loop;
end $$;
