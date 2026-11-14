import secrets
from datetime import datetime, timedelta, timezone
from fastapi import APIRouter, Depends, Header, HTTPException, Query
from geoalchemy2.elements import WKTElement
from sqlalchemy import func, select
from sqlalchemy.orm import Session
from app.config import settings
from app.db import get_db
from app.dependencies import get_current_user
from app.models import Station, StationComment, StationVote, User, utcnow
from app.schemas import (CommunityStationCreate, NearbyStationOut, StationCreate, StationOut,
                         StationVoteCreate, StationVoteOut)
from app.services.geo import distance_meters
from app.services.verification import is_verified, should_hide

router = APIRouter(prefix="/stations", tags=["stations"])

REGIONS = {"fergana", "andijan", "namangan"}
DUPLICATE_RADIUS_METERS = 100


def require_admin(x_admin_key: str | None = Header(default=None)):
    if not settings.admin_api_key or not x_admin_key or not secrets.compare_digest(x_admin_key, settings.admin_api_key):
        raise HTTPException(403, "Valid admin API key required")


def vote_counts(db: Session, station_ids: list[str]) -> dict[str, dict[str, int]]:
    counts = {sid: {"exists": 0, "missing": 0, "comments": 0} for sid in station_ids}
    if not station_ids:
        return counts
    for sid, vote, n in db.execute(
        select(StationVote.station_id, StationVote.vote, func.count())
        .where(StationVote.station_id.in_(station_ids)).group_by(StationVote.station_id, StationVote.vote)
    ):
        counts[sid][vote] = n
    for sid, n in db.execute(
        select(StationComment.station_id, func.count())
        .where(StationComment.station_id.in_(station_ids)).group_by(StationComment.station_id)
    ):
        counts[sid]["comments"] = n
    return counts


def station_out(station: Station, counts: dict[str, int]) -> dict:
    exists, missing = counts["exists"], counts["missing"]
    return {
        "id": station.id, "name": station.name, "address": station.address, "region": station.region,
        "latitude": station.latitude, "longitude": station.longitude, "is_active": station.is_active,
        "source": station.source,
        "confirmations_count": exists, "missing_count": missing,
        "confirmations_needed": 0 if station.source == "admin" else settings.station_verify_threshold,
        "is_verified": is_verified(station.source, exists, missing, settings.station_verify_threshold),
        "comments_count": counts["comments"],
    }


def load_station(db: Session, station_id: str) -> Station:
    station = db.get(Station, station_id)
    if not station or not station.is_active:
        raise HTTPException(404, "Station not found")
    return station


@router.post("", response_model=StationOut, status_code=201, dependencies=[Depends(require_admin)])
def create_station(payload: StationCreate, db: Session = Depends(get_db)):
    station = Station(**payload.model_dump(), source="admin",
        location=WKTElement(f"POINT({payload.longitude} {payload.latitude})", srid=4326))
    db.add(station)
    db.commit()
    db.refresh(station)
    return station_out(station, vote_counts(db, [station.id])[station.id])


@router.post("/community", response_model=StationOut, status_code=201)
def create_community_station(payload: CommunityStationCreate, db: Session = Depends(get_db),
                             user: User = Depends(get_current_user)):
    """Haydovchi yangi shahobcha qo'shadi. U boshqa haydovchilar tasdiqlaguncha "tasdiqlanmagan" turadi."""
    if distance_meters(payload.latitude, payload.longitude,
                       payload.reporter_latitude, payload.reporter_longitude) > settings.station_vote_radius_meters:
        raise HTTPException(403, "Siz shahobchadan juda uzoqdasiz")

    since = utcnow() - timedelta(days=1)
    added_today = db.scalar(select(func.count()).select_from(Station).where(
        Station.created_by_id == user.id, Station.created_at >= since))
    if added_today >= settings.max_new_stations_per_day:
        raise HTTPException(429, "Bugun juda ko'p shahobcha qo'shdingiz, ertaga urinib ko'ring")

    # Takrorlanishni oldini olish: 100 m ichida allaqachon shahobcha bo'lsa, uni tasdiqlashni taklif qilamiz.
    point = func.ST_SetSRID(func.ST_MakePoint(payload.longitude, payload.latitude), 4326).cast(Station.location.type)
    near = db.scalar(select(Station).where(
        Station.is_active.is_(True), func.ST_DWithin(Station.location, point, DUPLICATE_RADIUS_METERS)).limit(1))
    if near:
        raise HTTPException(409, detail={"message": "Bu yerda shahobcha allaqachon bor, uni tasdiqlang",
                                         "station_id": near.id})

    station = Station(name=payload.name.strip(), address=payload.address.strip(), region=payload.region,
        latitude=payload.latitude, longitude=payload.longitude, source="community", created_by_id=user.id,
        location=WKTElement(f"POINT({payload.longitude} {payload.latitude})", srid=4326))
    db.add(station)
    db.flush()
    # Qo'shgan haydovchi o'zi ham "bor" deb ovoz beradi (birinchi tasdiq).
    db.add(StationVote(station_id=station.id, user_id=user.id, vote="exists"))
    db.commit()
    db.refresh(station)
    return station_out(station, vote_counts(db, [station.id])[station.id])


@router.get("/nearby", response_model=list[NearbyStationOut])
def nearby_stations(latitude: float = Query(ge=-90, le=90),
                    longitude: float = Query(ge=-180, le=180),
                    radius_meters: int = Query(default=10000, ge=100, le=50000),
                    db: Session = Depends(get_db)):
    point = func.ST_SetSRID(func.ST_MakePoint(longitude, latitude), 4326).cast(Station.location.type)
    distance = func.ST_Distance(Station.location, point).label("distance_meters")
    query = select(Station, distance).where(
        Station.is_active.is_(True), func.ST_DWithin(Station.location, point, radius_meters)
    ).order_by(distance).limit(100)
    rows = db.execute(query).all()
    counts = vote_counts(db, [s.id for s, _ in rows])
    return [{**station_out(s, counts[s.id]), "distance_meters": round(m, 1)} for s, m in rows]


@router.get("", response_model=list[StationOut])
def list_stations(region: str | None = Query(default=None), db: Session = Depends(get_db)):
    query = select(Station).where(Station.is_active.is_(True))
    if region:
        if region not in REGIONS:
            raise HTTPException(422, "Unsupported region")
        query = query.where(Station.region == region)
    stations = list(db.scalars(query.order_by(Station.name)).all())
    counts = vote_counts(db, [s.id for s in stations])
    return [station_out(s, counts[s.id]) for s in stations]


@router.get("/{station_id}", response_model=StationOut)
def get_station(station_id: str, db: Session = Depends(get_db)):
    station = load_station(db, station_id)
    return station_out(station, vote_counts(db, [station.id])[station.id])


@router.post("/{station_id}/votes", response_model=StationVoteOut)
def vote_station(station_id: str, payload: StationVoteCreate, db: Session = Depends(get_db),
                 user: User = Depends(get_current_user)):
    """Haydovchi "bu yerda shahobcha bor / yo'q" deb ovoz beradi. Bir foydalanuvchi bitta ovoz
    beradi (qayta bossa ovozi almashadi). Faqat shahobcha yonidagi haydovchi ovoz bera oladi."""
    station = load_station(db, station_id)
    if distance_meters(station.latitude, station.longitude,
                       payload.latitude, payload.longitude) > settings.station_vote_radius_meters:
        raise HTTPException(403, "Siz shahobchadan juda uzoqdasiz")

    existing = db.scalar(select(StationVote).where(
        StationVote.station_id == station_id, StationVote.user_id == user.id))
    if existing:
        existing.vote = payload.vote
        existing.updated_at = utcnow()
    else:
        db.add(StationVote(station_id=station_id, user_id=user.id, vote=payload.vote))
    db.flush()

    counts = vote_counts(db, [station_id])[station_id]
    if should_hide(station.source, counts["exists"], counts["missing"], settings.station_hide_threshold):
        station.is_active = False
    db.commit()
    db.refresh(station)
    return {"my_vote": payload.vote, "station": station_out(station, counts)}
