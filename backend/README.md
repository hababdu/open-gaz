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
- POST /api/v1/stations — create a station (requires X-Admin-Key)
- POST /api/v1/stations/{id}/reports/status — authenticated status report
- POST /api/v1/stations/{id}/reports/queue — authenticated queue report
- GET /api/v1/stations/{id}/status — per-fuel consensus
- GET /api/v1/stations/{id}/queue/{fuel_type_id} — queue estimate

## Trust boundary
Reports are associated with authenticated accounts and submitted coordinates are checked against station coordinates. Client-provided GPS is not cryptographic proof of physical presence. Add device integrity signals, anomaly detection, moderation, and rate limiting before production. Community reports are not official station confirmations.

## Migration policy
Apply schema changes with Alembic before starting the API:

```bash
alembic upgrade head
```
