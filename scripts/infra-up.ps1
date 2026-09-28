$ErrorActionPreference = "Stop"

docker compose up -d
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}

docker compose ps
