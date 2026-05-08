CREATE ROLE "ekstern-trekk-api-db-user" WITH LOGIN PASSWORD 'app_pass';
GRANT CONNECT ON DATABASE postgres TO "ekstern-trekk-api-db-user";
GRANT USAGE ON SCHEMA public TO "ekstern-trekk-api-db-user";
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO "ekstern-trekk-api-db-user";
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO "ekstern-trekk-api-db-user";