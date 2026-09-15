# Contract: Supabase Storage Schema & RLS

**Spec ref**: FR-004, FR-016, FR-017 | Constitution III (Supabase = source of truth), II (credentials never in VCS)

## Schema (PostgreSQL / Supabase)

```sql
create table public.sessions (
  id             uuid primary key,
  user_id        uuid not null references auth.users(id) on delete cascade,
  date           timestamptz not null default now(),
  round_type     text not null check (round_type in ('TEN_ZONE','FIVE_ZONE')),
  distance_m     int  not null check (distance_m between 10 and 300),
  discipline     text not null check (discipline in
                   ('OLYMPIC_RECURVE','TRADITIONAL_RECURVE','BAREBOW','LONGBOW','COMPOUND')),
  end_count      int  not null check (end_count between 1 and 30),
  arrows_per_end int  not null check (arrows_per_end between 1 and 12),
  notes          text,
  status         text not null default 'ACTIVE' check (status in ('ACTIVE','COMPLETE')),
  created_at     timestamptz not null default now(),
  updated_at     timestamptz not null default now(),
  last_synced_at timestamptz
);
-- one ACTIVE session per user (FR-015) — partial unique index
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
  id          uuid primary key,
  end_id      uuid not null references public.ends(id) on delete cascade,
  arrow_number int not null check (arrow_number >= 1),
  score       int  not null check (score between 1 and 10),
  is_x_ring   boolean not null default false,
  edited_at   timestamptz not null default now()  -- LWW key (FR-016)
);
create index arrows_end_idx on public.arrows (end_id);

-- Domain-side copy for reads; "queue" lives only in Room (outbox), not in Supabase.
```

**Note**: `score between 1 and 10` is the DB upper bound; app-level validation enforces `max(roundType)` (FR-002) and the X-ring rule (`is_x_ring => score = 10`). `SYNC_WRITE` from data-model.md is a local-only Room table; it is NOT in Supabase.

## Row Level Security

```sql
alter table public.sessions enable row level security;
alter table public.ends     enable row level security;
alter table public.arrows   enable row level security;

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
```

- `user_id` is always bound from the authenticated session; the client never trusts a caller-supplied `user_id`.
- Also enable a `public.users_preferences` table for `PREFERENCES` (same `user_id` pattern) if v1 ships preferences sync; otherwise local-only DataStore (see data-model.md note).

## Credentials

- `SUPABASE_URL`, `SUPABASE_ANON_KEY` live in `local.properties` (git-ignored) per constitution II. Release builds reference the real project URL; test builds use a dedicated Supabase test project.
- No secrets in the repo: `local.properties` + `.env` are gitignored; CI injects via GH Actions secrets.

## Migration strategy

- Schema defined as idempotent SQL in `supabase/migrations/` applied via Supabase CLI; AppState.migrations tracked in the Supabase project, not the device.