-- Initialize test database schema
-- This script runs when the PostgreSQL container first starts

-- Create the test schema if it doesn't exist
CREATE SCHEMA IF NOT EXISTS test;

-- Grant privileges
GRANT ALL PRIVILEGES ON SCHEMA test TO postgres;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA test TO postgres;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA test TO postgres;

-- Set search path
ALTER DATABASE projects SET search_path TO test, public;
