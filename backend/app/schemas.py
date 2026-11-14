from datetime import datetime
from typing import Literal
from pydantic import BaseModel, ConfigDict, EmailStr, Field

FuelCode = Literal["cng", "lpg", "gasoline", "diesel", "other"]
StatusCode = Literal["open", "closed", "no_fuel"]

class RegisterRequest(BaseModel):
    email: EmailStr
    display_name: str = Field(min_length=2, max_length=100)
    password: str = Field(min_length=10, max_length=128)

class LoginRequest(BaseModel):
    email: EmailStr
    password: str = Field(min_length=1, max_length=128)

class UserOut(BaseModel):
    model_config = ConfigDict(from_attributes=True)
    id: str
    email: EmailStr
    display_name: str
    created_at: datetime

class AuthResponse(BaseModel):
    access_token: str
    token_type: str = "bearer"
    user: UserOut

class StationCreate(BaseModel):
    name: str = Field(min_length=2, max_length=180)
    address: str = Field(default="", max_length=300)
    region: Literal["fergana", "andijan", "namangan"]
    latitude: float = Field(ge=-90, le=90)
    longitude: float = Field(ge=-180, le=180)

class StationOut(BaseModel):
    model_config = ConfigDict(from_attributes=True)
    id: str
    name: str
    address: str
    region: str
    latitude: float
    longitude: float
    is_active: bool
    source: Literal["admin", "community"] = "admin"
    # Necha haydovchi "bu yerda shahobcha bor" / "yo'q" degan.
    confirmations_count: int = 0
    missing_count: int = 0
    # Tasdiqlangan deb hisoblash uchun nechta tasdiq kerak (UI da "2/3" ko'rsatish uchun).
    confirmations_needed: int = 0
    is_verified: bool = True
    comments_count: int = 0

class CommunityStationCreate(BaseModel):
    name: str = Field(min_length=2, max_length=180)
    address: str = Field(default="", max_length=300)
    region: Literal["fergana", "andijan", "namangan"]
    latitude: float = Field(ge=-90, le=90)
    longitude: float = Field(ge=-180, le=180)
    # Haydovchining hozirgi joyi: faqat shahobcha yonida turgan odam qo'sha oladi.
    reporter_latitude: float = Field(ge=-90, le=90)
    reporter_longitude: float = Field(ge=-180, le=180)

class StationVoteCreate(BaseModel):
    vote: Literal["exists", "missing"]
    latitude: float = Field(ge=-90, le=90)
    longitude: float = Field(ge=-180, le=180)

class StationVoteOut(BaseModel):
    my_vote: Literal["exists", "missing"]
    station: StationOut

class CommentCreate(BaseModel):
    text: str = Field(min_length=1, max_length=500)

class CommentOut(BaseModel):
    id: str
    author_name: str
    text: str
    created_at: datetime

class StatusReportCreate(BaseModel):
    fuel_type_id: FuelCode
    status: StatusCode
    latitude: float = Field(ge=-90, le=90)
    longitude: float = Field(ge=-180, le=180)
    note: str | None = Field(default=None, max_length=300)

class QueueReportCreate(BaseModel):
    fuel_type_id: FuelCode
    cars_count: int = Field(ge=0, le=500)
    latitude: float = Field(ge=-90, le=90)
    longitude: float = Field(ge=-180, le=180)

class ReportAccepted(BaseModel):
    id: str
    accepted: bool = True
    created_at: datetime

class FuelStatusOut(BaseModel):
    fuel_type_id: str
    status: Literal["open", "closed", "unknown"]
    confidence: float
    report_count: int
    last_updated: datetime | None

class QueueEstimateOut(BaseModel):
    fuel_type_id: str
    estimated_cars: float | None
    report_count: int
    last_updated: datetime | None

class NearbyStationOut(StationOut):
    distance_meters: float
