-- Users
CREATE TABLE users (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username    VARCHAR(50)  NOT NULL UNIQUE,
    email       VARCHAR(255) NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,          -- bcrypt
    role        VARCHAR(20)  NOT NULL DEFAULT 'SOLVER', -- SOLVER | ADMIN
    solved_count INT         NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Problems
CREATE TABLE problems (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    slug         VARCHAR(100) NOT NULL UNIQUE,  -- "two-sum"
    title        VARCHAR(255) NOT NULL,
    difficulty   VARCHAR(10)  NOT NULL,          -- EASY | MEDIUM | HARD
    description  TEXT         NOT NULL,          -- Markdown
    constraints  TEXT,                           -- Markdown
    time_limit_ms   INT NOT NULL DEFAULT 2000,
    memory_limit_mb INT NOT NULL DEFAULT 256,
    is_published BOOLEAN      NOT NULL DEFAULT false,
    created_by   UUID REFERENCES users(id),
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Topic Tags
CREATE TABLE tags (
    id   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(50) NOT NULL UNIQUE             -- "Dynamic Programming"
);

CREATE TABLE problem_tags (
    problem_id UUID REFERENCES problems(id) ON DELETE CASCADE,
    tag_id     UUID REFERENCES tags(id)     ON DELETE CASCADE,
    PRIMARY KEY (problem_id, tag_id)
);

-- Examples (visible, part of problem statement)
CREATE TABLE examples (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    problem_id  UUID REFERENCES problems(id) ON DELETE CASCADE,
    input       TEXT NOT NULL,
    output      TEXT NOT NULL,
    explanation TEXT,
    order_index INT  NOT NULL DEFAULT 0
);

-- Test Cases (hidden, used by judge)
CREATE TABLE test_cases (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    problem_id     UUID REFERENCES problems(id) ON DELETE CASCADE,
    input          TEXT    NOT NULL,
    expected_output TEXT   NOT NULL,
    is_hidden      BOOLEAN NOT NULL DEFAULT true,
    order_index    INT     NOT NULL DEFAULT 0
);

-- Submissions
CREATE TABLE submissions (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    problem_id   UUID REFERENCES problems(id),
    user_id      UUID REFERENCES users(id),
    language     VARCHAR(20)  NOT NULL,           -- JAVA | PYTHON
    code         TEXT         NOT NULL,
    status       VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    -- PENDING | RUNNING | ACCEPTED | WRONG_ANSWER
    -- TIME_LIMIT_EXCEEDED | MEMORY_LIMIT_EXCEEDED
    -- COMPILE_ERROR | RUNTIME_ERROR
    runtime_ms   INT,
    memory_mb    INT,
    passed_cases INT,
    total_cases  INT,
    error_output TEXT,                            -- CE/RE message
    submitted_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    judged_at    TIMESTAMPTZ
);

-- Outbox (Transactional Outbox Pattern — you know this from SCM)
CREATE TABLE outbox_events (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_id UUID        NOT NULL,            -- submission_id
    event_type   VARCHAR(100) NOT NULL,           -- SubmissionCreatedEvent
    payload      JSONB        NOT NULL,
    published    BOOLEAN      NOT NULL DEFAULT false,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);