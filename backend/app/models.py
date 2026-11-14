from datetime import datetime, timezone
from uuid import uuid4
from geoalchemy2 import Geography
from sqlalchemy import Boolean, DateTime, Float, ForeignKey, Index, Integer, String
from sqlalchemy.orm import Mapped, mapped_column, relationship
from app.db import Base

def utcnow() -> datetime:
    return datetime.now(timezone.utc)

class User(Base):
    __tablename__ = "users"
    id: Mapped[str] = mapped_column(String(36), primary_key=True, default=lambda: str(uuid4()))
    email: Mapped[str] = mapped_column(String(254), nullable=False, unique=True, index=True)
    display_name: Mapped[str] = mapped_column(String(100), nullable=False)
    password_hash: Mapped[str] = mapped_column(String(255), nullable=False)
    is_active: Mapped[bool] = mapped_column(Boolean, nullable=False, default=True)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False, default=utcnow)

class Station(Base):
    __tablename__ = "stations"
    id: Mapped[str] = mapped_column(String(36), primary_key=True, default=lambda: str(uuid4()))
    name: Mapped[str] = mapped_column(String(180), nullable=False, index=True)
    address: Mapped[str] = mapped_column(String(300), nullable=False, default="")
    region: Mapped[str] = mapped_column(String(40), nullable=False, index=True)
    latitude: Mapped[float] = mapped_column(Float, nullable=False)
    longitude: Mapped[float] = mapped_column(Float, nullable=False)
    location: Mapped[object] = mapped_column(Geography(geometry_type="POINT", srid=4326), nullable=False)
    is_active: Mapped[bool] = mapped_column(Boolean, nullable=False, default=True)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow, nullable=False)
    reports: Mapped[list["StatusReport"]] = relationship(back_populates="station")
    queue_reports: Mapped[list["QueueReport"]] = relationship(back_populates="station")

class FuelType(Base):
    __tablename__ = "fuel_types"
    id: Mapped[str] = mapped_column(String(24), primary_key=True)
    name: Mapped[str] = mapped_column(String(80), nullable=False, unique=True)
    is_active: Mapped[bool] = mapped_column(Boolean, default=True, nullable=False)

class StatusReport(Base):
    __tablename__ = "status_reports"
    id: Mapped[str] = mapped_column(String(36), primary_key=True, default=lambda: str(uuid4()))
    station_id: Mapped[str] = mapped_column(ForeignKey("stations.id", ondelete="CASCADE"), nullable=False)
    fuel_type_id: Mapped[str] = mapped_column(ForeignKey("fuel_types.id"), nullable=False)
    reporter_id: Mapped[str] = mapped_column(ForeignKey("users.id", ondelete="RESTRICT"), nullable=False, index=True)
    status: Mapped[str] = mapped_column(String(20), nullable=False)
    latitude: Mapped[float] = mapped_column(Float, nullable=False)
    longitude: Mapped[float] = mapped_column(Float, nullable=False)
    location: Mapped[object] = mapped_column(Geography(geometry_type="POINT", srid=4326), nullable=False)
    note: Mapped[str | None] = mapped_column(String(300))
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow, nullable=False, index=True)
    station: Mapped[Station] = relationship(back_populates="reports")
    __table_args__ = (Index("ix_status_station_fuel_created", "station_id", "fuel_type_id", "created_at"),)

class QueueReport(Base):
    __tablename__ = "queue_reports"
    id: Mapped[str] = mapped_column(String(36), primary_key=True, default=lambda: str(uuid4()))
    station_id: Mapped[str] = mapped_column(ForeignKey("stations.id", ondelete="CASCADE"), nullable=False)
    fuel_type_id: Mapped[str] = mapped_column(ForeignKey("fuel_types.id"), nullable=False)
    reporter_id: Mapped[str] = mapped_column(ForeignKey("users.id", ondelete="RESTRICT"), nullable=False, index=True)
    cars_count: Mapped[int] = mapped_column(Integer, nullable=False)
    latitude: Mapped[float] = mapped_column(Float, nullable=False)
    longitude: Mapped[float] = mapped_column(Float, nullable=False)
    location: Mapped[object] = mapped_column(Geography(geometry_type="POINT", srid=4326), nullable=False)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow, nullable=False, index=True)
    station: Mapped[Station] = relationship(back_populates="queue_reports")
    __table_args__ = (Index("ix_queue_station_fuel_created", "station_id", "fuel_type_id", "created_at"),)
