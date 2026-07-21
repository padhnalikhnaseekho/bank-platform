CREATE TABLE audit_events (
    id UUID PRIMARY KEY,
    event_id UUID NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    aggregate_type VARCHAR(64) NOT NULL,
    aggregate_id VARCHAR(64) NOT NULL,
    payload TEXT NOT NULL,
    headers TEXT,
    occurred_at TIMESTAMPTZ NOT NULL,
    stored_at TIMESTAMPTZ NOT NULL
);

CREATE UNIQUE INDEX idx_audit_events_event_id ON audit_events (event_id);
CREATE INDEX idx_audit_events_aggregate_id ON audit_events (aggregate_id);
CREATE INDEX idx_audit_events_event_type ON audit_events (event_type);
CREATE INDEX idx_audit_events_occurred_at ON audit_events (occurred_at);

-- Reinforces "audit records cannot be updated" beyond just omitting update/delete code.
-- A plain REVOKE would be a no-op here: Postgres table owners bypass GRANT/REVOKE checks
-- entirely, and the migration role is the table's owner. A trigger fires regardless of
-- ownership, so it's the mechanism that actually enforces immutability.
CREATE OR REPLACE FUNCTION reject_audit_events_mutation() RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'audit_events rows are immutable';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER audit_events_no_update
    BEFORE UPDATE ON audit_events
    FOR EACH ROW EXECUTE FUNCTION reject_audit_events_mutation();

CREATE TRIGGER audit_events_no_delete
    BEFORE DELETE ON audit_events
    FOR EACH ROW EXECUTE FUNCTION reject_audit_events_mutation();

CREATE TABLE processed_events (
    event_id UUID PRIMARY KEY,
    event_type VARCHAR(64) NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL
);
