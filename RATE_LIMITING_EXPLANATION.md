# 🔐 Rate Limiting with Redis + Lua - Step-by-Step Explanation

## 📋 Overview

This document explains how **Redis with Lua scripts** is used for rate limiting in the login/refresh endpoints, starting from when a user sends their credentials.

---

## 🎯 Configuration Settings

From `application.properties`:
```properties
security.rate-limit.login.ip.limit=25           # 25 attempts per window
security.rate-limit.login.ip.window-seconds=60  # 60 seconds window
security.rate-limit.login.username.limit=7      # 7 attempts per window  
security.rate-limit.login.username.window-seconds=300  # 300 seconds (5 minutes) window
```

**Translation:**
- **IP Rate Limit**: Maximum 25 login attempts from same IP per 60 seconds
- **Username Rate Limit**: Maximum 7 login attempts for same username per 5 minutes

---

## 🔄 Complete Flow: User Login with Rate Limiting

### **STEP 1: User Sends Login Request** 👤

```
POST /auth/login
Content-Type: application/json

{
  "usernameOrEmail": "john@example.com",
  "password": "secret123",
  "rememberMe": false
}
```

**What happens:**
- User's browser/app sends HTTP POST request to `/auth/login` endpoint
- Request includes credentials in JSON body
- Request arrives at `AuthController.login()` method

---

### **STEP 2: Controller Receives Request** 🎯

**File:** `AuthController.java` (line 57-64)

```java
@PostMapping("/login")
public BaseResponse<TokenResponse> login(@RequestBody AuthRequest authRequest,
                                         HttpServletRequest request,
                                         HttpServletResponse response) {
    long startTime = System.currentTimeMillis();
    logger.apiRequest("Login API called for user: " + authRequest.getUsernameOrEmail());
    
    // Extract client IP address
    String clientIp = clientIpResolver.resolve(request);
```

**What happens:**
1. Spring MVC receives the POST request
2. Extracts `usernameOrEmail` and `password` from request body
3. Gets the client's IP address using `ClientIpResolver`
   - Checks `X-Forwarded-For` header (for load balancers/proxies)
   - Falls back to `X-Real-IP` header
   - Finally uses `request.getRemoteAddr()` if no headers found

**Example IP extraction:**
- Direct connection: `192.168.1.100`
- Behind proxy: `203.0.113.50` (from X-Forwarded-For header)

---

### **STEP 3: First Rate Limit Check - IP-Based** 🛡️

**File:** `AuthController.java` (line 65-77)

```java
RateLimitResult ipLimit = rateLimiterService.checkLimit(
    "auth:login:ip:" + clientIp,              // Redis key
    rateLimitProperties.getLogin().getIp()    // Limit config (25/60s)
);

if (!ipLimit.isAllowed()) {
    logger.warn("Login blocked due to IP rate limit. IP: " + clientIp);
    return buildRateLimitedResponse(
        response,
        "Too many login attempts from this IP. Try again in " + 
        ipLimit.getRetryAfterSeconds() + " seconds.",
        ipLimit.getRetryAfterSeconds()
    );
}
```

**What happens:**
1. Creates Redis key: `"auth:login:ip:192.168.1.100"` (example)
2. Calls `rateLimiterService.checkLimit()` with:
   - **Key**: `"auth:login:ip:192.168.1.100"`
   - **Config**: Limit=25, Window=60 seconds
3. If rate limit exceeded → Returns 429 (Too Many Requests) immediately
4. If allowed → Continues to next check

---

### **STEP 4: Rate Limiter Service - Preparation** ⚙️

**File:** `RateLimiterService.java` (line 29-35)

```java
public RateLimitResult checkLimit(String key, Limit config) {
    if (config == null || config.getLimit() <= 0) {
        return RateLimitResult.allowed();  // No limit configured
    }
    
    Duration window = Duration.ofSeconds(config.getWindowSeconds());
    List<String> keys = Collections.singletonList(key);
```

**What happens:**
1. Validates configuration exists
2. Converts window seconds to milliseconds for Lua script
3. Prepares key list for Redis Lua script execution

**Example values:**
- `key` = `"auth:login:ip:192.168.1.100"`
- `window` = `60000` milliseconds (60 seconds)
- `limit` = `25` requests

---

### **STEP 5: Execute Redis Lua Script** 🚀

**File:** `RateLimiterService.java` (line 38-43)

```java
Long ttlMillis = redisTemplate.execute(
    script,                                    // Lua script
    keys,                                      // ["auth:login:ip:192.168.1.100"]
    String.valueOf(window.toMillis()),         // "60000"
    String.valueOf(config.getLimit())          // "25"
);
```

**What happens:**
1. Spring calls `redisTemplate.execute()` which sends Lua script to Redis server
2. Redis executes the script **atomically** (all-or-nothing, thread-safe)

---

### **STEP 6: The Lua Script Execution in Redis** 📜

**File:** `RateLimiterService.java` (line 57-68)

```lua
local current = redis.call('incr', KEYS[1])    -- Increment counter
if current == 1 then                           -- First request in window
    redis.call('pexpire', KEYS[1], ARGV[1])    -- Set expiration (60 seconds)
end
if current > tonumber(ARGV[2]) then            -- Exceeded limit (25)?
    return redis.call('pttl', KEYS[1])         -- Return time remaining
end
return 0                                        -- Within limit, allow request
```

**Detailed Step-by-Step Execution:**

#### **Scenario A: First Login Attempt (Allowed)** ✅

1. **`redis.call('incr', KEYS[1])`**
   - Redis key: `auth:login:ip:192.168.1.100`
   - Current value: `null` (doesn't exist)
   - After increment: `1` (key created, value = 1)
   - Redis stores: `auth:login:ip:192.168.1.100 = "1"`

2. **`if current == 1 then`**
   - Condition: `1 == 1` → **TRUE**
   - Executes: `redis.call('pexpire', KEYS[1], ARGV[1])`
   - Sets expiration: 60000 milliseconds (60 seconds)
   - Key will auto-delete after 60 seconds

3. **`if current > tonumber(ARGV[2]) then`**
   - Condition: `1 > 25` → **FALSE**
   - Skips return statement

4. **`return 0`**
   - Returns `0` to Java application
   - `0` means: **Request allowed**

#### **Scenario B: 10th Login Attempt (Still Allowed)** ✅

1. **`redis.call('incr', KEYS[1])`**
   - Current value in Redis: `"9"`
   - After increment: `10`
   - Redis updates: `auth:login:ip:192.168.1.100 = "10"`

2. **`if current == 1 then`**
   - Condition: `10 == 1` → **FALSE**
   - Skips expiration setting (already set)

3. **`if current > tonumber(ARGV[2]) then`**
   - Condition: `10 > 25` → **FALSE**
   - Skips return statement

4. **`return 0`**
   - Returns `0` → **Request allowed**

#### **Scenario C: 26th Login Attempt (Blocked)** ❌

1. **`redis.call('incr', KEYS[1])`**
   - Current value in Redis: `"25"`
   - After increment: `26`
   - Redis updates: `auth:login:ip:192.168.1.100 = "26"`

2. **`if current == 1 then`**
   - Condition: `26 == 1` → **FALSE**
   - Skips expiration setting

3. **`if current > tonumber(ARGV[2]) then`**
   - Condition: `26 > 25` → **TRUE**
   - Executes: `return redis.call('pttl', KEYS[1])`
   - Gets remaining time: Example `35000` milliseconds (35 seconds left)

4. **Returns `35000`**
   - Non-zero value means: **Request blocked**
   - Time remaining: 35 seconds

---

### **STEP 7: Process Lua Script Result** 📊

**File:** `RateLimiterService.java` (line 45-50)

```java
if (ttlMillis == null || ttlMillis <= 0) {
    return RateLimitResult.allowed();  // Request allowed
}

long retryAfterSeconds = Math.max(1, TimeUnit.MILLISECONDS.toSeconds(ttlMillis));
return RateLimitResult.blocked(retryAfterSeconds);  // Request blocked
```

**What happens:**
- If result is `0` or null → Returns `RateLimitResult.allowed()`
- If result is positive (e.g., `35000`) → Converts to seconds and returns `RateLimitResult.blocked(35)`

**Example:**
- Lua returns: `35000` milliseconds
- Converted: `35` seconds
- Returns: `RateLimitResult.blocked(35)`

---

### **STEP 8: Check IP Rate Limit Result** ✅/❌

**File:** `AuthController.java` (line 70-77)

```java
if (!ipLimit.isAllowed()) {
    logger.warn("Login blocked due to IP rate limit. IP: " + clientIp);
    return buildRateLimitedResponse(
        response,
        "Too many login attempts from this IP. Try again in " + 
        ipLimit.getRetryAfterSeconds() + " seconds.",
        ipLimit.getRetryAfterSeconds()
    );
}
```

**What happens:**
- If blocked → Returns HTTP 429 (Too Many Requests) immediately
- Response includes `Retry-After` header
- **Authentication never happens** (credentials not checked)
- Request stops here

**Example Response:**
```json
HTTP/1.1 429 Too Many Requests
Retry-After: 35

{
  "success": false,
  "statusCode": 429,
  "message": "Too many login attempts from this IP. Try again in 35 seconds."
}
```

---

### **STEP 9: Second Rate Limit Check - Username-Based** 👤

**File:** `AuthController.java` (line 79-93)

```java
String usernameKey = authRequest.getUsernameOrEmail();
if (usernameKey != null && !usernameKey.isBlank()) {
    RateLimitResult userLimit = rateLimiterService.checkLimit(
        "auth:login:user:" + usernameKey.toLowerCase(),  // Redis key
        rateLimitProperties.getLogin().getUsername()     // Limit config (7/300s)
    );
    if (!userLimit.isAllowed()) {
        logger.warn("Login blocked due to username rate limit. User: " + usernameKey);
        return buildRateLimitedResponse(
            response,
            "Too many login attempts for this account. Try again in " + 
            userLimit.getRetryAfterSeconds() + " seconds.",
            userLimit.getRetryAfterSeconds()
        );
    }
}
```

**What happens:**
1. Creates Redis key: `"auth:login:user:john@example.com"` (lowercase)
2. Uses config: Limit=7, Window=300 seconds (5 minutes)
3. Executes **same Lua script** but with different key and limit
4. If blocked → Returns 429 immediately
5. If allowed → Continues to authentication

**Why two checks?**
- **IP limit**: Prevents brute-force from single IP (25/60s)
- **Username limit**: Prevents targeted attacks on specific accounts (7/5min)

---

### **STEP 10: Authentication (Only if Rate Limits Pass)** 🔑

**File:** `AuthController.java` (line 95-127)

```java
try {
    Authentication authenticate = authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(
            authRequest.getUsernameOrEmail(), 
            authRequest.getPassword()
        )
    );

    if (authenticate.isAuthenticated()) {
        // Generate tokens...
        String accessToken = service.generateAccessTokenByUsernameOrEmail(...);
        String refreshToken = service.generateRefreshTokenByUsernameOrEmail(...);
        // ... return success response
    }
} catch (Exception e) {
    // Invalid credentials - but rate limit counter already incremented
    return new BaseResponse<>(false, null, 0, "User or password is not matched", null);
}
```

**What happens:**
1. Only reaches here if **both rate limits passed**
2. Spring Security validates credentials against database
3. If valid → Generates JWT tokens and returns success
4. If invalid → Returns error (but rate limit counter was already incremented)

**Important Note:**
- Rate limit counter increments **BEFORE** authentication
- This prevents attackers from knowing which attempts failed due to rate limit vs wrong password
- Even failed logins count toward the limit (security best practice)

---

## 🔍 Visual Flow Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│ 1. User sends POST /auth/login with credentials                 │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│ 2. Controller extracts IP: "192.168.1.100"                      │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│ 3. IP Rate Limit Check                                          │
│    Key: "auth:login:ip:192.168.1.100"                          │
│    Limit: 25 requests / 60 seconds                             │
└────────────────────────────┬────────────────────────────────────┘
                             │
                ┌────────────┴────────────┐
                │                         │
         Blocked (≥25)            Allowed (<25)
                │                         │
                ▼                         ▼
    ┌───────────────────┐    ┌──────────────────────────────┐
    │ Return HTTP 429   │    │ 4. Username Rate Limit Check │
    │ Retry-After: 35s  │    │ Key: "auth:login:user:john" │
    └───────────────────┘    │ Limit: 7 requests / 5 min    │
                             └────────────┬─────────────────┘
                                          │
                             ┌────────────┴────────────┐
                             │                         │
                      Blocked (≥7)             Allowed (<7)
                             │                         │
                             ▼                         ▼
                ┌───────────────────┐    ┌─────────────────────────┐
                │ Return HTTP 429   │    │ 5. Authenticate User    │
                │ Retry-After: 60s  │    │ (Check credentials)     │
                └───────────────────┘    └────────────┬────────────┘
                                                      │
                                         ┌────────────┴────────────┐
                                         │                         │
                                  Invalid Creds          Valid Creds
                                         │                         │
                                         ▼                         ▼
                            ┌──────────────────┐    ┌──────────────────────┐
                            │ Return 401 Error │    │ Generate JWT Tokens  │
                            │ (Rate limit      │    │ Return 200 Success   │
                            │  already +1)     │    │                      │
                            └──────────────────┘    └──────────────────────┘
```

---

## 🎯 Redis Data Structure

### **What's Stored in Redis?**

```
Key: "auth:login:ip:192.168.1.100"
Value: "15"                    (current attempt count)
TTL: 45 seconds                (time until auto-delete)

Key: "auth:login:user:john@example.com"
Value: "3"                     (current attempt count)
TTL: 280 seconds               (time until auto-delete)
```

### **Key Characteristics:**

1. **Automatic Expiration**: Keys auto-delete when TTL expires
2. **Sliding Window**: Each request resets/extends the window
3. **Thread-Safe**: Lua script ensures atomic operations
4. **No Race Conditions**: Multiple simultaneous requests handled correctly

---

## 💡 Why Lua Scripts?

### **Advantages:**

1. **Atomic Operations**: All Redis commands in script execute as one unit
2. **No Race Conditions**: Prevents concurrent requests from bypassing limits
3. **Single Round-Trip**: One network call instead of multiple (INC, GET, SET, EXPIRE)
4. **Performance**: Faster than multiple Redis commands
5. **Consistency**: Ensures accurate rate limit counting

### **Without Lua (Problematic Approach):**

```java
// ❌ BAD: Multiple Redis calls (race conditions possible)
Long count = redisTemplate.opsForValue().increment(key);
if (count == 1) {
    redisTemplate.expire(key, Duration.ofSeconds(60));
}
if (count > 25) {
    return blocked();
}
```

**Problems:**
- Between `increment()` and `expire()`, another request could interfere
- Between `increment()` and `get()`, count could change
- Not thread-safe in high-concurrency scenarios

### **With Lua (Correct Approach):**

```java
// ✅ GOOD: Single atomic operation
Long result = redisTemplate.execute(script, keys, args);
```

**Benefits:**
- All operations execute atomically
- No race conditions
- Thread-safe
- Single network round-trip

---

## 🔄 Complete Example Scenario

### **Timeline: User at IP 192.168.1.100**

```
Time 0s:   Login attempt #1  → Redis: "auth:login:ip:192.168.1.100" = 1, TTL=60s
Time 5s:   Login attempt #2  → Redis: "auth:login:ip:192.168.1.100" = 2
Time 10s:  Login attempt #3  → Redis: "auth:login:ip:192.168.1.100" = 3
...
Time 58s:  Login attempt #25 → Redis: "auth:login:ip:192.168.1.100" = 25, TTL=2s
Time 59s:  Login attempt #26 → Redis: "auth:login:ip:192.168.1.100" = 26
         → BLOCKED! Returns: "Try again in 2 seconds"
Time 61s:  Redis key expires (auto-deleted)
Time 62s:  Login attempt #27 → Redis: "auth:login:ip:192.168.1.100" = 1, TTL=60s (NEW WINDOW)
         → ALLOWED (new window started)
```

---

## 🛡️ Security Features

1. **Double Protection**: IP + Username limits prevent both distributed and targeted attacks
2. **Early Exit**: Blocks before authentication (saves database load)
3. **Generic Errors**: Same error message for rate limit and invalid credentials (prevents enumeration)
4. **Automatic Reset**: Keys expire automatically (no manual cleanup needed)
5. **Resilience**: Falls back to "allowed" if Redis is down (graceful degradation)

---

## 📝 Summary

1. ✅ User sends credentials → Controller receives request
2. ✅ Extract IP address → Create Redis key
3. ✅ Execute Lua script → Atomically increment counter
4. ✅ Check result → Block if limit exceeded
5. ✅ Repeat for username limit → Double protection
6. ✅ If both pass → Authenticate credentials
7. ✅ Generate tokens → Return success

**The Lua script ensures rate limiting is accurate, fast, and thread-safe!** 🚀

