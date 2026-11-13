from fastapi import FastAPI

app = FastAPI(
    title="Open Gaz API",
    description="Community-powered automotive gas station availability for Uzbekistan.",
    version="0.1.0",
)


@app.get("/health", tags=["system"])
async def health() -> dict[str, str]:
    return {"status": "ok", "service": "open-gaz-api"}


@app.get("/api/v1", tags=["system"])
async def api_info() -> dict[str, str]:
    return {
        "name": "Open Gaz API",
        "version": "0.1.0",
        "status": "foundation",
    }
