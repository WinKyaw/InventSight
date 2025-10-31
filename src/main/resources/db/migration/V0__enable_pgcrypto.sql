-- Flyway migration V0: Enable pgcrypto extension
-- This must run before V1+ migrations that use gen_random_uuid()

CREATE EXTENSION IF NOT EXISTS pgcrypto;
