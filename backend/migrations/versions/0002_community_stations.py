"""Community stations: source/created_by, driver votes, comments."""
import sqlalchemy as sa
from alembic import op

revision = "0002_community_stations"
down_revision = "0001_initial"
branch_labels = None
depends_on = None


def upgrade():
    bind = op.get_bind()
    insp = sa.inspect(bind)
    # 0001 Base.metadata.create_all ishlatgani uchun yangi bazada bular allaqachon
    # bo'lishi mumkin; shuning uchun mavjudligini tekshiramiz.
    station_cols = {c["name"] for c in insp.get_columns("stations")}
    if "source" not in station_cols:
        op.add_column("stations", sa.Column("source", sa.String(12), nullable=False, server_default="admin"))
    if "created_by_id" not in station_cols:
        op.add_column("stations", sa.Column("created_by_id", sa.String(36), nullable=True))
        op.create_foreign_key("fk_stations_created_by", "stations", "users", ["created_by_id"], ["id"], ondelete="SET NULL")
        op.create_index("ix_stations_created_by_id", "stations", ["created_by_id"])

    tables = set(insp.get_table_names())
    if "station_votes" not in tables:
        op.create_table(
            "station_votes",
            sa.Column("id", sa.String(36), primary_key=True),
            sa.Column("station_id", sa.String(36), sa.ForeignKey("stations.id", ondelete="CASCADE"), nullable=False),
            sa.Column("user_id", sa.String(36), sa.ForeignKey("users.id", ondelete="CASCADE"), nullable=False),
            sa.Column("vote", sa.String(8), nullable=False),
            sa.Column("created_at", sa.DateTime(timezone=True), nullable=False),
            sa.Column("updated_at", sa.DateTime(timezone=True), nullable=False),
            sa.UniqueConstraint("station_id", "user_id", name="uq_station_vote_user"),
        )
    if "station_comments" not in tables:
        op.create_table(
            "station_comments",
            sa.Column("id", sa.String(36), primary_key=True),
            sa.Column("station_id", sa.String(36), sa.ForeignKey("stations.id", ondelete="CASCADE"), nullable=False),
            sa.Column("user_id", sa.String(36), sa.ForeignKey("users.id", ondelete="RESTRICT"), nullable=False),
            sa.Column("text", sa.String(500), nullable=False),
            sa.Column("created_at", sa.DateTime(timezone=True), nullable=False),
        )
        op.create_index("ix_station_comments_user_id", "station_comments", ["user_id"])
        op.create_index("ix_station_comments_created_at", "station_comments", ["created_at"])
        op.create_index("ix_comment_station_created", "station_comments", ["station_id", "created_at"])


def downgrade():
    op.drop_table("station_comments")
    op.drop_table("station_votes")
    op.drop_index("ix_stations_created_by_id", table_name="stations")
    op.drop_constraint("fk_stations_created_by", "stations", type_="foreignkey")
    op.drop_column("stations", "created_by_id")
    op.drop_column("stations", "source")
