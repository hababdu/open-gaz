# Open Gaz

Community-powered live map of operating automotive gas stations across Uzbekistan's Fergana Valley.

## Initial scope
- Android app: Kotlin + Jetpack Compose
- Map: OpenStreetMap (osmdroid), no API key required
- Backend: FastAPI
- Database: PostgreSQL + PostGIS
- Fuel types: CNG (metan), LPG (propan), and other automotive fuels
- Launch region: Andijan, Fergana, Namangan

## Product principles
1. Station availability is reported per fuel type, not only per station.
2. Reports are time-limited and weighted by freshness, proximity, and reporter trust.
3. Conflicting or insufficient reports produce an **Unknown** state rather than a misleading claim.
4. Queue estimates use recent reports and display sample size and last-updated time.
5. Navigation is offered only after the driver reviews the destination.

## Repository layout
- `android/` — Android application
- `backend/` — API and status/queue domain logic
- `docs/` — product and technical specifications

## Development status
Backend: stations, status/queue reports, driver-added stations with community confirmations, comments.
Android: first full client (list/map, station details, reports, confirmations, comments); not yet built in Android Studio.
Database credentials and secrets must be supplied locally; never commit secrets.

## Local backend
```bash
cd backend
python -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
uvicorn app.main:app --reload
```

API health check: `GET /health`.
