-- !Ups

CREATE UNIQUE INDEX unique_username_null_domain
ON accounts (username)
WHERE domain IS NULL;

-- !Downs

DROP INDEX IF EXISTS unique_username_null_domain;