ALTER TABLE message_status
    ADD COLUMN debitor_id VARCHAR(32),
    ADD COLUMN nav_trekk_id VARCHAR(32),
    ADD COLUMN kreditor_trekk_id VARCHAR(256);

ALTER TABLE message_status
    DROP COLUMN response_xml,
    DROP COLUMN request_xml;
