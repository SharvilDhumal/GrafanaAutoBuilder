# Grafana Autobuilder — Dashboard Generation Guide (CSV ➜ Grafana)

This guide focuses only on generating Grafana dashboards from a CSV using the Admin Panel workflow. For installation, environment variables, backend/frontend startup, and database configuration, please see the project `README.md`. Image placeholders are included—replace them with your image links later.

> Notes
> - The project currently supports only PostgreSQL datasources.
> - You can use multiple PostgreSQL datasources per dashboard by specifying the datasource UID per CSV row.

---

## Table of Contents
- Overview
- Minimal Requirements
- CSV Format (what the app expects)
- Use the Admin Panel (Upload & Build)
- Verify in Grafana
- Troubleshooting (CSV/build-focused)
- Quick Checklist

---

## Overview
The app ingests a CSV that describes dashboards/panels and automatically creates/updates dashboards in Grafana using the Grafana HTTP API. Datasource control is per-panel via the datasource UID.

![High-level overview — replace with your image](ADD_IMAGE_LINK_OVERVIEW)

---

## Architecture (at a glance)
1. CSV is uploaded via the Admin Panel.
2. Backend parses CSV and composes Grafana dashboard JSON.
3. Backend calls Grafana API to create/update dashboards.
4. Per-panel datasource is resolved via default settings or the per-row `datasource_uid`.

![Architecture diagram — replace with your image](ADD_IMAGE_LINK_ARCH)

---

## Minimal Requirements
Before building dashboards, make sure you have:
- Grafana URL and an API token with dashboard write permissions.
- At least one PostgreSQL datasource configured in Grafana and its UID(s).
- Grafana Autobuilder app running and reachable (backend + frontend).

For how to install, configure, and start services, see `README.md`.

![Minimal requirements — replace with your image](ADD_IMAGE_LINK_MIN_REQS)

---

## CSV Format (what the app expects)
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

## Use the Admin Panel (Upload & Build)
1. Navigate to the app and open the Admin section.  
   - If authentication is enabled, log in or sign up.
2. Upload CSV  
   - Choose a sample-based CSV (from the CSV Format section)  
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

## Verify in Grafana
1. Open Grafana → Dashboards.
2. Locate the newly created/updated dashboard.
3. Open panels and confirm:
   - Queries run against the intended PostgreSQL datasource(s).
   - Visualizations, thresholds, units, and time ranges match expectations.

![Dashboard verified — replace with your image](ADD_IMAGE_LINK_VERIFY)

---

## Troubleshooting (CSV/build-focused)
- Dashboards not appearing  
  - Confirm your API token has dashboard write permissions.  
  - Check backend logs for Grafana API errors.  
  - Ensure the target folder/org (if specified) exists.
- Panels show errors / no data  
  - Verify SQL in CSV against real tables/columns in your DB.  
  - Ensure Grafana datasource connection works.  
  - If using `datasource_uid` per row, ensure those UIDs exist in Grafana.
- Multiple datasources in one dashboard  
  - Include `datasource_uid` per panel row, or set a default at build time.

For runtime/setup issues (install, auth, backend, frontend), see `README.md`.

---

## Quick Checklist
- [ ] Grafana URL + API token ready  
- [ ] PostgreSQL datasource UID(s) available in Grafana  
- [ ] CSV prepared from samples (with `datasource_uid` if needed)  
- [ ] Build triggered from Admin Panel  
- [ ] Dashboard verified in Grafana

![Checklist done — replace with your image](ADD_IMAGE_LINK_CHECKLIST)