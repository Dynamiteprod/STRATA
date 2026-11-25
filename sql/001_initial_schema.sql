-- STRATA PostgreSQL 16 Schema + Initial Migration (Livrable 2 - Section 9 & 6.3)
-- Conventions: snake_case identifiers, ULID prefixed text PKs, timestamptz, bigint minor units (XOF centimes)

CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE users (
    id TEXT PRIMARY KEY CHECK (id ~ '^usr_'),
    phone TEXT UNIQUE,
    email TEXT UNIQUE,
    display_name TEXT NOT NULL,
    country CHAR(2) NOT NULL DEFAULT 'CI',
    locale VARCHAR(10) NOT NULL DEFAULT 'fr',
    plan VARCHAR(20) NOT NULL DEFAULT 'free',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE payout_accounts (
    id TEXT PRIMARY KEY CHECK (id ~ '^pay_'),
    party_id TEXT NOT NULL,
    method VARCHAR(20) NOT NULL CHECK (method IN ('orange_money', 'mtn_momo', 'moov', 'wave', 'bank')),
    masked_reference TEXT NOT NULL,
    encrypted_destination BYTEA NOT NULL,
    verified BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE parties (
    id TEXT PRIMARY KEY CHECK (id ~ '^pty_'),
    kind VARCHAR(16) NOT NULL CHECK (kind IN ('person', 'company', 'cmo')),
    name TEXT NOT NULL,
    cmo_affiliation TEXT,
    payout_account_id TEXT REFERENCES payout_accounts(id)
);

CREATE TABLE artists (
    id TEXT PRIMARY KEY CHECK (id ~ '^art_'),
    name TEXT NOT NULL,
    slug TEXT NOT NULL UNIQUE,
    verified BOOLEAN NOT NULL DEFAULT false,
    owner_user_id TEXT NOT NULL REFERENCES users(id),
    payout_account_id TEXT REFERENCES payout_accounts(id)
);

CREATE TABLE works (
    id TEXT PRIMARY KEY CHECK (id ~ '^wrk_'),
    title TEXT NOT NULL,
    iswc TEXT UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE work_splits (
    id TEXT PRIMARY KEY,
    work_id TEXT NOT NULL REFERENCES works(id) ON DELETE CASCADE,
    party_id TEXT NOT NULL REFERENCES parties(id),
    role VARCHAR(20) NOT NULL CHECK (role IN ('author', 'composer', 'publisher')),
    share_bp INTEGER NOT NULL CHECK (share_bp > 0 AND share_bp <= 10000),
    valid_from TIMESTAMPTZ NOT NULL DEFAULT now(),
    valid_to TIMESTAMPTZ
);

CREATE TABLE releases (
    id TEXT PRIMARY KEY CHECK (id ~ '^rel_'),
    artist_id TEXT NOT NULL REFERENCES artists(id),
    title TEXT NOT NULL,
    type VARCHAR(16) NOT NULL CHECK (type IN ('single', 'ep', 'album')),
    release_date DATE NOT NULL,
    cover_uri TEXT
);

CREATE TABLE tracks (
    id TEXT PRIMARY KEY CHECK (id ~ '^trk_'),
    work_id TEXT NOT NULL REFERENCES works(id),
    title TEXT NOT NULL,
    isrc TEXT UNIQUE,
    duration_ms INTEGER NOT NULL CHECK (duration_ms > 0),
    explicit BOOLEAN NOT NULL DEFAULT false,
    release_id TEXT REFERENCES releases(id),
    status VARCHAR(16) NOT NULL DEFAULT 'ingesting' CHECK (status IN ('ingesting', 'live', 'blocked')),
    audio_fingerprint vector(128)
);

CREATE TABLE recording_splits (
    id TEXT PRIMARY KEY,
    track_id TEXT NOT NULL REFERENCES tracks(id) ON DELETE CASCADE,
    party_id TEXT NOT NULL REFERENCES parties(id),
    role VARCHAR(20) NOT NULL CHECK (role IN ('producer', 'performer')),
    share_bp INTEGER NOT NULL CHECK (share_bp > 0 AND share_bp <= 10000),
    valid_from TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE assets (
    id TEXT PRIMARY KEY CHECK (id ~ '^ast_'),
    track_id TEXT NOT NULL REFERENCES tracks(id) ON DELETE CASCADE,
    kind VARCHAR(16) NOT NULL CHECK (kind IN ('master', 'mix', 'stem', 'waveform')),
    stem_type VARCHAR(16),
    source VARCHAR(16) NOT NULL CHECK (source IN ('official', 'ai')),
    codec VARCHAR(16) NOT NULL,
    bitrate INTEGER NOT NULL,
    uri TEXT NOT NULL,
    model_version TEXT
);

CREATE TABLE analyses (
    id TEXT PRIMARY KEY CHECK (id ~ '^anl_'),
    track_id TEXT NOT NULL REFERENCES tracks(id) ON DELETE CASCADE,
    version TEXT NOT NULL,
    json_uri TEXT NOT NULL,
    human_reviewed JSONB NOT NULL DEFAULT '{"chords":false,"sections":false,"lyrics":false}',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE licenses (
    id TEXT PRIMARY KEY CHECK (id ~ '^lic_'),
    track_id TEXT NOT NULL REFERENCES tracks(id) ON DELETE CASCADE,
    allow_stream BOOLEAN NOT NULL DEFAULT true,
    allow_stems BOOLEAN NOT NULL DEFAULT true,
    allow_loop_export BOOLEAN NOT NULL DEFAULT false,
    allow_fork BOOLEAN NOT NULL DEFAULT false,
    allow_orchestra BOOLEAN NOT NULL DEFAULT true,
    fork_split_bp INTEGER NOT NULL DEFAULT 7000 CHECK (fork_split_bp BETWEEN 1000 AND 9500),
    territories TEXT[] NOT NULL DEFAULT ARRAY['CI'],
    valid_from TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE version_links (
    id TEXT PRIMARY KEY CHECK (id ~ '^lnk_'),
    source_track_id TEXT NOT NULL REFERENCES tracks(id),
    target_track_id TEXT NOT NULL REFERENCES tracks(id),
    link_type VARCHAR(24) NOT NULL CHECK (link_type IN ('sample_of', 'cover_of', 'live_of', 'demo_of', 'remaster_of', 'remix_of', 'fork_of', 'interpolation_of')),
    provenance VARCHAR(16) NOT NULL CHECK (provenance IN ('artist', 'staff', 'community', 'auto')),
    confidence NUMERIC(4,3) NOT NULL CHECK (confidence BETWEEN 0 AND 1),
    status VARCHAR(16) NOT NULL DEFAULT 'active'
);

CREATE TABLE version_alignments (
    source_track_id TEXT NOT NULL REFERENCES tracks(id),
    target_track_id TEXT NOT NULL REFERENCES tracks(id),
    link_type VARCHAR(24) NOT NULL,
    mapping JSONB NOT NULL,
    coverage NUMERIC(4,3) NOT NULL CHECK (coverage BETWEEN 0 AND 1),
    confidence NUMERIC(4,3) NOT NULL CHECK (confidence BETWEEN 0 AND 1),
    method TEXT NOT NULL,
    computed_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    reviewed_by TEXT REFERENCES users(id),
    PRIMARY KEY (source_track_id, target_track_id, link_type)
);

CREATE TABLE forks (
    id TEXT PRIMARY KEY CHECK (id ~ '^frk_'),
    base_track_id TEXT NOT NULL REFERENCES tracks(id),
    parent_fork_id TEXT REFERENCES forks(id),
    author_user_id TEXT NOT NULL REFERENCES users(id),
    spec JSONB NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'draft' CHECK (status IN ('draft', 'published', 'hidden')),
    published_at TIMESTAMPTZ
);

CREATE TABLE orchestra_sessions (
    id TEXT PRIMARY KEY CHECK (id ~ '^orc_'),
    host_user_id TEXT NOT NULL REFERENCES users(id),
    track_id TEXT NOT NULL REFERENCES tracks(id),
    started_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    ended_at TIMESTAMPTZ,
    participant_count INTEGER NOT NULL CHECK (participant_count BETWEEN 1 AND 8)
);

CREATE TABLE play_events (
    id TEXT NOT NULL,
    user_id TEXT NOT NULL REFERENCES users(id),
    track_id TEXT NOT NULL REFERENCES tracks(id),
    fork_id TEXT REFERENCES forks(id),
    depth_level SMALLINT NOT NULL CHECK (depth_level BETWEEN 0 AND 4),
    stems_opened TEXT[] NOT NULL DEFAULT ARRAY[]::TEXT[],
    duration_ms INTEGER NOT NULL,
    context VARCHAR(16) NOT NULL CHECK (context IN ('solo', 'orchestra')),
    device_id TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (id, created_at)
) PARTITION BY RANGE (created_at);

CREATE TABLE play_events_2026_11 PARTITION OF play_events
    FOR VALUES FROM ('2026-11-01') TO ('2026-12-01');

CREATE TABLE ledger_entries (
    id TEXT PRIMARY KEY CHECK (id ~ '^ldg_'),
    event_id TEXT NOT NULL,
    account_id TEXT NOT NULL,
    amount_minor BIGINT NOT NULL CHECK (amount_minor > 0),
    currency CHAR(3) NOT NULL DEFAULT 'XOF',
    direction VARCHAR(6) NOT NULL CHECK (direction IN ('debit', 'credit')),
    rule_version VARCHAR(24) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE subscriptions (
    id TEXT PRIMARY KEY CHECK (id ~ '^sub_'),
    user_id TEXT NOT NULL REFERENCES users(id),
    plan VARCHAR(20) NOT NULL,
    provider VARCHAR(24) NOT NULL,
    status VARCHAR(16) NOT NULL,
    current_period_end TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_tracks_isrc ON tracks(isrc);
CREATE INDEX idx_ledger_event ON ledger_entries(event_id);
CREATE INDEX idx_ledger_account ON ledger_entries(account_id, created_at);
