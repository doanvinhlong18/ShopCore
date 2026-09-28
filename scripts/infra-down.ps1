$ErrorActionPreference = "Stop"

# Named volumes are intentionally preserved.
docker compose down
exit $LASTEXITCODE
