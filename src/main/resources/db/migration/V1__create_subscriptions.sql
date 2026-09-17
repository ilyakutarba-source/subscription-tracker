CREATE TABLE subscriptions (
    id UUID PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(1000),
    price NUMERIC(19, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    billing_period VARCHAR(20) NOT NULL,
    start_date DATE NOT NULL,
    next_payment_date DATE NOT NULL,
    category VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT chk_subscriptions_price_positive CHECK (price > 0),
    CONSTRAINT chk_subscriptions_currency CHECK (currency ~ '^[A-Z]{3}$'),
    CONSTRAINT chk_subscriptions_billing_period CHECK (billing_period IN ('MONTHLY', 'YEARLY')),
    CONSTRAINT chk_subscriptions_category CHECK (category IN
        ('ENTERTAINMENT', 'SOFTWARE', 'EDUCATION', 'CLOUD', 'MUSIC', 'GAMING', 'OTHER')),
    CONSTRAINT chk_subscriptions_status CHECK (status IN ('ACTIVE', 'PAUSED', 'CANCELLED'))
);

CREATE INDEX idx_subscriptions_status ON subscriptions (status);
CREATE INDEX idx_subscriptions_status_next_payment
    ON subscriptions (status, next_payment_date);

