ALTER TABLE message_status ADD COLUMN idempotency_key VARCHAR(36);
DELETE FROM message_status WHERE idempotency_key IS NULL;
ALTER TABLE message_status ALTER COLUMN idempotency_key SET NOT NULL;
CREATE UNIQUE INDEX message_status_org_nr_idempotency_key_idx ON message_status (org_nr, idempotency_key);
