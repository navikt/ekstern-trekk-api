ALTER TABLE message_status
    ADD COLUMN debitor_id TEXT,
    ADD COLUMN nav_trekk_id TEXT,
    ADD COLUMN kreditor_trekk_id TEXT;

ALTER TABLE message_status
    DROP COLUMN response_xml,
    DROP COLUMN request_xml;
