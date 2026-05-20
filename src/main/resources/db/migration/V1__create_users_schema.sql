CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE users_profile (
  id         UUID         PRIMARY KEY,
  name       VARCHAR(255) NOT NULL,
  phone      VARCHAR(20),
  created_at TIMESTAMP    NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE wallets (
  id         UUID    PRIMARY KEY DEFAULT uuid_generate_v4(),
  user_id    UUID    NOT NULL UNIQUE REFERENCES users_profile(id),
  balance    NUMERIC(10,2) NOT NULL DEFAULT 0.00
    CONSTRAINT chk_balance_positive CHECK (balance >= 0),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TYPE wallet_tx_type AS ENUM ('RECHARGE', 'PAYMENT');

CREATE TABLE wallet_transactions (
  id               UUID          PRIMARY KEY DEFAULT uuid_generate_v4(),
  wallet_id        UUID          NOT NULL REFERENCES wallets(id),
  amount           NUMERIC(10,2) NOT NULL
    CONSTRAINT chk_amount_positive CHECK (amount > 0),
  type             wallet_tx_type NOT NULL,
  description      VARCHAR(255),
  transaction_date TIMESTAMP     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_wallet_user       ON wallets(user_id);
CREATE INDEX idx_wallet_tx_wallet  ON wallet_transactions(wallet_id);
CREATE INDEX idx_wallet_tx_date    ON wallet_transactions(transaction_date);
