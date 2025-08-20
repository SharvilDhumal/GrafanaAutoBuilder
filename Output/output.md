# Grafana Autobuilder — End‑to‑End Guide (CSV ➜ Grafana)

This guide explains how to go from a CSV file to a live Grafana dashboard using this project. It includes Grafana setup, backend/frontend setup, CSV format, and the Admin Panel workflow. Image placeholders are included—replace them with your image links later.

> Notes
> - The project currently supports only PostgreSQL datasources.
> - You can use multiple PostgreSQL datasources per dashboard by specifying the datasource UID per CSV row.

---

## Table of Contents
- Overview
- Prerequisites
- Architecture (at a glance)
- Step 1: Prepare Grafana
- Step 2: Prepare PostgreSQL
- Step 3: Start the Backend (Spring Boot)
- Step 4: Start the Frontend (Angular)
- Step 5: Understand the CSV Format
- Step 6: Use the Admin Panel (Upload & Build)
- Step 7: Verify in Grafana
- Optional: Supabase Storage
- Troubleshooting & FAQ
- Quick Checklist

---

## Overview
The app ingests a CSV that describes dashboards/panels and automatically creates/updates dashboards in Grafana using the Grafana HTTP API. Datasource control is per-panel via the datasource UID.

![High-level overview — replace with your image](ADD_IMAGE_LINK_OVERVIEW)

---

## Prerequisites
- Grafana: Local or remote instance you can access as an admin.
- PostgreSQL: Running locally with a database the backend can access.
- Java 17+ and Maven or Gradle: To run the Spring Boot backend.
- Node.js 18+ and Angular CLI: To run the Angular frontend.
- Grafana API Token: Admin or sufficient privileges to create dashboards.

![Prerequisites checklist — replace with your image](ADD_IMAGE_LINK_PREREQS)

---

## Architecture (at a glance)
1. CSV is uploaded via the Angular Admin Panel.
2. Backend parses CSV and composes Grafana dashboard JSON.
3. Backend calls Grafana API to create/update dashboards.
4. Datasource per panel is resolved using configured defaults or the CSV per-row `datasource_uid`.

![Architecture diagram — replace with your image](ADD_IMAGE_LINK_ARCH)

---

## Step 1: Prepare Grafana
1. Install/Run Grafana  
   - Local default: http://localhost:3000
2. Create a Grafana API Token (Admin → Service Accounts or API Keys)  
   - Scope: Admin or at least Dashboards:Write and Datasources:Read  
   - Save the token securely  
   - ![Create API token — replace with your image](ADD_IMAGE_LINK_GRAFANA_TOKEN)
3. Create PostgreSQL Datasource(s) in Grafana  
   - Configure host, port, database, user, password  
   - Get each datasource UID from the Datasource settings page (UID is visible there)  
   - ![Datasource UID location — replace with your image](ADD_IMAGE_LINK_DS_UID)

Record: Grafana URL, API token, and datasource UID(s) for later steps.

---

## Step 2: Prepare PostgreSQL
Ensure a PostgreSQL instance is running and reachable by the backend. Example local setup:
- URL: `jdbc:postgresql://localhost:5432/grafana_autobuilder`
- Username: `postgres`
- Password: `sharvil39*`

Create the database if it does not exist. Validate connectivity using pgAdmin or psql.

![PostgreSQL ready — replace with your image](ADD_IMAGE_LINK_POSTGRES)

---

## Step 3: Start the Backend (Spring Boot)
Backend code is under `src/Backend/`.

1. Set environment variables (examples):
   - `GRAFANA_URL` (e.g., `http://localhost:3000`)
   - `GRAFANA_API_TOKEN` (token created in Step 1)
   - `SPRING_DATASOURCE_URL` (e.g., `jdbc:postgresql://localhost:5432/grafana_autobuilder`)
   - `SPRING_DATASOURCE_USERNAME`
   - `SPRING_DATASOURCE_PASSWORD`
   - Optional overrides as defined in `src/Backend/` configuration files.

2. Run the server from `src/Backend/`:
   - Maven: `mvn spring-boot:run`
   - Gradle: `gradle bootRun`

3. The backend listens on `http://localhost:8080` by default.

![Backend running — replace with your image](ADD_IMAGE_LINK_BACKEND_RUNNING)

---

## Step 4: Start the Frontend (Angular)
1. Install dependencies at the project root:
   - `npm install`
2. Start the dev server:
   - `npm start` or `ng serve`
3. Open: `http://localhost:4200`

![Frontend home — replace with your image](ADD_IMAGE_LINK_FRONTEND_HOME)

---

## Step 5: Understand the CSV Format
Use the sample CSVs in the project root as templates:
- `sample_dashboard_minimal.csv`
- `sample_dashboard_minimal_with_uid.csv`
- `sample_panels.csv`
- `sample_panels_postgresql.csv`
- `complex_sample_postgresql.csv`

Key points:
- Only PostgreSQL datasources are supported currently.
- You can specify a datasource per row via a column like `datasource_uid`.
- Start from the samples and adjust titles, queries, panel types, and layout to your needs.
- Ensure any referenced tables/columns exist in your PostgreSQL database.

![CSV format concept — replace with your image](ADD_IMAGE_LINK_CSV_FORMAT)

---

## Step 6: Use the Admin Panel (Upload & Build)
1. Navigate to the app and open the Admin section.  
   - If authentication is enabled, log in or sign up.
2. Upload CSV  
   - Choose a sample-based CSV (from Step 5)  
   - Optionally map fields if the UI prompts  
   - ![Upload CSV — replace with your image](ADD_IMAGE_LINK_UPLOAD)
3. Configure build options (if shown)  
   - Default datasource UID vs. per-row `datasource_uid`  
   - Overwrite existing dashboards vs. create new ones  
   - Folder/Org selection  
   - ![Build options — replace with your image](ADD_IMAGE_LINK_BUILD_OPTIONS)
4. Run Build  
   - Triggers the backend to parse CSV and call Grafana API  
   - Review logs/status in the UI  
   - ![Build progress — replace with your image](ADD_IMAGE_LINK_BUILD_PROGRESS)

Tip: Keep an eye on console/backend logs for validation or API errors.

---

## Step 7: Verify in Grafana
1. Open Grafana → Dashboards.
2. Locate the newly created/updated dashboard.
3. Open panels and confirm:
   - Queries run against the intended PostgreSQL datasource(s).
   - Visualizations, thresholds, units, and time ranges match expectations.

![Dashboard verified — replace with your image](ADD_IMAGE_LINK_VERIFY)

---

## Optional: Supabase Storage
If you plan to store CSVs or assets in Supabase:
1. Set environment variables (see project README):
   - `SUPABASE_URL`
   - `SUPABASE_SERVICE_KEY`
   - `SUPABASE_BUCKET`
2. Configure the app to read from/write to the bucket as needed.

![Supabase settings — replace with your image](ADD_IMAGE_LINK_SUPABASE)

---

## Troubleshooting & FAQ
- Dashboards not appearing  
  - Validate `GRAFANA_URL` and `GRAFANA_API_TOKEN` in backend env  
  - Check backend logs for API errors  
  - Ensure your Grafana user/API token has write permissions
- Panels show errors / no data  
  - Confirm PostgreSQL credentials and network access in the Grafana datasource  
  - Verify SQL in CSV against real tables/columns  
  - If using `datasource_uid` per row, ensure those UIDs exist in Grafana
- Multiple datasources in one dashboard  
  - Include `datasource_uid` per panel row, or set a default at build time
- Backend won’t start  
  - Ensure Java, Maven/Gradle installed  
  - Verify PostgreSQL is running and connection details are correct
- Auth issues  
  - If frontend auth is disabled or using demo logic, try accessing Admin without login  
  - If enabled, ensure backend auth endpoints are reachable (`/api/auth/login`, `/api/auth/signup`)

---

## Quick Checklist
- [ ] Grafana running, API token created  
- [ ] PostgreSQL running and reachable  
- [ ] Backend env set (Grafana URL/token, DB URL/user/pass)  
- [ ] Backend running on 8080  
- [ ] Frontend running on 4200  
- [ ] CSV prepared from samples (with `datasource_uid` if needed)  
- [ ] Build triggered from Admin Panel  
- [ ] Dashboard verified in Grafana

![Checklist done — replace with your image](ADD_IMAGE_LINK_CHECKLIST)