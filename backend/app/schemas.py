from datetime import datetime
from typing import Literal
from pydantic import BaseModel, ConfigDict, Field

FuelCode = Literal["cng", "lpg", "gasoline", "diesel", "other"]
StatusCode = Literal["open", "closed", "no_fuel"]

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

class StatusReportCreate(BaseModel):
    fuel_type_id: FuelCode
    status: StatusCode
    reporter_id: str = Field(min_length=3, max_length=80)
    latitude: float = Field(ge=-90, le=90)
    longitude: float = Field(ge=-180, le=180)
    note: str | None = Field(default=None, max_length=300)

class QueueReportCreate(BaseModel):
    fuel_type_id: FuelCode
    reporter_id: str = Field(min_length=3, max_length=80)
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
