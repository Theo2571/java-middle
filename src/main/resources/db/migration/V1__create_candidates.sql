CREATE TABLE candidates (
    id          VARCHAR(150)  NOT NULL PRIMARY KEY,
    name        VARCHAR(100)  NOT NULL,
    email       VARCHAR(255)  NOT NULL,
    phone       VARCHAR(30),
    position    VARCHAR(100)  NOT NULL,
    pos_label   VARCHAR(255),
    city        VARCHAR(100),
    telegram    VARCHAR(100),
    total_exp   VARCHAR(50),
    stack       TEXT,
    education   TEXT,
    verdict     VARCHAR(20)   NOT NULL,
    status      VARCHAR(20)   NOT NULL DEFAULT 'NEW',
    summary     TEXT,
    criteria    JSONB         NOT NULL DEFAULT '[]',
    experience  JSONB         NOT NULL DEFAULT '[]',
    questions   JSONB         NOT NULL DEFAULT '[]',
    parsed_at   TIMESTAMPTZ,
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX ux_candidates_email ON candidates (email);
CREATE INDEX ix_candidates_verdict ON candidates (verdict);
CREATE INDEX ix_candidates_status ON candidates (status);
CREATE INDEX ix_candidates_position ON candidates (position);
CREATE INDEX ix_candidates_created_at ON candidates (created_at);
