-- access_tokens schema

-- !Ups

CREATE TABLE access_tokens (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    token TEXT NOT NULL,
    refresh_token TEXT,
    expires_in INTEGER,
    revoked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
    scopes TEXT,
    application_id BIGINT,
    resource_owner_id BIGINT NOT NULL,
    last_used_at TIMESTAMPTZ,
    last_used_ip INET
);

CREATE INDEX idx_access_tokens_resource_owner_id
    ON access_tokens (resource_owner_id)
    WHERE resource_owner_id IS NOT NULL;

CREATE UNIQUE INDEX index_access_tokens_token
    ON access_tokens (token);

-- !Downs

DROP TABLE access_tokens;

-- applications schema

-- !Ups

CREATE TABLE applications (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    name TEXT NOT NULL,
    secret TEXT NOT NULL,
    redirect_uri TEXT NOT NULL,
    scopes TEXT DEFAULT '' NOT NULL,
    code TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
    website TEXT,
    owner_type TEXT,
    owner_id BIGINT,
    confidential BOOLEAN DEFAULT TRUE NOT NULL
);

CREATE INDEX idx_applications_owner_id_and_owner_type
    ON applications (owner_id, owner_type);

-- !Downs

DROP TABLE applications;
