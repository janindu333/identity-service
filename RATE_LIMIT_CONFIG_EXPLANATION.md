# 🔍 What is `rateLimitProperties.getLogin().getIp()` Sending?

## Chain of Method Calls

```java
rateLimitProperties.getLogin().getIp()
     ↓                    ↓              ↓
RateLimitProperties   Endpoint       Limit
```

## Step-by-Step Breakdown

### Step 1: `rateLimitProperties.getLogin()`
**Returns:** `Endpoint` object

**From `RateLimitProperties.java`:**
```java
public class RateLimitProperties {
    private Endpoint login = new Endpoint();  // Instance variable
    
    public Endpoint getLogin() {
        return login;  // Returns the Endpoint object
    }
}
```

**The `Endpoint` object contains:**
```java
public static class Endpoint {
    private Limit ip = new Limit();         // IP rate limit config
    private Limit username = new Limit();   // Username rate limit config
}
```

---

### Step 2: `.getIp()` on the Endpoint
**Returns:** `Limit` object

**From `RateLimitProperties.java`:**
```java
public static class Endpoint {
    private Limit ip = new Limit();  // Instance variable
    
    public Limit getIp() {
        return ip;  // Returns the Limit object
    }
}
```

---

### Step 3: What's Inside the `Limit` Object?

**The `Limit` class structure:**
```java
public static class Limit {
    private int limit = 0;              // Maximum requests allowed
    private long windowSeconds = 60;    // Time window in seconds
}
```

**From `application.properties`:**
```properties
security.rate-limit.login.ip.limit=25
security.rate-limit.login.ip.window-seconds=60
```

**Spring Boot automatically binds these properties to the `Limit` object:**
- `limit` = **25** (from `security.rate-limit.login.ip.limit`)
- `windowSeconds` = **60** (from `security.rate-limit.login.ip.window-seconds`)

---

## What Gets Passed to `rateLimiterService.checkLimit()`?

### The Method Signature:
```java
public RateLimitResult checkLimit(String key, Limit config)
                                    ↑              ↑
                              Redis key      Limit object
```

### In the Controller:
```java
RateLimitResult ipLimit = rateLimiterService.checkLimit(
    "auth:login:ip:" + clientIp,           // String: "auth:login:ip:192.168.1.100"
    rateLimitProperties.getLogin().getIp() // Limit object with limit=25, windowSeconds=60
);
```

---

## What the `Limit` Object Contains (Actual Values)

Based on `application.properties`:

```java
Limit config = rateLimitProperties.getLogin().getIp();

config.getLimit()         → 25
config.getWindowSeconds() → 60
```

**Visual representation:**
```
Limit {
    limit = 25
    windowSeconds = 60
}
```

---

## How These Values Are Used in `RateLimiterService`

**In `RateLimiterService.checkLimit()` method:**

```java
public RateLimitResult checkLimit(String key, Limit config) {
    // Extract values from the Limit object
    Duration window = Duration.ofSeconds(config.getWindowSeconds());  // 60 seconds
    
    // Execute Lua script with these values
    Long ttlMillis = redisTemplate.execute(
        script,
        keys,                                    // ["auth:login:ip:192.168.1.100"]
        String.valueOf(window.toMillis()),       // "60000" (60 seconds in milliseconds)
        String.valueOf(config.getLimit())        // "25" (maximum requests)
    );
}
```

---

## Complete Flow with Values

### 1. Configuration Loading (Application Startup)
```
application.properties
    ↓
security.rate-limit.login.ip.limit=25
security.rate-limit.login.ip.window-seconds=60
    ↓
Spring Boot @ConfigurationProperties binding
    ↓
RateLimitProperties {
    login = Endpoint {
        ip = Limit {
            limit = 25
            windowSeconds = 60
        }
    }
}
```

### 2. When Login Request Arrives
```
Controller receives request
    ↓
rateLimitProperties.getLogin()
    ↓
Returns: Endpoint {
    ip = Limit { limit=25, windowSeconds=60 }
    username = Limit { limit=7, windowSeconds=300 }
}
    ↓
.getIp()
    ↓
Returns: Limit {
    limit = 25
    windowSeconds = 60
}
    ↓
Passed to: rateLimiterService.checkLimit("auth:login:ip:192.168.1.100", limitObject)
```

### 3. Inside RateLimiterService
```java
checkLimit("auth:login:ip:192.168.1.100", limitObject)
    ↓
Extract: config.getLimit() → 25
Extract: config.getWindowSeconds() → 60
    ↓
Convert: 60 seconds → 60000 milliseconds
    ↓
Execute Lua Script:
    KEYS[1] = "auth:login:ip:192.168.1.100"
    ARGV[1] = "60000"  (window in milliseconds)
    ARGV[2] = "25"     (maximum requests)
```

---

## Visual Representation

```
┌─────────────────────────────────────────────────────────────┐
│ rateLimitProperties.getLogin().getIp()                      │
│                                                             │
│  Returns: Limit Object                                      │
│  ┌───────────────────────────────────────────────┐          │
│  │ limit: 25                                      │          │
│  │ windowSeconds: 60                              │          │
│  └───────────────────────────────────────────────┘          │
│                                                             │
│  This object gets passed to:                                │
│  rateLimiterService.checkLimit(key, config)                 │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│ Inside checkLimit() method:                                 │
│                                                             │
│  config.getLimit()          → 25                            │
│  config.getWindowSeconds()  → 60                            │
│                                                             │
│  Converted to:                                              │
│  - windowMillis = 60000                                     │
│  - limit = 25                                               │
│                                                             │
│  Sent to Redis Lua script:                                  │
│  ARGV[1] = "60000"                                          │
│  ARGV[2] = "25"                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## Summary

**What `rateLimitProperties.getLogin().getIp()` sends:**

✅ A `Limit` object containing:
- **limit** = `25` (maximum requests allowed)
- **windowSeconds** = `60` (time window in seconds)

**These values come from:**
- `application.properties` → `security.rate-limit.login.ip.limit=25`
- `application.properties` → `security.rate-limit.login.ip.window-seconds=60`

**How it's used:**
1. Extracted in `RateLimiterService.checkLimit()`
2. Converted: `60 seconds` → `60000 milliseconds`
3. Passed to Redis Lua script as:
   - `ARGV[1]` = `"60000"` (window expiration)
   - `ARGV[2]` = `"25"` (maximum count)

**Translation:** 
- "Allow maximum **25 login attempts** from the same IP address within a **60-second** time window"

