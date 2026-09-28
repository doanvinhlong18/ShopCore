$ErrorActionPreference = "Stop"

& "$PSScriptRoot\..\mvnw.cmd" clean verify
exit $LASTEXITCODE
