-- Phase 3 rollback: restore legacy local-identity columns on user_credential.
-- Use only if you need to roll back from the Keycloak-only local profile schema.

ALTER TABLE user_credential
    ADD COLUMN IF NOT EXISTS first_name VARCHAR(255) NULL,
    ADD COLUMN IF NOT EXISTS last_name VARCHAR(255) NULL,
    ADD COLUMN IF NOT EXISTS email VARCHAR(255) NULL,
    ADD COLUMN IF NOT EXISTS phone VARCHAR(255) NULL,
    ADD COLUMN IF NOT EXISTS password VARCHAR(255) NULL,
    ADD COLUMN IF NOT EXISTS role_id BIGINT NULL,
    ADD COLUMN IF NOT EXISTS email_verified BIT(1) NOT NULL DEFAULT b'0';

-- Optional helpers for compatibility with old code paths (run if needed).
-- CREATE INDEX ix_user_credential_email ON user_credential (email);
-- CREATE INDEX ix_user_credential_phone ON user_credential (phone);
-- CREATE INDEX ix_user_credential_role_id ON user_credential (role_id);

-- Optional FK restore (adjust role table/column types if your schema differs).
-- ALTER TABLE user_credential
--   ADD CONSTRAINT fk_user_credential_role
--   FOREIGN KEY (role_id) REFERENCES role(id);
