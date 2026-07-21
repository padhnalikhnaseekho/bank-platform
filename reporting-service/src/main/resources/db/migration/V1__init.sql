CREATE TABLE account_activity_view (
    id UUID PRIMARY KEY,
    customer_id UUID NOT NULL,
    account_id UUID NOT NULL,
    event_type VARCHAR(32) NOT NULL,
    amount NUMERIC(19, 4) NOT NULL,
    currency VARCHAR(8) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_account_activity_view_account_id_occurred_at ON account_activity_view (account_id, occurred_at);
CREATE INDEX idx_account_activity_view_customer_id ON account_activity_view (customer_id);

CREATE TABLE statement_jobs (
    id UUID PRIMARY KEY,
    customer_id UUID NOT NULL,
    account_id UUID NOT NULL,
    period_start TIMESTAMPTZ NOT NULL,
    period_end TIMESTAMPTZ NOT NULL,
    status VARCHAR(16) NOT NULL,
    csv_file_url TEXT,
    pdf_file_url TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_statement_jobs_customer_id ON statement_jobs (customer_id);

CREATE TABLE processed_events (
    event_id UUID PRIMARY KEY,
    event_type VARCHAR(64) NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL
);
