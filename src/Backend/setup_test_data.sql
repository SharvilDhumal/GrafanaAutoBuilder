-- Setup test data for Grafana Autobuilder PostgreSQL testing
-- Run this script in your PostgreSQL database

-- Create sample tables for testing

-- Users table (already exists, but adding sample data)
INSERT INTO users (id, email, password, roles, enabled, created_at, updated_at) VALUES
(gen_random_uuid(), 'test1@example.com', '$2a$10$encrypted', 'ROLE_USER', true, NOW() - INTERVAL '30 days', NOW()),
(gen_random_uuid(), 'test2@example.com', '$2a$10$encrypted', 'ROLE_USER', true, NOW() - INTERVAL '25 days', NOW()),
(gen_random_uuid(), 'test3@example.com', '$2a$10$encrypted', 'ROLE_USER', true, NOW() - INTERVAL '20 days', NOW()),
(gen_random_uuid(), 'test4@example.com', '$2a$10$encrypted', 'ROLE_USER', true, NOW() - INTERVAL '15 days', NOW()),
(gen_random_uuid(), 'test5@example.com', '$2a$10$encrypted', 'ROLE_USER', true, NOW() - INTERVAL '10 days', NOW()),
(gen_random_uuid(), 'test6@example.com', '$2a$10$encrypted', 'ROLE_USER', true, NOW() - INTERVAL '5 days', NOW()),
(gen_random_uuid(), 'test7@example.com', '$2a$10$encrypted', 'ROLE_USER', true, NOW() - INTERVAL '1 day', NOW());

-- Create user_sessions table
CREATE TABLE IF NOT EXISTS user_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    last_login TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    session_duration_minutes INTEGER DEFAULT 30
);

-- Create application_logs table
CREATE TABLE IF NOT EXISTS application_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    timestamp TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    status_code INTEGER NOT NULL,
    endpoint VARCHAR(255),
    response_time_ms INTEGER DEFAULT 200
);

-- Create db_connections table
CREATE TABLE IF NOT EXISTS db_connections (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    connection_time TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    connection_duration_ms INTEGER DEFAULT 1000,
    user_id UUID
);

-- Create api_requests table
CREATE TABLE IF NOT EXISTS api_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    request_time TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    response_time_ms INTEGER DEFAULT 250,
    endpoint VARCHAR(255),
    method VARCHAR(10)
);

-- Insert sample data for the last 7 days
DO $$
DECLARE
    i INTEGER;
    current_time TIMESTAMP WITH TIME ZONE;
BEGIN
    FOR i IN 0..168 LOOP  -- 7 days * 24 hours
        current_time := NOW() - (i * INTERVAL '1 hour');
        
        -- Insert user sessions
        INSERT INTO user_sessions (user_id, last_login, session_duration_minutes)
        SELECT 
            (SELECT id FROM users ORDER BY RANDOM() LIMIT 1),
            current_time,
            (RANDOM() * 60 + 10)::INTEGER
        FROM generate_series(1, (RANDOM() * 5 + 1)::INTEGER);
        
        -- Insert application logs
        INSERT INTO application_logs (timestamp, status_code, endpoint, response_time_ms)
        SELECT 
            current_time + (RANDOM() * INTERVAL '1 hour'),
            CASE WHEN RANDOM() > 0.95 THEN 500 ELSE 200 END,
            CASE (RANDOM() * 3)::INTEGER
                WHEN 0 THEN '/api/users'
                WHEN 1 THEN '/api/dashboard'
                ELSE '/api/metrics'
            END,
            (RANDOM() * 300 + 100)::INTEGER
        FROM generate_series(1, (RANDOM() * 10 + 5)::INTEGER);
        
        -- Insert database connections
        INSERT INTO db_connections (connection_time, connection_duration_ms, user_id)
        SELECT 
            current_time + (RANDOM() * INTERVAL '1 hour'),
            (RANDOM() * 2000 + 500)::INTEGER,
            (SELECT id FROM users ORDER BY RANDOM() LIMIT 1)
        FROM generate_series(1, (RANDOM() * 3 + 1)::INTEGER);
        
        -- Insert API requests
        INSERT INTO api_requests (request_time, response_time_ms, endpoint, method)
        SELECT 
            current_time + (RANDOM() * INTERVAL '1 hour'),
            (RANDOM() * 400 + 150)::INTEGER,
            CASE (RANDOM() * 4)::INTEGER
                WHEN 0 THEN '/api/auth/login'
                WHEN 1 THEN '/api/dashboard/upload'
                WHEN 2 THEN '/api/metrics'
                ELSE '/api/users/profile'
            END,
            CASE (RANDOM() * 2)::INTEGER
                WHEN 0 THEN 'GET'
                ELSE 'POST'
            END
        FROM generate_series(1, (RANDOM() * 15 + 10)::INTEGER);
    END LOOP;
END $$;

-- Create indexes for better performance
CREATE INDEX IF NOT EXISTS idx_user_sessions_last_login ON user_sessions(last_login);
CREATE INDEX IF NOT EXISTS idx_application_logs_timestamp ON application_logs(timestamp);
CREATE INDEX IF NOT EXISTS idx_db_connections_connection_time ON db_connections(connection_time);
CREATE INDEX IF NOT EXISTS idx_api_requests_request_time ON api_requests(request_time);

-- Grant permissions to grafana_user
GRANT ALL PRIVILEGES ON TABLE user_sessions TO grafana_user;
GRANT ALL PRIVILEGES ON TABLE application_logs TO grafana_user;
GRANT ALL PRIVILEGES ON TABLE db_connections TO grafana_user;
GRANT ALL PRIVILEGES ON TABLE api_requests TO grafana_user;

-- Show sample data
SELECT 'User Sessions' as table_name, COUNT(*) as count FROM user_sessions
UNION ALL
SELECT 'Application Logs' as table_name, COUNT(*) as count FROM application_logs
UNION ALL
SELECT 'DB Connections' as table_name, COUNT(*) as count FROM db_connections
UNION ALL
SELECT 'API Requests' as table_name, COUNT(*) as count FROM api_requests;
