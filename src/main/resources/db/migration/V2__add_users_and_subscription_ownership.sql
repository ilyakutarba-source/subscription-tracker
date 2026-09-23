CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(320) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    display_name VARCHAR(120) NOT NULL,
    role VARCHAR(20) NOT NULL,
    enabled BOOLEAN NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT chk_users_email_normalized CHECK (email = LOWER(email)),
    CONSTRAINT chk_users_role CHECK (role IN ('USER', 'ADMIN'))
);

-- This disabled account owns rows created before accounts existed. It has no
-- usable credential. Optional startup bootstrap variables can activate it once.
INSERT INTO users (id, email, password_hash, display_name, role, enabled, created_at, updated_at)
VALUES ('00000000-0000-0000-0000-000000000001', 'legacy-owner@invalid.local',
        '!disabled-legacy-account!', 'Legacy owner', 'ADMIN', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

ALTER TABLE subscriptions ADD COLUMN user_id UUID;

UPDATE subscriptions
SET user_id = '00000000-0000-0000-0000-000000000001'
WHERE user_id IS NULL;

ALTER TABLE subscriptions ALTER COLUMN user_id SET NOT NULL;
ALTER TABLE subscriptions
    ADD CONSTRAINT fk_subscriptions_user
    FOREIGN KEY (user_id) REFERENCES users (id);

CREATE INDEX idx_subscriptions_user_created_at
    ON subscriptions (user_id, created_at DESC);
CREATE INDEX idx_subscriptions_user_status
    ON subscriptions (user_id, status);
CREATE INDEX idx_subscriptions_user_status_next_payment
    ON subscriptions (user_id, status, next_payment_date);
