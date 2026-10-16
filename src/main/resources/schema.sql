CREATE TABLE IF NOT EXISTS transaction_record (
    record_id    VARCHAR(40)  PRIMARY KEY,
    account_id   VARCHAR(20)  NOT NULL,
    amount_minor BIGINT       NOT NULL,
    currency     CHAR(3)      NOT NULL,
    posted_date  DATE         NOT NULL,
    source_file  VARCHAR(512) NOT NULL,
    loaded_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS ingestion_checkpoint (
    file_key   VARCHAR(512) PRIMARY KEY,
    last_line  BIGINT       NOT NULL,
    updated_at TIMESTAMPTZ  NOT NULL
);
