# GrafanaAutobuilder

This project was generated using [Angular CLI](https://github.com/angular/angular-cli) version 20.1.1.

## Overview

Grafana AutoBuilder lets you upload a CSV and generate a Grafana dashboard automatically. The project has:

- Frontend: Angular (Navbar, Upload, Footer, Documentation components)
- Backend: Spring Boot (Auth endpoints, PostgreSQL connection)
- Database: PostgreSQL
- Datasource support: **PostgreSQL only** (multiple Postgres datasources are supported via UID)

## Prerequisites

- Node.js 18+ and npm
- Angular CLI: `npm i -g @angular/cli`
- Java 17+
- Maven or Gradle
- PostgreSQL running locally
- Optional: Supabase account (for storing uploaded CSVs via Supabase Storage)

Database configuration (used by Spring Boot):

- URL: `jdbc:postgresql://localhost:5432/grafana_autobuilder`
- Username: `grafana_user`
- Password: `sharvil39`

Grafana configuration (used by Spring Boot):

- URL: `http://localhost:3000`
- API Key: A Grafana API key with at least Editor permissions

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

### Backend configuration (.env and application.yml)

The backend reads defaults from `src/Backend/src/main/resources/application.yml` and can be overridden with environment variables. A starter env is provided in `.env.example` at the repo root — copy it to `.env` and adjust values.

Relevant properties from `application.yml`:

- Spring datasource: `spring.datasource.*` (PostgreSQL)
- Grafana: `grafana.url`, `grafana.apiKey`, `grafana.defaultDatasourceUid`, `grafana.defaultDatasourceType`
- Supabase: `supabase.url`, `supabase.serviceKey`, `supabase.storage.bucket`

Environment variable overrides supported (set in your shell or a process manager):

- `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`
- `GRAFANA_API_KEY`
- `SUPABASE_URL`, `SUPABASE_SERVICE_KEY`, `SUPABASE_BUCKET`

Security: never commit real secrets. `.env` is gitignored. The repository may contain placeholder values in `application.yml`; override them in your local environment.

## Using the App

- Home: Overview and quick entry points
- Upload: Drag & drop or browse a `.csv` file and click "Generate Dashboard"
- Documentation: In-app guide describing setup and CSV format

## CSV prerequisites for dashboard generation

The backend reads a CSV of panel definitions and converts each row into a Grafana panel. Columns must match exactly the headers below.

### Required headers (case-sensitive):

- `title` — Panel title (string)
- `datasource` — Grafana datasource UID (PostgreSQL). If blank, backend uses the default UID from `application.yml`.
- `query` — SQL for PostgreSQL (use Grafana macros like `$__timeFilter()` and `$__timeGroupAlias()`).
- `visualization` — Panel type: `timeseries`, `stat`, `barchart`, `gauge`
  - Aliases: you may also use `panel_type`, `chart_type`, or `viewType` (they map to `visualization`).
- `unit` — Display unit (e.g., `percent`, `bytes`, `s`, `ms`, `short`)

### Optional headers:

- `thresholds` — Two numbers separated by `|` (e.g., `80|90`)
- `w` — Width in grid units (integer)
- `h` — Height in grid units (integer)

### Datasource support

- Only PostgreSQL datasources are supported at the moment.
- You may specify a different Postgres datasource per row by setting the `datasource` column to that datasource's UID.

### Sample CSV - PostgreSQL

```csv
title,datasource,query,visualization,unit,thresholds,w,h
User Registrations,fev06aq8frhfka,"SELECT DATE_TRUNC('day', created_at) as time, COUNT(*) as value FROM users WHERE created_at >= $__timeFrom() AND created_at <= $__timeTo() GROUP BY DATE_TRUNC('day', created_at) ORDER BY time",timeseries,short,100|200,12,8
Active Users,fev06aq8frhfka,"SELECT DATE_TRUNC('hour', last_login) as time, COUNT(DISTINCT user_id) as value FROM user_sessions WHERE last_login >= $__timeFrom() AND last_login <= $__timeTo() GROUP BY DATE_TRUNC('hour', last_login) ORDER BY time",timeseries,short,50|100,6,6
```

### PostgreSQL Setup

To use PostgreSQL as a datasource:

1. **Add PostgreSQL datasource in Grafana:**

   - Go to Grafana → Configuration → Data Sources
   - Click "Add data source"
   - Select "PostgreSQL"
   - Configure connection details
   - Copy the UID from the datasource page (e.g., `fev06aq8frhfka`) and use it in the CSV `datasource` column when you want to override the default

2. **Use the correct query format:**

   - Must have `time` column (timestamp)
   - Must have `value` column (numeric)
   - Use `$__timeFrom()` and `$__timeTo()` for time filtering

3. **Run the test data script:**
   ```bash
   psql -U grafana_user -d grafana_autobuilder -f src/Backend/setup_test_data.sql
   ```

### Grafana Setup

1. Create an API key in Grafana (Settings → API keys). Set this as `GRAFANA_API_KEY` or in `application.yml` (`grafana.apiKey`).
2. Add a PostgreSQL datasource in Grafana and copy its UID. Set a default UID in `application.yml` as `grafana.defaultDatasourceUid` or provide per-row via CSV `datasource`.
3. Ensure Grafana is reachable at `http://localhost:3000` (or update `grafana.url`).

The backend uses these to create dashboards and panels.

### Supabase Storage (for CSV uploads)

Uploads are stored in Supabase Storage by `SupabaseStorageService` (`src/Backend/src/main/java/com/example/grafanaautobuilder/service/storage/SupabaseStorageService.java`). This is optional but recommended for keeping an auditable record of uploaded CSV files.

Setup steps:

1. Create a Supabase project and go to Project Settings → API.
2. Copy the Project URL (e.g., `https://YOUR-PROJECT.supabase.co`) and the Service Role key.
   - Important: Use the Service Role key server-side only. Keep it secret.
3. Create a Storage bucket (e.g., `uploads`).
4. Provide configuration to the backend via env vars or `application.yml`:
   - `SUPABASE_URL`
   - `SUPABASE_SERVICE_KEY`
   - `SUPABASE_BUCKET` (defaults to `uploads`)

How it works:

- On dashboard upload (`POST /api/dashboard/upload`), the CSV is first stored in Supabase (`bucket/users/anonymous/<uuid>-file.csv`), then processed to create a Grafana dashboard. Metadata is saved via `FileMetadataRepository`.
- You can also upload directly via `POST /api/files/upload` and request a signed download URL via `GET /api/files/signed-url?path=...`.

Troubleshooting Supabase:

- Ensure the Service Role key is set and valid; 401 responses indicate an auth header or key issue.
- If you see errors creating signed URLs, verify the bucket exists and the path is correct.

### Checklist (make sure this is true before uploading):

- File is UTF-8 encoded `.csv` with a single header row.
- Headers match exactly: `title,datasource,query,visualization,unit,thresholds,w,h`.
  - Alternatively, you can use `panel_type`, `chart_type`, or `viewType` instead of `visualization`.
- `datasource` exists and is configured in Grafana.
- `query` is valid for the chosen datasource (PromQL/SQL/etc.).
- `query` is valid SQL for PostgreSQL.
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
- Supabase upload/sign issues: Check `SUPABASE_URL`, `SUPABASE_SERVICE_KEY`, and that the bucket exists. See server logs.
- Footer shows white border: The standalone Footer is full-bleed; make sure global body margins are reset in `styles.css`.

## End-to-end Quick Start

1. Install dependencies
   - Frontend: `npm ci`
   - Backend: ensure Maven/Gradle available
2. Copy `.env.example` to `.env` and fill in values (Grafana API key, DB creds, optional Supabase vars).
3. Start services
   - Backend: from `src/Backend/`, run `mvn spring-boot:run` (or `gradle bootRun`). Backend runs on `http://localhost:8080`.
   - Frontend: from repo root, run `ng serve`. Frontend runs on `http://localhost:4200`.
4. In Grafana, create a PostgreSQL datasource and note its UID. Put UID into `application.yml` (`grafana.defaultDatasourceUid`) or provide per CSV row.
5. Upload a sample CSV from the UI or call `POST /api/dashboard/upload` with `multipart/form-data`.
6. Open the returned Grafana dashboard URL.

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
