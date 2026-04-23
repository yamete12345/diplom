-- V1: Начальная схема БД модуля управления банковскими счетами.

CREATE TABLE users (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    email          VARCHAR(255) NOT NULL UNIQUE,
    password_hash  VARCHAR(255) NOT NULL,
    full_name      VARCHAR(255) NOT NULL,
    email_verified BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE otp_codes (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT       NOT NULL,
    code_hash   VARCHAR(255) NOT NULL,
    purpose     VARCHAR(32)  NOT NULL,
    expires_at  TIMESTAMP    NOT NULL,
    used        BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_otp_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_otp_user ON otp_codes(user_id);

CREATE TABLE accounts (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT         NOT NULL,
    account_number  VARCHAR(20)    NOT NULL UNIQUE,
    type            VARCHAR(16)    NOT NULL,
    currency        VARCHAR(3)     NOT NULL DEFAULT 'RUB',
    balance         DECIMAL(19, 4) NOT NULL DEFAULT 0,
    status          VARCHAR(16)    NOT NULL DEFAULT 'ACTIVE',
    opened_at       TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    closed_at       TIMESTAMP      NULL,
    CONSTRAINT fk_account_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_accounts_user ON accounts(user_id);

CREATE TABLE transactions (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    type        VARCHAR(16)    NOT NULL,
    amount      DECIMAL(19, 4) NOT NULL,
    currency    VARCHAR(3)     NOT NULL DEFAULT 'RUB',
    created_at  TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    description VARCHAR(500)
);

CREATE TABLE postings (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    transaction_id BIGINT         NOT NULL,
    account_id     BIGINT         NOT NULL,
    direction      VARCHAR(8)     NOT NULL,
    amount         DECIMAL(19, 4) NOT NULL,
    CONSTRAINT fk_posting_tx   FOREIGN KEY (transaction_id) REFERENCES transactions(id) ON DELETE CASCADE,
    CONSTRAINT fk_posting_acct FOREIGN KEY (account_id)     REFERENCES accounts(id)
);

CREATE INDEX idx_postings_tx   ON postings(transaction_id);
CREATE INDEX idx_postings_acct ON postings(account_id);
