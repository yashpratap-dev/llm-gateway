-- Seed default PRIORITY routing policy for any tenant that does not already have one.
-- Idempotent: skips tenants where a routing_policies row already exists.
INSERT INTO routing_policies (id, tenant_id, strategy, updated_at)
SELECT gen_random_uuid(), id, 'PRIORITY', NOW()
FROM tenants
WHERE id NOT IN (SELECT tenant_id FROM routing_policies);
