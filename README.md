# GrafanaAutobuilder

This project was generated using [Angular CLI](https://github.com/angular/angular-cli) version 20.1.1.

## Overview

Grafana AutoBuilder lets you upload a CSV and generate a Grafana dashboard automatically. The project has:

- Frontend: Angular (Navbar, Upload, Footer, Documentation components)
- Backend: Spring Boot (Auth endpoints, PostgreSQL connection)
- Database: PostgreSQL

## Prerequisites

- Node.js 18+ and npm
- Angular CLI: `npm i -g @angular/cli`
- Java 17+
- Maven or Gradle
- PostgreSQL running locally

Database configuration (used by Spring Boot):

- URL: `jdbc:postgresql://localhost:5432/grafana_autobuilder`
- Username: `postgres`
- Password: `sharvil39*`

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

## CSV Format

The app accepts standard `.csv` files. Recommended columns for dashboard generation:

- `timestamp` — ISO 8601 or epoch milliseconds (e.g., `2025-08-10T12:34:56Z`)
- `metric` — Measurement/series name (e.g., `cpu_usage`, `http_requests`)
- `value` — Numeric value
- `tags` (optional) — key=value pairs separated by `|` (e.g., `host=web-1|region=us-east`)

Example:

```csv
timestamp,metric,value,tags
2025-08-10T12:00:00Z,cpu_usage,37.5,host=web-1|region=us-east
2025-08-10T12:00:00Z,http_requests,120,host=web-1|route=/api
2025-08-10T12:01:00Z,cpu_usage,45.2,host=web-1|region=us-east
```

Tips:

- Ensure the file extension is `.csv`.
- Keep timestamp formats consistent across rows.
- Keep `value` strictly numeric (no units/currency symbols).

## Styling and Fonts

The entire site uses the Poppins font (fallbacks: Space Grotesk, sans-serif). This is defined globally in `src/styles.css`.

## Troubleshooting

- Backend won't start: Confirm PostgreSQL is running and credentials/URL are correct.
- CSV not accepted: Ensure it's `.csv` and not empty; validate numeric values.
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
