$ErrorActionPreference = "Stop"

$repositoryRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$outputDirectory = Join-Path $repositoryRoot ".local\jwt"

java (Join-Path $PSScriptRoot "GenerateJwtKeyPair.java") $outputDirectory

Write-Host "JWT_PRIVATE_KEY_LOCATION=file:./.local/jwt/private.pem"
Write-Host "JWT_PUBLIC_KEY_LOCATION=file:./.local/jwt/public.pem"
