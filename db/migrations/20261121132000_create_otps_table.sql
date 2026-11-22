-- migrate:up
CREATE TABLE otps (
    id BIGSERIAL PRIMARY KEY,
    email TEXT NOT NULL UNIQUE,
    otp_code VARCHAR(6) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- migrate:down
DROP TABLE IF EXISTS otps;
