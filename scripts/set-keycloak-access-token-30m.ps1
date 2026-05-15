# Sets Keycloak realm saloon2 access token lifespan to 30 minutes (1800 seconds).
# Requires Keycloak admin on http://localhost:9090 (default admin/admin).

$KeycloakUrl = if ($env:KEYCLOAK_AUTH_URL) { $env:KEYCLOAK_AUTH_URL } else { "http://localhost:9090" }
$Realm = if ($env:KEYCLOAK_REALM) { $env:KEYCLOAK_REALM } else { "saloon2" }
$AdminUser = if ($env:KEYCLOAK_ADMIN_USER) { $env:KEYCLOAK_ADMIN_USER } else { "admin" }
$AdminPassword = if ($env:KEYCLOAK_ADMIN_PASSWORD) { $env:KEYCLOAK_ADMIN_PASSWORD } else { "admin" }

$tokenResponse = curl.exe -s -X POST "$KeycloakUrl/realms/master/protocol/openid-connect/token" `
    -H "Content-Type: application/x-www-form-urlencoded" `
    -d "grant_type=password&client_id=admin-cli&username=$AdminUser&password=$AdminPassword" | ConvertFrom-Json

if (-not $tokenResponse.access_token) {
    Write-Error "Failed to obtain Keycloak admin token. Check admin credentials and Keycloak URL."
    exit 1
}

$adminToken = $tokenResponse.access_token
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$bodyFile = Join-Path $scriptDir "keycloak-access-token-30m.json"

$status = curl.exe -s -o NUL -w "%{http_code}" -X PUT "$KeycloakUrl/admin/realms/$Realm" `
    -H "Authorization: Bearer $adminToken" `
    -H "Content-Type: application/json" `
    --data-binary "@$bodyFile"

if ($status -ne "204") {
    Write-Error "Realm update failed with HTTP $status"
    exit 1
}

$lifespan = (curl.exe -s "$KeycloakUrl/admin/realms/$Realm" -H "Authorization: Bearer $adminToken" | ConvertFrom-Json).accessTokenLifespan
Write-Host "Realm '$Realm' accessTokenLifespan = $lifespan seconds ($([int]$lifespan / 60) minutes). Users must log in again for new tokens."
