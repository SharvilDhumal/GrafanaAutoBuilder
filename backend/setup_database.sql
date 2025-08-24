-- Database setup script for Grafana Autobuilder
-- Run this as a PostgreSQL superuser (postgres)

-- Create the database if it doesn't exist
CREATE DATABASE grafana_autobuilder;

-- Create the user if it doesn't exist
CREATE USER grafana_user WITH PASSWORD 'sharvil39';

-- Grant necessary permissions to the user
GRANT ALL PRIVILEGES ON DATABASE grafana_autobuilder TO grafana_user;

-- Connect to the database
\c grafana_autobuilder;

-- Grant schema permissions
GRANT ALL ON SCHEMA public TO grafana_user;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO grafana_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO grafana_user;

-- Set default privileges for future tables
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO grafana_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO grafana_user;
