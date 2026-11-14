# Open Gaz Backend

## Stack
- FastAPI + SQLAlchemy 2
- PostgreSQL 16 + PostGIS
- Alembic migrations
- JWT access tokens and Argon2 password hashes

## Local setup (macOS / Linux)

```bash
cd backend
cp .env.example .env
docker compose up -d db
python3 -m venv .venv
source .venv/bin/activate
python -m pip install --upgrade pip
pip install -r requirements.txt
alembic upgrade head
uvicorn app.main:app --reload
```

OpenAPI docs: http://127.0.0.1:8000/docs

## Environment
Set unique strong values for JWT_SECRET_KEY and ADMIN_API_KEY before deploying. The .env.example values are placeholders. Never commit .env.

## API overview
- POST /api/v1/auth/register — create an account
- POST /api/v1/auth/login — receive a bearer token
- GET /api/v1/auth/me — current account
- GET /api/v1/fuel-types — active fuel types
- GET /api/v1/stations?region=fergana — station list
- GET /api/v1/stations/nearby?latitude=...&longitude=...&radius_meters=... — nearby stations
- POST /api/v1/stations — create a verified station (requires X-Admin-Key)
- POST /api/v1/stations/community — a driver adds a station (must be within 500 m of it; starts unverified)
- POST /api/v1/stations/{id}/votes — a nearby driver confirms ("exists") or disputes ("missing") a station; one vote per user
- GET /api/v1/stations/{id}/comments — comments, newest first (limit/offset)
- POST /api/v1/stations/{id}/comments — add a comment (authenticated, rate limited)
- POST /api/v1/stations/{id}/reports/status — authenticated status report
- POST /api/v1/stations/{id}/reports/queue — authenticated queue report
- GET /api/v1/stations/{id}/status — per-fuel consensus
- GET /api/v1/stations/{id}/queue/{fuel_type_id} — queue estimate

## Community stations
Drivers can add stations. Each station response carries `confirmations_count`, `missing_count`,
`confirmations_needed`, `is_verified` and `comments_count`, so the app can show "2/3 drivers confirmed".

- The creator's own vote is the first confirmation.
- A community station becomes verified after `STATION_VERIFY_THRESHOLD` (default 3) distinct drivers confirm it and
  confirmations outnumber "missing" votes. Admin-created stations are verified immediately.
- If `STATION_HIDE_THRESHOLD` (default 3) drivers say it is missing and they outnumber confirmations, a community station is hidden.
- Adding and voting require the driver to be within `STATION_VOTE_RADIUS_METERS` (default 500) of the station.
- Duplicates: adding a station within 100 m of an existing one returns 409 with the existing `station_id` to confirm instead.
- Limits: `MAX_NEW_STATIONS_PER_DAY`, `MAX_COMMENTS_PER_10_MIN`.

## Trust boundary
Reports are associated with authenticated accounts and submitted coordinates are checked against station coordinates. Client-provided GPS is not cryptographic proof of physical presence. Add device integrity signals, anomaly detection, moderation, and rate limiting before production. Community reports are not official station confirmations.

## Migration policy
Apply schema changes with Alembic before starting the API:

```bash
alembic upgrade head
```
