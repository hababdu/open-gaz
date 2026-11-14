from datetime import timedelta
from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy import func, select
from sqlalchemy.orm import Session
from app.config import settings
from app.db import get_db
from app.dependencies import get_current_user
from app.models import Station, StationComment, User, utcnow
from app.schemas import CommentCreate, CommentOut

router = APIRouter(prefix="/stations", tags=["comments"])


def _station_or_404(db: Session, station_id: str) -> Station:
    station = db.get(Station, station_id)
    if not station or not station.is_active:
        raise HTTPException(404, "Station not found")
    return station


@router.get("/{station_id}/comments", response_model=list[CommentOut])
def list_comments(station_id: str, limit: int = Query(default=30, ge=1, le=100),
                  offset: int = Query(default=0, ge=0), db: Session = Depends(get_db)):
    _station_or_404(db, station_id)
    rows = db.execute(
        select(StationComment, User.display_name).join(User, User.id == StationComment.user_id)
        .where(StationComment.station_id == station_id)
        .order_by(StationComment.created_at.desc()).limit(limit).offset(offset)
    ).all()
    return [{"id": c.id, "author_name": name, "text": c.text, "created_at": c.created_at} for c, name in rows]


@router.post("/{station_id}/comments", response_model=CommentOut, status_code=201)
def add_comment(station_id: str, payload: CommentCreate, db: Session = Depends(get_db),
                user: User = Depends(get_current_user)):
    _station_or_404(db, station_id)
    text = " ".join(payload.text.split())  # ortiqcha bo'sh joy va qator tashlashlarni olib tashlaymiz
    if not text:
        raise HTTPException(422, "Izoh bo'sh bo'lmasligi kerak")
    recent = db.scalar(select(func.count()).select_from(StationComment).where(
        StationComment.user_id == user.id, StationComment.created_at >= utcnow() - timedelta(minutes=10)))
    if recent >= settings.max_comments_per_10_min:
        raise HTTPException(429, "Juda tez-tez izoh yozyapsiz, birozdan keyin urinib ko'ring")
    comment = StationComment(station_id=station_id, user_id=user.id, text=text)
    db.add(comment)
    db.commit()
    db.refresh(comment)
    return {"id": comment.id, "author_name": user.display_name, "text": comment.text,
            "created_at": comment.created_at}
