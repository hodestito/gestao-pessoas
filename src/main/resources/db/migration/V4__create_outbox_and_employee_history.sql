CREATE TABLE outbox_events (
    event_id UUID PRIMARY KEY,
    employee_id UUID NOT NULL,
    event_type VARCHAR(80) NOT NULL,
    schema_version INTEGER NOT NULL,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    payload TEXT NOT NULL,
    published_at TIMESTAMP WITH TIME ZONE,
    attempts INTEGER DEFAULT 0 NOT NULL,
    available_at TIMESTAMP WITH TIME ZONE NOT NULL,
    last_error VARCHAR(120)
);
CREATE INDEX idx_outbox_pending ON outbox_events(published_at, available_at, occurred_at);

CREATE TABLE employee_events (
    event_id UUID PRIMARY KEY,
    employee_id UUID NOT NULL,
    event_type VARCHAR(80) NOT NULL,
    schema_version INTEGER NOT NULL,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    processed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    payload TEXT NOT NULL
);
CREATE INDEX idx_employee_events_employee ON employee_events(employee_id, occurred_at);
