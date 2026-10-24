-- ChatCode: skema Supabase. Jalankan SEKALI di SQL Editor pada project baru.

-- ========== TABEL ==========
create table public.profiles (
  id uuid primary key references auth.users (id) on delete cascade,
  username text not null unique check (username ~ '^[a-z0-9_]{3,20}$'),
  display_name text not null default '' check (char_length(display_name) <= 50),
  bio text not null default '' check (char_length(bio) <= 200),
  avatar_url text,
  created_at timestamptz not null default now()
);

create table public.follows (
  follower_id uuid not null references public.profiles (id) on delete cascade,
  following_id uuid not null references public.profiles (id) on delete cascade,
  created_at timestamptz not null default now(),
  primary key (follower_id, following_id),
  check (follower_id <> following_id)
);

create table public.posts (
  id bigint generated always as identity primary key,
  author_id uuid not null default auth.uid() references public.profiles (id) on delete cascade,
  body text not null default '' check (char_length(body) <= 2000),
  image_url text,
  created_at timestamptz not null default now(),
  check (char_length(body) > 0 or image_url is not null)
);

create table public.post_likes (
  post_id bigint not null references public.posts (id) on delete cascade,
  user_id uuid not null default auth.uid() references public.profiles (id) on delete cascade,
  created_at timestamptz not null default now(),
  primary key (post_id, user_id)
);

create table public.comments (
  id bigint generated always as identity primary key,
  post_id bigint not null references public.posts (id) on delete cascade,
  author_id uuid not null default auth.uid() references public.profiles (id) on delete cascade,
  body text not null check (char_length(body) between 1 and 1000),
  created_at timestamptz not null default now()
);

create table public.stories (
  id bigint generated always as identity primary key,
  author_id uuid not null default auth.uid() references public.profiles (id) on delete cascade,
  image_url text not null,
  created_at timestamptz not null default now()
);

create table public.conversations (
  id bigint generated always as identity primary key,
  created_by uuid default auth.uid() references public.profiles (id) on delete set null,
  created_at timestamptz not null default now()
);

create table public.conversation_members (
  conversation_id bigint not null references public.conversations (id) on delete cascade,
  user_id uuid not null references public.profiles (id) on delete cascade,
  last_read_at timestamptz not null default now(),
  primary key (conversation_id, user_id)
);

create table public.messages (
  id bigint generated always as identity primary key,
  conversation_id bigint not null references public.conversations (id) on delete cascade,
  sender_id uuid not null default auth.uid() references public.profiles (id) on delete cascade,
  kind text not null check (kind in ('text', 'voice', 'image', 'video', 'file')),
  body text not null default '' check (char_length(body) <= 4000),
  media_url text,
  file_name text,
  duration_sec integer,
  waveform jsonb,
  created_at timestamptz not null default now(),
  read_at timestamptz
);

create table public.projects (
  id bigint generated always as identity primary key,
  owner_id uuid not null default auth.uid() references public.profiles (id) on delete cascade,
  name text not null check (char_length(name) between 1 and 80),
  slug text not null,
  description text not null default '' check (char_length(description) <= 500),
  status text not null default 'ide' check (status in ('ide', 'berjalan', 'testing', 'selesai', 'ditunda', 'arsip')),
  progress integer not null default 0 check (progress between 0 and 100),
  primary_language text not null default '',
  visibility text not null default 'privat' check (visibility in ('privat', 'teman', 'publik')),
  settings jsonb not null default '{}'::jsonb,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  unique (owner_id, slug)
);

create table public.call_signals (
  id bigint generated always as identity primary key,
  conversation_id bigint not null references public.conversations (id) on delete cascade,
  from_user uuid not null default auth.uid() references public.profiles (id) on delete cascade,
  to_user uuid not null references public.profiles (id) on delete cascade,
  kind text not null check (kind in ('ring', 'offer', 'answer', 'ice', 'reject', 'end')),
  payload jsonb not null default '{}'::jsonb,
  created_at timestamptz not null default now()
);

-- ========== INDEKS ==========
create index posts_created_idx on public.posts (created_at desc);
create index posts_author_idx on public.posts (author_id, created_at desc);
create index stories_created_idx on public.stories (created_at desc);
create index comments_post_idx on public.comments (post_id, created_at);
create index messages_conv_idx on public.messages (conversation_id, created_at);
create index members_user_idx on public.conversation_members (user_id);
create index projects_owner_idx on public.projects (owner_id, updated_at desc);
create index call_signals_to_idx on public.call_signals (to_user, created_at desc);
create index follows_following_idx on public.follows (following_id);

-- ========== FUNGSI ==========
create or replace function public.is_member(cid bigint)
returns boolean
language sql
security definer
stable
set search_path = public
as $fn$
  select exists (
    select 1 from public.conversation_members
    where conversation_id = cid and user_id = auth.uid()
  );
$fn$;

create or replace function public.start_conversation(other_user uuid)
returns bigint
language plpgsql
security definer
set search_path = public
as $fn$
declare
  cid bigint;
begin
  if auth.uid() is null then
    raise exception 'Belum masuk';
  end if;
  if other_user = auth.uid() then
    raise exception 'Tidak bisa mengobrol dengan diri sendiri';
  end if;

  select m1.conversation_id into cid
  from public.conversation_members m1
  join public.conversation_members m2 on m1.conversation_id = m2.conversation_id
  where m1.user_id = auth.uid() and m2.user_id = other_user
  limit 1;

  if cid is not null then
    return cid;
  end if;

  insert into public.conversations (created_by) values (auth.uid()) returning id into cid;
  insert into public.conversation_members (conversation_id, user_id)
  values (cid, auth.uid()), (cid, other_user);
  return cid;
end;
$fn$;

create or replace function public.set_updated_at()
returns trigger
language plpgsql
as $fn$
begin
  new.updated_at = now();
  return new;
end;
$fn$;

create trigger projects_set_updated_at
before update on public.projects
for each row execute function public.set_updated_at();

create or replace function public.handle_new_user()
returns trigger
language plpgsql
security definer
set search_path = public
as $fn$
declare
  base text;
  uname text;
begin
  base := lower(regexp_replace(
    coalesce(new.raw_user_meta_data ->> 'username', split_part(coalesce(new.email, ''), '@', 1)),
    '[^a-zA-Z0-9_]', '', 'g'));
  if char_length(base) < 3 then
    base := base || 'user';
  end if;
  base := left(base, 20);
  uname := base;
  while exists (select 1 from public.profiles where username = uname) loop
    uname := left(base, 15) || floor(random() * 10000)::int::text;
  end loop;

  insert into public.profiles (id, username, display_name)
  values (
    new.id,
    uname,
    left(coalesce(nullif(new.raw_user_meta_data ->> 'display_name', ''), uname), 50)
  );
  return new;
end;
$fn$;

create trigger on_auth_user_created
after insert on auth.users
for each row execute function public.handle_new_user();

revoke all on function public.is_member(bigint) from public;
revoke all on function public.start_conversation(uuid) from public;
grant execute on function public.is_member(bigint) to authenticated;
grant execute on function public.start_conversation(uuid) to authenticated;

-- ========== RLS ==========
alter table public.profiles enable row level security;
alter table public.follows enable row level security;
alter table public.posts enable row level security;
alter table public.post_likes enable row level security;
alter table public.comments enable row level security;
alter table public.stories enable row level security;
alter table public.conversations enable row level security;
alter table public.conversation_members enable row level security;
alter table public.messages enable row level security;
alter table public.projects enable row level security;
alter table public.call_signals enable row level security;

-- profiles
create policy "profil dibaca pengguna masuk" on public.profiles
  for select to authenticated using (true);
create policy "ubah profil sendiri" on public.profiles
  for update to authenticated using (id = auth.uid()) with check (id = auth.uid());

-- follows
create policy "follow dibaca pengguna masuk" on public.follows
  for select to authenticated using (true);
create policy "ikuti sebagai diri sendiri" on public.follows
  for insert to authenticated with check (follower_id = auth.uid());
create policy "berhenti mengikuti" on public.follows
  for delete to authenticated using (follower_id = auth.uid());

-- posts
create policy "postingan dibaca pengguna masuk" on public.posts
  for select to authenticated using (true);
create policy "buat postingan sendiri" on public.posts
  for insert to authenticated with check (author_id = auth.uid());
create policy "ubah postingan sendiri" on public.posts
  for update to authenticated using (author_id = auth.uid()) with check (author_id = auth.uid());
create policy "hapus postingan sendiri" on public.posts
  for delete to authenticated using (author_id = auth.uid());

-- post_likes
create policy "suka dibaca pengguna masuk" on public.post_likes
  for select to authenticated using (true);
create policy "suka sebagai diri sendiri" on public.post_likes
  for insert to authenticated with check (user_id = auth.uid());
create policy "batal suka" on public.post_likes
  for delete to authenticated using (user_id = auth.uid());

-- comments
create policy "komentar dibaca pengguna masuk" on public.comments
  for select to authenticated using (true);
create policy "komentar sebagai diri sendiri" on public.comments
  for insert to authenticated with check (author_id = auth.uid());
create policy "hapus komentar sendiri" on public.comments
  for delete to authenticated using (author_id = auth.uid());

-- stories (hanya yang belum 24 jam)
create policy "story aktif dibaca pengguna masuk" on public.stories
  for select to authenticated
  using (created_at > now() - interval '24 hours' or author_id = auth.uid());
create policy "buat story sendiri" on public.stories
  for insert to authenticated with check (author_id = auth.uid());
create policy "hapus story sendiri" on public.stories
  for delete to authenticated using (author_id = auth.uid());

-- conversations
create policy "percakapan dibaca anggota" on public.conversations
  for select to authenticated using (public.is_member(id));

-- conversation_members
create policy "anggota dibaca sesama anggota" on public.conversation_members
  for select to authenticated using (public.is_member(conversation_id));
create policy "ubah status baca sendiri" on public.conversation_members
  for update to authenticated using (user_id = auth.uid()) with check (user_id = auth.uid());

-- messages
create policy "pesan dibaca anggota" on public.messages
  for select to authenticated using (public.is_member(conversation_id));
create policy "kirim pesan sebagai anggota" on public.messages
  for insert to authenticated
  with check (sender_id = auth.uid() and public.is_member(conversation_id));
create policy "tandai pesan terbaca" on public.messages
  for update to authenticated
  using (public.is_member(conversation_id) and sender_id <> auth.uid())
  with check (public.is_member(conversation_id) and sender_id <> auth.uid());
create policy "hapus pesan sendiri" on public.messages
  for delete to authenticated using (sender_id = auth.uid());

-- projects
create policy "proyek dibaca sesuai visibilitas" on public.projects
  for select to authenticated
  using (
    owner_id = auth.uid()
    or visibility = 'publik'
    or (
      visibility = 'teman'
      and exists (
        select 1 from public.follows a
        where a.follower_id = auth.uid() and a.following_id = projects.owner_id
      )
      and exists (
        select 1 from public.follows b
        where b.follower_id = projects.owner_id and b.following_id = auth.uid()
      )
    )
  );
create policy "buat proyek sendiri" on public.projects
  for insert to authenticated with check (owner_id = auth.uid());
create policy "ubah proyek sendiri" on public.projects
  for update to authenticated using (owner_id = auth.uid()) with check (owner_id = auth.uid());
create policy "hapus proyek sendiri" on public.projects
  for delete to authenticated using (owner_id = auth.uid());

-- call_signals
create policy "sinyal dibaca pengirim dan penerima" on public.call_signals
  for select to authenticated using (from_user = auth.uid() or to_user = auth.uid());
create policy "kirim sinyal sebagai anggota" on public.call_signals
  for insert to authenticated
  with check (from_user = auth.uid() and public.is_member(conversation_id));
create policy "hapus sinyal sendiri" on public.call_signals
  for delete to authenticated using (from_user = auth.uid() or to_user = auth.uid());

-- ========== REALTIME ==========
alter publication supabase_realtime add table
  public.posts, public.stories, public.messages,
  public.conversation_members, public.call_signals;

-- ========== STORAGE ==========
insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types) values
  ('avatars', 'avatars', true, 1048576, array['image/jpeg', 'image/webp', 'image/png']),
  ('posts', 'posts', true, 2097152, array['image/jpeg', 'image/webp', 'image/png']),
  ('stories', 'stories', true, 2097152, array['image/jpeg', 'image/webp', 'image/png'])
on conflict (id) do nothing;

insert into storage.buckets (id, name, public, file_size_limit) values
  ('chat', 'chat', false, 12582912)
on conflict (id) do nothing;

create policy "bucket publik dapat dibaca" on storage.objects
  for select to authenticated using (bucket_id in ('avatars', 'posts', 'stories'));
create policy "unggah ke folder sendiri" on storage.objects
  for insert to authenticated
  with check (
    bucket_id in ('avatars', 'posts', 'stories')
    and (storage.foldername(name))[1] = auth.uid()::text
  );
create policy "ubah file sendiri" on storage.objects
  for update to authenticated
  using (
    bucket_id in ('avatars', 'posts', 'stories')
    and (storage.foldername(name))[1] = auth.uid()::text
  );
create policy "hapus file sendiri" on storage.objects
  for delete to authenticated
  using (
    bucket_id in ('avatars', 'posts', 'stories')
    and (storage.foldername(name))[1] = auth.uid()::text
  );

-- chat: path file harus "<id_percakapan>/<nama_file>"
create policy "media chat dibaca anggota" on storage.objects
  for select to authenticated
  using (
    bucket_id = 'chat'
    and case
      when (storage.foldername(name))[1] ~ '^[0-9]+$'
        then public.is_member(((storage.foldername(name))[1])::bigint)
      else false
    end
  );
create policy "media chat diunggah anggota" on storage.objects
  for insert to authenticated
  with check (
    bucket_id = 'chat'
    and case
      when (storage.foldername(name))[1] ~ '^[0-9]+$'
        then public.is_member(((storage.foldername(name))[1])::bigint)
      else false
    end
  );

-- ========== PEMBERSIH STORY 24 JAM (opsional, butuh pg_cron) ==========
do $do$
begin
  create extension if not exists pg_cron;
  perform cron.schedule(
    'hapus-story-kedaluwarsa',
    '*/30 * * * *',
    $job$delete from public.stories where created_at < now() - interval '24 hours'$job$
  );
exception when others then
  raise notice 'pg_cron tidak tersedia, story kedaluwarsa tetap tersaring lewat query: %', sqlerrm;
end
$do$;
