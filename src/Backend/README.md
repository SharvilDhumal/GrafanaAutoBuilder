# Grafana Autobuilder Backend

This is the Spring Boot backend for the Grafana Autobuilder application.

## Prerequisites

- Java 17 or higher
- Maven 3.6 or higher
- PostgreSQL 12 or higher

## Database Setup

1. **Install PostgreSQL** if not already installed
2. **Run the database setup script** as a PostgreSQL superuser:

```bash
# Connect to PostgreSQL as superuser
psql -U postgres

# Run the setup script
\i setup_database.sql
```

Alternatively, you can run the commands manually:

```sql
-- Create database
CREATE DATABASE grafana_autobuilder;

-- Create user
CREATE USER grafana_user WITH PASSWORD 'sharvil39';

-- Grant permissions
GRANT ALL PRIVILEGES ON DATABASE grafana_autobuilder TO grafana_user;
\c grafana_autobuilder;
GRANT ALL ON SCHEMA public TO grafana_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO grafana_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO grafana_user;
```

## Running the Application

1. **Navigate to the backend directory**:

   ```bash
   cd src/Backend
   ```

2. **Run the application**:
   ```bash
   mvn spring-boot:run
   ```

The application will start on `http://localhost:8080`

## Configuration

The application configuration is in `src/main/resources/application.yml`:

- **Database**: PostgreSQL on localhost:5432
- **Port**: 8080
- **JWT Secret**: Configured in application.yml (should be moved to environment variables in production)

## API Endpoints

- `POST /api/auth/signup` - User registration
- `POST /api/auth/login` - User login
- `POST /api/auth/forgot-password` - Password reset request
- `POST /api/auth/reset-password` - Password reset

## Troubleshooting

### Permission Denied Error

If you see "permission denied for schema public", make sure:

1. The database user has proper permissions
2. Run the database setup script as a superuser
3. The database exists and is accessible

### Flyway Migration Issues

If Flyway fails to run migrations:

1. Check that the database exists
2. Verify user permissions
3. Check the migration files in `src/main/resources/db/migration/`
