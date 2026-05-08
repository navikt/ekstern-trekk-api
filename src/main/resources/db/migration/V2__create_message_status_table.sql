CREATE TABLE "message_status" (
    "message_id" VARCHAR(256) PRIMARY KEY,
    "processed_at" TIMESTAMP DEFAULT now(),
    "latest_status" message_status_type NOT NULL,
    "response_at" TIMESTAMP,
    "response_description" VARCHAR(256)
);
