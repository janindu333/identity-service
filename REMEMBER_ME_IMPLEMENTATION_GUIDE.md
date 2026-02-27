# Remember Me Implementation Guide

## Overview

This guide explains the HTTP-only cookie-based "Remember Me" functionality implementation for the barber booking system's identity service.

## 🔐 Security Implementation

### What We Implemented

**Industry Standard Approach: HTTP-Only Cookies**
- ✅ Refresh tokens stored in HTTP-only cookies (XSS protection)
- ✅ Access tokens returned in response body (stored in memory by frontend)
- ✅ Dynamic expiration based on "remember me" flag
- ✅ Secure, SameSite=Strict cookies (CSRF protection)
- ✅ Separate refresh endpoint for token renewal
- ✅ Logout endpoint to clear cookies

---

## 📋 API Endpoints

### 1. Login with Remember Me

**Endpoint:** `POST /auth/login`

**Request Body:**
```json
{
  "usernameOrEmail": "user@example.com",
  "password": "myPassword123",
  "rememberMe": true
}
```

**Response:**
```json
{
  "success": true,
  "message": null,
  "code": 0,
  "error": null,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": null,
    "expiresIn": 7200
  }
}
```

**Response Headers:**
```
Set-Cookie: refreshToken=<jwt_token>; 
  HttpOnly; 
  Secure; 
  Path=/; 
  Max-Age=2592000; 
  SameSite=Strict
```

**Token Expiration:**
- `rememberMe = true`: Refresh token valid for **30 days**
- `rememberMe = false`: Refresh token valid for **7 days**
- Access token: **Always 2 hours** (regardless of rememberMe)

---

### 2. Refresh Access Token

**Endpoint:** `POST /auth/refresh`

**Request:** No body needed (cookie sent automatically)

**Response:**
```json
{
  "success": true,
  "message": null,
  "code": 0,
  "error": null,
  "data": {
    "accessToken": "new_access_token_here",
    "refreshToken": null,
    "expiresIn": 7200
  }
}
```

---

### 3. Logout

**Endpoint:** `POST /auth/logout`

**Request:** No body needed

**Response:**
```json
{
  "success": true,
  "message": "Logged out successfully",
  "code": 0,
  "error": null,
  "data": null
}
```

**Effect:** Clears the refresh token cookie by setting Max-Age=0

---

## 🖥️ Frontend Integration

### React/Angular/Vue Example

```javascript
// Login function
async function login(email, password, rememberMe) {
  const response = await fetch('http://localhost:8080/auth/login', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    credentials: 'include', // IMPORTANT: Include cookies
    body: JSON.stringify({
      usernameOrEmail: email,
      password: password,
      rememberMe: rememberMe
    })
  });

  const result = await response.json();
  
  if (result.success) {
    // Store access token in memory (React state/context, NOT localStorage)
    setAccessToken(result.data.accessToken);
    
    // Refresh token is automatically stored in HTTP-only cookie
    // No need to manually handle it
    
    console.log('Login successful!');
    console.log('Access token expires in:', result.data.expiresIn, 'seconds');
  }
}

// Refresh access token function
async function refreshAccessToken() {
  const response = await fetch('http://localhost:8080/auth/refresh', {
    method: 'POST',
    credentials: 'include', // Cookie sent automatically
  });

  const result = await response.json();
  
  if (result.success) {
    setAccessToken(result.data.accessToken);
    return result.data.accessToken;
  }
  
  return null;
}

// Logout function
async function logout() {
  await fetch('http://localhost:8080/auth/logout', {
    method: 'POST',
    credentials: 'include',
  });

  // Clear access token from memory
  setAccessToken(null);
  
  // Redirect to login page
  window.location.href = '/login';
}

// API call with automatic token refresh
async function apiCall(url, options = {}) {
  // Add access token to request
  options.headers = {
    ...options.headers,
    'Authorization': `Bearer ${accessToken}`
  };
  options.credentials = 'include'; // Always include cookies

  let response = await fetch(url, options);

  // If access token expired (401), try to refresh
  if (response.status === 401) {
    const newToken = await refreshAccessToken();
    
    if (newToken) {
      // Retry with new token
      options.headers.Authorization = `Bearer ${newToken}`;
      response = await fetch(url, options);
    } else {
      // Refresh failed, redirect to login
      window.location.href = '/login';
    }
  }

  return response;
}
```

---

## 🔧 Backend Configuration

### Cookie Security Settings

The cookie is configured with the following security attributes:

```java
ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken)
    .httpOnly(true)    // Cannot be accessed by JavaScript (XSS protection)
    .secure(true)      // Only sent over HTTPS
    .path("/")         // Available for all paths
    .maxAge(maxAge)    // 30 days or 7 days based on rememberMe
    .sameSite("Strict") // CSRF protection
    .build();
```

### CORS Configuration (Required)

For cookies to work with frontend, configure CORS in your API Gateway or Spring Boot:

```java
@Configuration
public class CorsConfig {
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                    .allowedOrigins("http://localhost:3000") // Your frontend URL
                    .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH")
                    .allowedHeaders("*")
                    .allowCredentials(true); // CRITICAL: Allow cookies
            }
        };
    }
}
```

---

## 🏗️ Architecture Flow

### Login Flow
```
1. User enters credentials + checks "Remember Me"
   ↓
2. Frontend sends POST /auth/login with rememberMe: true
   ↓
3. Backend validates credentials
   ↓
4. Backend generates:
   - Access Token (2 hours)
   - Refresh Token (30 days if rememberMe, 7 days otherwise)
   ↓
5. Backend sets refresh token in HTTP-only cookie
   ↓
6. Backend returns access token in response body
   ↓
7. Frontend stores access token in memory (React state)
   ↓
8. Browser automatically stores cookie (HTTP-only)
```

### Token Refresh Flow
```
1. Access token expires after 2 hours
   ↓
2. API call returns 401 Unauthorized
   ↓
3. Frontend calls POST /auth/refresh (no body needed)
   ↓
4. Browser automatically sends refresh token cookie
   ↓
5. Backend validates refresh token from cookie
   ↓
6. Backend generates new access token
   ↓
7. Frontend updates access token in memory
   ↓
8. Frontend retries original API call
```

### Logout Flow
```
1. User clicks logout
   ↓
2. Frontend calls POST /auth/logout
   ↓
3. Backend clears refresh token cookie (Max-Age=0)
   ↓
4. Frontend clears access token from memory
   ↓
5. User redirected to login page
```

---

## 🛡️ Security Features

### XSS Protection
- Refresh token in **HTTP-only cookie** → JavaScript cannot access it
- Even if XSS attack occurs, attacker only gets short-lived access token (2 hours)

### CSRF Protection
- **SameSite=Strict** → Cookie only sent to same domain
- Prevents cross-site request forgery attacks

### Token Expiration Strategy
- Access Token: **2 hours** → Minimize damage if stolen
- Refresh Token: **7-30 days** → Can be revoked if needed

### HTTPS Only
- `Secure` flag → Cookie only sent over HTTPS
- Prevents man-in-the-middle attacks

---

## 📝 Important Notes

### For Development (localhost)

If testing on localhost without HTTPS, temporarily set `secure(false)`:

```java
ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken)
    .httpOnly(true)
    .secure(false)  // Only for localhost testing
    .path("/")
    .maxAge(maxAge)
    .sameSite("Lax")  // Use "Lax" for localhost, "Strict" for production
    .build();
```

### For Production

Always use:
- `secure(true)` - HTTPS only
- `sameSite("Strict")` - Maximum CSRF protection
- Valid SSL certificate

---

## 🧪 Testing

### Test Remember Me = True

```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "usernameOrEmail": "test@example.com",
    "password": "password123",
    "rememberMe": true
  }' \
  -c cookies.txt -v
```

Expected: `Set-Cookie` header with `Max-Age=2592000` (30 days)

### Test Remember Me = False

```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "usernameOrEmail": "test@example.com",
    "password": "password123",
    "rememberMe": false
  }' \
  -c cookies.txt -v
```

Expected: `Set-Cookie` header with `Max-Age=604800` (7 days)

### Test Refresh Token

```bash
curl -X POST http://localhost:8080/auth/refresh \
  -b cookies.txt -v
```

Expected: New access token in response

### Test Logout

```bash
curl -X POST http://localhost:8080/auth/logout \
  -b cookies.txt -v
```

Expected: `Set-Cookie` header with `Max-Age=0` (clears cookie)

---

## 🆚 Comparison: Old vs New

| Feature | Old Implementation | New Implementation |
|---------|-------------------|-------------------|
| Refresh Token Storage | Query parameter/localStorage | HTTP-only cookie |
| XSS Protection | ❌ Vulnerable | ✅ Protected |
| Remember Me | ❌ Not implemented | ✅ Implemented |
| Token Expiration | Fixed 7 days | Dynamic (7-30 days) |
| CSRF Protection | ❌ None | ✅ SameSite=Strict |
| Industry Standard | ❌ No | ✅ Yes |

---

## 🔮 Future Enhancements

### 1. Refresh Token Rotation
Generate a new refresh token each time it's used:
```java
@PostMapping("/refresh")
public BaseResponse<TokenResponse> refreshAccessToken(
    HttpServletRequest request, HttpServletResponse response) {
    
    String oldRefreshToken = getRefreshTokenFromCookie(request);
    
    // Generate new access token
    String accessToken = service.getAccessTokenByRefreshToken(oldRefreshToken);
    
    // Generate new refresh token (rotation)
    String newRefreshToken = service.rotateRefreshToken(oldRefreshToken);
    
    // Update cookie with new refresh token
    setRefreshTokenCookie(response, newRefreshToken, rememberMe);
    
    return new BaseResponse<>(true, null, 0, null, 
        new TokenResponse(accessToken, 7200L));
}
```

### 2. Refresh Token Blacklist
Store revoked tokens in Redis:
```java
@Service
public class TokenBlacklistService {
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    public void blacklistToken(String token) {
        String tokenId = jwtService.extractTokenId(token);
        long expirationSeconds = jwtService.getExpirationSeconds(token);
        redisTemplate.opsForValue().set(
            "blacklist:" + tokenId, 
            "revoked", 
            expirationSeconds, 
            TimeUnit.SECONDS
        );
    }
    
    public boolean isBlacklisted(String token) {
        String tokenId = jwtService.extractTokenId(token);
        return redisTemplate.hasKey("blacklist:" + tokenId);
    }
}
```

### 3. Device Tracking
Track which devices have active sessions:
```java
@Entity
public class UserSession {
    @Id
    private Long id;
    private Long userId;
    private String deviceInfo;
    private String ipAddress;
    private LocalDateTime loginTime;
    private LocalDateTime lastActivity;
    private String refreshTokenHash;
    private boolean rememberMe;
}
```

---

## 📚 References

- [OWASP JWT Security Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/JSON_Web_Token_for_Java_Cheat_Sheet.html)
- [MDN: Set-Cookie](https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Set-Cookie)
- [RFC 6265: HTTP State Management Mechanism](https://datatracker.ietf.org/doc/html/rfc6265)

---

## ✅ Implementation Checklist

- [x] AuthRequest updated with rememberMe field
- [x] TokenResponse updated with expiresIn field
- [x] JwtService supports custom expiration times
- [x] AuthService handles rememberMe logic
- [x] AuthController sets HTTP-only cookies
- [x] New /auth/refresh endpoint created
- [x] Logout endpoint clears cookies
- [x] Security attributes configured (HttpOnly, Secure, SameSite)
- [ ] CORS configuration added (API Gateway/Spring Boot)
- [ ] Frontend updated to use credentials: 'include'
- [ ] Production HTTPS certificate configured
- [ ] Testing completed in development environment

---

**Author:** Identity Service Team  
**Date:** October 10, 2025  
**Version:** 1.0

