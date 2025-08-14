# GrafanaAutobuilder

This project was generated using [Angular CLI](https://github.com/angular/angular-cli) version 20.1.1.

## Overview

Grafana AutoBuilder lets you upload a CSV and generate a Grafana dashboard automatically. The project has:

- Frontend: Angular (Navbar, Upload, Footer, Documentation components)
- Backend: Spring Boot (Auth endpoints, PostgreSQL connection)
- Database: PostgreSQL
- **Supports multiple datasources**: Prometheus and PostgreSQL in the same dashboard

## Prerequisites

- Node.js 18+ and npm
- Angular CLI: `npm i -g @angular/cli`
- Java 17+
- Maven or Gradle
- PostgreSQL running locally

Database configuration (used by Spring Boot):

- URL: `jdbc:postgresql://localhost:5432/grafana_autobuilder`
- Username: `grafana_user`
- Password: `sharvil39`

## Development server

To start a local development server, run:

```bash
ng serve
```

Once the server is running, open your browser and navigate to `http://localhost:4200/`. The application will automatically reload whenever you modify any of the source files.

## Backend (Spring Boot)

From the backend project root, start the API server on port 8080:

- Maven: `mvn spring-boot:run`
- Gradle: `gradle bootRun`

Verify PostgreSQL is running and accessible using the connection above (Services/pgAdmin/psql).

Implemented auth endpoints:

- `POST /api/auth/login`
- `POST /api/auth/signup`

Note: The Angular app currently uses demo logic for auth and may not yet call these endpoints. Integration work is planned.

## Using the App

- Home: Overview and quick entry points
- Upload: Drag & drop or browse a `.csv` file and click "Generate Dashboard"
- Documentation: In-app guide describing setup and CSV format

## CSV prerequisites for dashboard generation

The backend reads a CSV of panel definitions and converts each row into a Grafana panel. Columns must match exactly the headers below.

### Required headers (case-sensitive):

- `title` — Panel title (string)
- `datasource` — Grafana datasource UID (e.g., `Prometheus`, `PostgreSQL`)
- `query` — Query for the datasource (PromQL for Prometheus, SQL for PostgreSQL)
- `visualization` — Panel type: `timeseries`, `stat`, `barchart`, `gauge`
- `unit` — Display unit (e.g., `percent`, `bytes`, `s`, `ms`, `short`)

### Optional headers:

- `thresholds` — Two numbers separated by `|` (e.g., `80|90`)
- `w` — Width in grid units (integer)
- `h` — Height in grid units (integer)

### Multiple Datasources Support

**Yes, multiple datasources are supported!** Each panel can use a different datasource, allowing you to mix Prometheus and PostgreSQL panels in the same dashboard.

### Sample CSV - Prometheus

```csv
title,datasource,query,visualization,unit,thresholds,w,h
CPU Usage,Prometheus,avg(rate(process_cpu_seconds_total[5m]))*100,timeseries,percent,80|90,12,8
Requests per Second,Prometheus,sum(rate(http_requests_total[1m])),stat,reqps,1000|2000,6,6
Error Rate,Prometheus,100 * (sum(rate(http_requests_total{status=~"5.."}[5m]))/sum(rate(http_requests_total[5m]))),barchart,percent,5|10,6,6
```

### Sample CSV - PostgreSQL

```csv
title,datasource,query,visualization,unit,thresholds,w,h
User Registrations,PostgreSQL,"SELECT DATE_TRUNC('day', created_at) as time, COUNT(*) as value FROM users WHERE created_at >= $__timeFrom() AND created_at <= $__timeTo() GROUP BY DATE_TRUNC('day', created_at) ORDER BY time",timeseries,short,100|200,12,8
Active Users,PostgreSQL,"SELECT DATE_TRUNC('hour', last_login) as time, COUNT(DISTINCT user_id) as value FROM user_sessions WHERE last_login >= $__timeFrom() AND last_login <= $__timeTo() GROUP BY DATE_TRUNC('hour', last_login) ORDER BY time",timeseries,short,50|100,6,6
```

### Sample CSV - Mixed Datasources

```csv
title,datasource,query,visualization,unit,thresholds,w,h
System CPU,Prometheus,avg(rate(process_cpu_seconds_total[5m])) * 100,timeseries,percent,80|90,12,8
User Registrations,PostgreSQL,"SELECT DATE_TRUNC('day', created_at) as time, COUNT(*) as value FROM users WHERE created_at >= $__timeFrom() AND created_at <= $__timeTo() GROUP BY DATE_TRUNC('day', created_at) ORDER BY time",timeseries,short,100|200,12,8
Memory Usage,Prometheus,process_resident_memory_bytes,gauge,bytes,10737418240|16106127360,6,6
Database Connections,PostgreSQL,"SELECT DATE_TRUNC('minute', connection_time) as time, COUNT(*) as value FROM db_connections WHERE connection_time >= $__timeFrom() AND connection_time <= $__timeTo() GROUP BY DATE_TRUNC('minute', connection_time) ORDER BY time",stat,short,80|120,8,8
```

### PostgreSQL Setup

To use PostgreSQL as a datasource:

1. **Add PostgreSQL datasource in Grafana:**

   - Go to Grafana → Configuration → Data Sources
   - Click "Add data source"
   - Select "PostgreSQL"
   - Configure connection details
   - Note the UID (e.g., "PostgreSQL")

2. **Use the correct query format:**

   - Must have `time` column (timestamp)
   - Must have `value` column (numeric)
   - Use `$__timeFrom()` and `$__timeTo()` for time filtering

3. **Run the test data script:**
   ```bash
   psql -U grafana_user -d grafana_autobuilder -f src/Backend/setup_test_data.sql
   ```

### Checklist (make sure this is true before uploading):

- File is UTF-8 encoded `.csv` with a single header row.
- Headers match exactly: `title,datasource,query,visualization,unit,thresholds,w,h`.
- `datasource` exists and is configured in Grafana.
- `query` is valid for the chosen datasource (PromQL/SQL/etc.).
- `visualization` is one of: `timeseries`, `stat`, `barchart`, `gauge`.
- `thresholds` format: `low|high` (optional; leave blank if not used).
- `w` and `h` are integers if provided; leave blank to let the layout pick defaults.
- No extra/unknown columns.
- **For PostgreSQL queries:** Always use `$__timeFrom()` and `$__timeTo()` for time filtering.

Note: These fields are parsed by `CsvParsingService` and mapped to `PanelConfig` on the backend.

## Styling and Fonts

The entire site uses the Poppins font (fallbacks: Space Grotesk, sans-serif). This is defined globally in `src/styles.css`.

## Troubleshooting

- Backend won't start: Confirm PostgreSQL is running and credentials/URL are correct.
- CSV not accepted: Ensure it's `.csv` and not empty; validate numeric values.
- No data showing: Check that PostgreSQL queries use `$__timeFrom()` and `$__timeTo()`.
- PostgreSQL connection issues: Verify datasource configuration in Grafana.
- Footer shows white border: The standalone Footer is full-bleed; make sure global body margins are reset in `styles.css`.

## Code scaffolding

Angular CLI includes powerful code scaffolding tools. To generate a new component, run:

```bash
ng generate component component-name
```

For a complete list of available schematics (such as `components`, `directives`, or `pipes`), run:

```bash
ng generate --help
```

## Building

To build the project run:

```bash
ng build
```

This will compile your project and store the build artifacts in the `dist/` directory. By default, the production build optimizes your application for performance and speed.

## Running unit tests

To execute unit tests with the [Karma](https://karma-runner.github.io) test runner, use the following command:

```bash
ng test
```

## Running end-to-end tests

For end-to-end (e2e) testing, run:

```bash
ng e2e
```

Angular CLI does not come with an end-to-end testing framework by default. You can choose one that suits your needs.

## Additional Resources

For more information on using the Angular CLI, including detailed command references, visit the [Angular CLI Overview and Command Reference](https://angular.dev/tools/cli) page.
