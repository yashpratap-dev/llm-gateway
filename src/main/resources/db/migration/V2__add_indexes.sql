-- Additional indexes for M2 authentication and query performance.
-- idx_api_keys_key_hash already exists from V1 — not recreated here.
-- Usage-log indexes use IF NOT EXISTS as a safety guard against re-runs.

CREATE INDEX IF NOT EXISTS idx_api_keys_status ON api_keys(status);
CREATE INDEX IF NOT EXISTS idx_budgets_tenant_id ON budgets(tenant_id);
CREATE INDEX IF NOT EXISTS idx_usage_logs_tenant_id ON usage_logs(tenant_id);
CREATE INDEX IF NOT EXISTS idx_usage_logs_created_at ON usage_logs(created_at);
CREATE INDEX IF NOT EXISTS idx_usage_logs_provider ON usage_logs(provider);
