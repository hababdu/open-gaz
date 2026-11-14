"""Initial Open Gaz schema with PostGIS and user accounts."""
from alembic import op
from app.db import Base
from app import models  # noqa: F401

revision = "0001_initial"
down_revision = None
branch_labels = None
depends_on = None

def upgrade():
    op.execute("CREATE EXTENSION IF NOT EXISTS postgis")
    Base.metadata.create_all(bind=op.get_bind())

def downgrade():
    Base.metadata.drop_all(bind=op.get_bind())
