-- Test data initialization script for TestContainers PostgreSQL
-- This script is executed when the PostgreSQL container starts

-- Create extensions if needed
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Note: Tables will be created automatically by Hibernate
-- This script can contain additional test-specific data or configurations

-- Set timezone
SET timezone = 'UTC';

-- Create any additional test-specific database objects here
-- For example, test-specific stored procedures, views, or functions

-- Grant permissions (though not needed for test user in TestContainers)
-- GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO test;
-- GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO test;