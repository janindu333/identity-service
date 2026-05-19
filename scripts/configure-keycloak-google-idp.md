# Keycloak: Google Identity Provider

Google sign-in requires a **Google OAuth 2.0 Client** and a **Google Identity Provider** in realm `saloon2`.

## 1. Google Cloud Console

1. Create OAuth 2.0 credentials (Web application).
2. **Authorized redirect URI** (Keycloak broker callback):

   `http://localhost:9090/realms/saloon2/broker/google/endpoint`

   (Use your public Keycloak URL in production.)

3. Note **Client ID** and **Client secret**.

## 2. Keycloak Admin

1. Realm **saloon2** → **Identity providers** → **Google**.
2. Set alias **`google`** (must match `auth.google.idp-alias`).
3. Paste Google Client ID / secret.
4. Enable **Trust email**.
5. Save.

## 3. Keycloak client (e.g. `api-gateway-test`)

- Enable **Standard flow** (authorization code).
- Valid redirect URIs must include your frontend callback, e.g.  
  `http://localhost:3000/auth/google/callback`
- For ID token exchange (`POST /auth/google`): enable **Token exchange** and allow exchange from `google` IdP (Client scopes / permissions).

## 4. Identity service environment

```properties
GOOGLE_SIGNIN_ENABLED=true
GOOGLE_CLIENT_ID=<your-google-web-client-id>
GOOGLE_REDIRECT_URI=http://localhost:3000/auth/google/callback
GOOGLE_ALLOWED_REDIRECT_URIS=http://localhost:3000/auth/google/callback
KEYCLOAK_GOOGLE_IDP_ALIAS=google
```

## API usage

| Flow | Endpoint |
|------|----------|
| Browser redirect | `GET /auth/google/authorize?redirectUri=...&prompt=select_account` → open `authorizationUrl` (account chooser) |
| Logout | `POST /auth/logout?postLogoutRedirectUri=http://localhost:3000` → use `data.keycloakLogoutUrl` in browser |
| After Keycloak redirect | `POST /auth/google/callback` `{ "code", "redirectUri" }` |
| SPA / mobile (Google Sign-In button) | `POST /auth/google` `{ "idToken", "role" }` |
