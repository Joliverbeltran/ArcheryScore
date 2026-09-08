-- ArcheryScore initial schema (contracts/storage.md)
-- Source of truth: Supabase. Local Room mirrors this.

create table public.sessions (
  id              uuid primary key,
  user_id         uuid not null references auth.users(id) on delete cascade,
  date            timestamptz not null default now(),
  round_type      text not null check (round_type in ('TEN_ZONE','FIVE_ZONE')),
  distance_m      int  not null check (distance_m between 10 and 300),
  discipline      text not null check (discipline in
                    ('OLYMPIC_RECURVE','TRADITIONAL_RECURVE','BAREBOW','LONGBOW','COMPOUND')),
  end_count       int  not null check (end_count between 1 and 30),
  arrows_per_end  int  not null check (arrows_per_end between 1 and 12),
  notes           text,
  status          text not null default 'ACTIVE' check (status in ('ACTIVE','COMPLETE')),
  created_at      timestamptz not null default now(),
  updated_at      timestamptz not null default now(),
  last_synced_at  timestamptz
);

create unique index sessions_one_active_per_user
  on public.sessions (user_id) where (status = 'ACTIVE');

create table public.ends (
  id         uuid primary key,
  session_id uuid not null references public.sessions(id) on delete cascade,
  end_number int  not null check (end_number >= 1),
  created_at timestamptz not null default now(),
  unique (session_id, end_number)
);
create index ends_session_idx on public.ends (session_id);

create table public.arrows (
  id           uuid primary key,
  end_id       uuid not null references public.ends(id) on delete cascade,
  arrow_number int  not null check (arrow_number >= 1),
  score        int  not null check (score between 1 and 10),
  is_x_ring    boolean not null default false,
  edited_at    timestamptz not null default now()
);
create index arrows_end_idx on public.arrows (end_id);

create table public.prefs (
  id                      uuid primary key default gen_random_uuid(),
  user_id                 uuid not null references auth.users(id) on delete cascade,
  default_round_type      text not null default 'TEN_ZONE',
  default_end_count       int  not null default 6,
  default_arrows_per_end  int  not null default 3,
  default_distance_m      int  not null default 18,
  default_discipline      text not null default 'OLYMPIC_RECURVE',
  count_x_rings_deeply    boolean not null default false,
  unique (user_id)
);

-- Row Level Security
alter table public.sessions enable row level security;
alter table public.ends     enable row level security;
alter table public.arrows   enable row level security;
alter table public.prefs    enable row level security;

create policy "own sessions" on public.sessions for all
  using (auth.uid() = user_id) with check (auth.uid() = user_id);
create policy "own ends" on public.ends for all
  using (exists (select 1 from public.sessions s
                 where s.id = session_id and s.user_id = auth.uid()))
  with check (exists (select 1 from public.sessions s
                 where s.id = session_id and s.user_id = auth.uid()));
create policy "own arrows" on public.arrows for all
  using (exists (select 1 from public.ends e
                 join public.sessions s on s.id = e.session_id
                 where e.id = end_id and s.user_id = auth.uid()))
  with check (exists (select 1 from public.ends e
                 join public.sessions s on s.id = e.session_id
                 where e.id = end_id and s.user_id = auth.uid()));
create policy "own prefs" on public.prefs for all
  using (auth.uid() = user_id) with check (auth.uid() = user_id);