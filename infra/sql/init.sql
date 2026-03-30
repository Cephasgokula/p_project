-- LendIQ Database Initialization
-- PostgreSQL 16

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ── applicants ──────────────────────────────────────────────────────
CREATE TABLE applicants (
    id                  UUID           PRIMARY KEY DEFAULT uuid_generate_v4(),
    full_name           TEXT           NOT NULL,
    pan_hash            TEXT           UNIQUE NOT NULL,
    income              NUMERIC(12,2)  NOT NULL,
    age                 INT            NOT NULL CHECK (age > 17),
    employment_months   INT            NOT NULL,
    existing_debt       NUMERIC(12,2)  DEFAULT 0,
    credit_bureau_score INT            CHECK (credit_bureau_score BETWEEN 300 AND 900),
    device_fp           TEXT,
    ip_hash             TEXT,
    created_at          TIMESTAMPTZ    NOT NULL DEFAULT now()
);

CREATE INDEX idx_applicants_device_fp ON applicants(device_fp);
CREATE INDEX idx_applicants_ip_hash   ON applicants(ip_hash);

-- ── applications ────────────────────────────────────────────────────
CREATE TABLE applications (
    id              UUID           PRIMARY KEY DEFAULT uuid_generate_v4(),
    applicant_id    UUID           NOT NULL REFERENCES applicants(id),
    amount          NUMERIC(12,2)  NOT NULL,
    term_months     INT            NOT NULL,
    purpose         TEXT           NOT NULL,
    status          TEXT           NOT NULL DEFAULT 'pending',
    source_channel  TEXT           NOT NULL,
    kafka_offset    BIGINT,
    created_at      TIMESTAMPTZ    NOT NULL DEFAULT now()
);

CREATE INDEX idx_applications_applicant  ON applications(applicant_id);
CREATE INDEX idx_applications_status     ON applications(status);
CREATE INDEX idx_applications_kafka      ON applications(kafka_offset);

-- ── lenders ─────────────────────────────────────────────────────────
CREATE TABLE lenders (
    id              UUID           PRIMARY KEY DEFAULT uuid_generate_v4(),
    name            TEXT           NOT NULL,
    income_min      NUMERIC(12,2)  NOT NULL,
    income_max      NUMERIC(12,2)  NOT NULL,
    age_min         INT            NOT NULL,
    age_max         INT            NOT NULL,
    score_threshold NUMERIC(6,2)   NOT NULL,
    max_loan_amount NUMERIC(12,2)  NOT NULL,
    webhook_url     TEXT           NOT NULL,
    active          BOOLEAN        DEFAULT true
);

-- ── decisions (append-only — no UPDATE/DELETE ever) ─────────────────
CREATE TABLE decisions (
    id              UUID           PRIMARY KEY DEFAULT uuid_generate_v4(),
    application_id  UUID           NOT NULL REFERENCES applications(id),
    dt_score        NUMERIC(6,2)   NOT NULL,
    ml_score        NUMERIC(6,2)   NOT NULL,
    fairness_score  NUMERIC(6,2)   NOT NULL,
    final_score     NUMERIC(6,2)   NOT NULL,
    fraud_prob      NUMERIC(5,4),
    outcome         TEXT           NOT NULL,
    lender_id       UUID           REFERENCES lenders(id),
    model_version   TEXT           NOT NULL,
    shap_json       JSONB          NOT NULL DEFAULT '{}',
    decision_path   TEXT[]         NOT NULL DEFAULT '{}',
    processing_ms   INT            NOT NULL,
    decided_at      TIMESTAMPTZ    NOT NULL DEFAULT now()
);

CREATE INDEX idx_decisions_application ON decisions(application_id);
CREATE INDEX idx_decisions_outcome     ON decisions(outcome);
CREATE INDEX idx_decisions_decided_at  ON decisions(decided_at);

-- Prevent UPDATE/DELETE on decisions table
CREATE OR REPLACE FUNCTION prevent_decision_mutation() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'decisions table is append-only — UPDATE and DELETE are prohibited';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_decisions_no_update BEFORE UPDATE ON decisions
    FOR EACH ROW EXECUTE FUNCTION prevent_decision_mutation();
CREATE TRIGGER trg_decisions_no_delete BEFORE DELETE ON decisions
    FOR EACH ROW EXECUTE FUNCTION prevent_decision_mutation();

-- ── fraud_events ────────────────────────────────────────────────────
CREATE TABLE fraud_events (
    id              UUID           PRIMARY KEY DEFAULT uuid_generate_v4(),
    applicant_id    UUID           NOT NULL REFERENCES applicants(id),
    event_type      TEXT           NOT NULL,
    window_count    INT,
    ring_id         TEXT,
    fraud_prob      NUMERIC(5,4),
    resolved        BOOLEAN        DEFAULT false,
    detected_at     TIMESTAMPTZ    NOT NULL DEFAULT now()
);

CREATE INDEX idx_fraud_applicant ON fraud_events(applicant_id);
CREATE INDEX idx_fraud_resolved  ON fraud_events(resolved);

-- ── Seed demo lender ────────────────────────────────────────────────
INSERT INTO lenders (id, name, income_min, income_max, age_min, age_max, score_threshold, max_loan_amount, webhook_url)
VALUES
    ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'Prime Bank',    50000, 500000, 21, 55, 700, 5000000, 'https://hooks.example.com/prime'),
    ('b1ffcd00-ad1c-5fa9-cc7e-7ccace491b22', 'FlexFin NBFC',  25000, 200000, 21, 60, 550, 2000000, 'https://hooks.example.com/flexfin'),
    ('c2ggde11-be2d-6gb0-dd8f-8ddbdf502c33', 'QuickCredit',   15000, 150000, 18, 65, 450, 1000000, 'https://hooks.example.com/quickcredit');
