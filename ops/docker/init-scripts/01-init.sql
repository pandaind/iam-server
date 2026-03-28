-- Initialize IAM Database
-- This script is executed when PostgreSQL container starts

-- Ensure the database exists
\c iamdb;

-- Create extensions if needed
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Create indexes for better performance (will be created by Hibernate, but good to have)
-- These will be created automatically by JPA, but can be customized here if needed

-- Grant permissions
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO iam_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO iam_user;

-- Optional: Create read-only user for reporting
-- CREATE USER iam_reader WITH PASSWORD 'readonly_password';
-- GRANT CONNECT ON DATABASE iamdb TO iam_reader;
-- GRANT USAGE ON SCHEMA public TO iam_reader;
-- GRANT SELECT ON ALL TABLES IN SCHEMA public TO iam_reader;

-- Set default permissions for future objects
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO iam_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO iam_user;