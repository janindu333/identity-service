# 🔴 Redis & Lua Script: Deep Dive Explanation

## 🎯 Overview

This document explains:
1. **How Redis is engaged** in the rate limiting process
2. **What Redis is doing** (storage, operations, TTL management)
3. **What the Lua script is doing** (atomic operations, logic)

---

## 🔌 Part 1: How Redis is Engaged

### 1.1 Redis Connection Setup

**File:** `RedisConfig.java`

```java
@Bean
public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory) {
    RedisTemplate<String, String> template = new RedisTemplate<>();
    template.setConnectionFactory(connectionFactory);
    // ... serializers ...
    return template;
}
```

**What happens:**
- Spring Boot creates a connection to Redis server
- Connection info from `application.properties`:
  - Host: `localhost`
  - Port: `6379`
  - Database: `0`
  - Connection Pool: Max 8 active connections

**Visual:**
```
┌─────────────────┐         TCP/IP         ┌─────────────────┐
│  Spring Boot    │ ◄──────────────────►  │  Redis Server   │
│  Application    │    Port 6379          │  (localhost)    │
└─────────────────┘                        └─────────────────┘
      │                                           │
      └─── RedisTemplate ────────────────────────┘
           (Connection Pool)
```

---

### 1.2 RedisTemplate - The Bridge

**In `RateLimiterService.java`:**

```java
private final RedisTemplate<String, String> redisTemplate;
```

**What `RedisTemplate` does:**
- Provides Java methods to interact with Redis
- Handles connection pooling (reuses connections)
- Serializes Java objects to Redis format
- Executes Redis commands and Lua scripts

**Connection Flow:**
```
Java Application
    ↓
RedisTemplate (Spring abstraction)
    ↓
Lettuce Connection Pool (async Redis client)
    ↓
TCP Socket Connection
    ↓
Redis Server (localhost:6379)
```

---

## 📦 Part 2: What Redis is Doing

### 2.1 Data Structure Used

**Redis stores rate limit counters as simple KEY-VALUE pairs:**

```
Key:   "auth:login:ip:192.168.1.100"
Value: "15"                    (string representation of number)
TTL:   45 seconds              (Time To Live - auto-expiration)
```

**Redis Data Type:** `STRING` (but stores numeric value)

### 2.2 Redis Operations

Redis performs these operations for rate limiting:

#### A. **INCR** (Increment)
```
INCR "auth:login:ip:192.168.1.100"
```
- If key doesn't exist: Creates it with value `1`
- If key exists: Increments the value by 1
- Returns: New value after increment

**Examples:**
```
Before: Key doesn't exist
Command: INCR "auth:login:ip:192.168.1.100"
After:  "auth:login:ip:192.168.1.100" = "1"

Before: "auth:login:ip:192.168.1.100" = "5"
Command: INCR "auth:login:ip:192.168.1.100"
After:  "auth:login:ip:192.168.1.100" = "6"
```

#### B. **PEXPIRE** (Set Expiration in Milliseconds)
```
PEXPIRE "auth:login:ip:192.168.1.100" 60000
```
- Sets key to expire after 60000 milliseconds (60 seconds)
- After expiration, Redis automatically deletes the key
- This creates a "sliding window" for rate limiting

**What happens:**
```
Time 0s:  Key created, PEXPIRE set to 60s
Time 45s: Key still exists, TTL = 15s remaining
Time 60s: Key automatically deleted by Redis
```

#### C. **PTTL** (Get Remaining Time in Milliseconds)
```
PTTL "auth:login:ip:192.168.1.100"
```
- Returns: Time remaining until key expires (in milliseconds)
- Returns `-1` if key has no expiration
- Returns `-2` if key doesn't exist

**Example:**
```
Key created at: 10:00:00
Current time:   10:00:25
TTL was set:    60 seconds
PTTL returns:   35000 (35 seconds remaining)
```

---

### 2.3 Redis Memory State Over Time

**Timeline Example:**

```
Time 0s:   First login request
           ┌──────────────────────────────────────┐
           │ Key: "auth:login:ip:192.168.1.100"  │
           │ Value: "1"                           │
           │ TTL: 60 seconds                      │
           └──────────────────────────────────────┘

Time 5s:   Second login request
           ┌──────────────────────────────────────┐
           │ Key: "auth:login:ip:192.168.1.100"  │
           │ Value: "2"                           │
           │ TTL: 55 seconds remaining            │
           └──────────────────────────────────────┘

Time 10s:  Third login request
           ┌──────────────────────────────────────┐
           │ Key: "auth:login:ip:192.168.1.100"  │
           │ Value: "3"                           │
           │ TTL: 50 seconds remaining            │
           └──────────────────────────────────────┘

Time 60s:  Key expires (automatically deleted)
           ┌──────────────────────────────────────┐
           │ Key: DELETED                         │
           │ (Window resets)                      │
           └──────────────────────────────────────┘
```

---

## 📜 Part 3: What the Lua Script is Doing

### 3.1 Why Lua Script?

**Problem without Lua:**
```java
// ❌ BAD: Multiple Redis commands (race conditions)
Long count = redisTemplate.opsForValue().increment(key);  // Command 1
if (count == 1) {
    redisTemplate.expire(key, Duration.ofSeconds(60));    // Command 2
}
```

**Issues:**
- Between commands, another request could interfere
- Not atomic (all-or-nothing)
- Race conditions possible

**Solution with Lua:**
```java
// ✅ GOOD: All operations in one atomic script
Long result = redisTemplate.execute(script, keys, args);  // Single command
```

**Benefits:**
- All operations execute atomically
- No race conditions
- Single network round-trip

---

### 3.2 The Lua Script Breakdown

**Full Script:**
```lua
local current = redis.call('incr', KEYS[1])
if current == 1 then
    redis.call('pexpire', KEYS[1], ARGV[1])
end
if current > tonumber(ARGV[2]) then
    return redis.call('pttl', KEYS[1])
end
return 0
```

**Parameters passed:**
- `KEYS[1]` = `"auth:login:ip:192.168.1.100"` (Redis key)
- `ARGV[1]` = `"60000"` (Window size in milliseconds)
- `ARGV[2]` = `"25"` (Maximum requests allowed)

---

### 3.3 Line-by-Line Explanation

#### **Line 1: Increment Counter**
```lua
local current = redis.call('incr', KEYS[1])
```

**What it does:**
- Calls Redis `INCR` command on the key
- Increments the counter value by 1
- If key doesn't exist, creates it with value 1
- Stores result in local variable `current`

**Redis Operation:**
```
INCR "auth:login:ip:192.168.1.100"
```

**Examples:**
```
Before: Key doesn't exist
After:  current = 1, Key = "1"

Before: Key = "5"
After:  current = 6, Key = "6"
```

---

#### **Lines 2-4: Set Expiration (First Request Only)**
```lua
if current == 1 then
    redis.call('pexpire', KEYS[1], ARGV[1])
end
```

**What it does:**
- Checks if this is the first request (`current == 1`)
- If yes, sets expiration time on the key
- Only runs once (when key is first created)

**Redis Operation:**
```
PEXPIRE "auth:login:ip:192.168.1.100" 60000
```

**Why only when current == 1?**
- Prevents resetting expiration on every request
- Maintains the original 60-second window
- If we set expiration on every request, the window would never end!

**Example:**
```
Request #1: current = 1 → Sets expiration to 60s ✅
Request #2: current = 2 → Skips (expiration already set)
Request #3: current = 3 → Skips (expiration already set)
```

---

#### **Lines 5-7: Check Limit (Block if Exceeded)**
```lua
if current > tonumber(ARGV[2]) then
    return redis.call('pttl', KEYS[1])
end
```

**What it does:**
- Compares current count with limit (`ARGV[2]` = 25)
- If exceeded: Returns remaining time until key expires
- This tells the application how long to wait

**Redis Operation:**
```
PTTL "auth:login:ip:192.168.1.100"
```

**Logic:**
```
If current > 25:
    Return: Time remaining (e.g., 35000 milliseconds)
    Meaning: BLOCKED ❌

If current <= 25:
    Skip this block, continue to next line
    Meaning: ALLOWED ✅
```

**Examples:**
```
Request #25: current = 25, 25 > 25? No → Continue
Request #26: current = 26, 26 > 25? Yes → Return TTL (e.g., 35000ms)
```

---

#### **Line 8: Return Success**
```lua
return 0
```

**What it does:**
- Returns `0` to Java application
- `0` means: Request is **ALLOWED** ✅
- Only reached if limit not exceeded

**Logic Flow:**
```
If we reach here:
  - Counter was incremented
  - Limit not exceeded (current <= 25)
  - Request should be allowed
```

---

## 🔄 Part 4: Complete Execution Examples

### Example 1: First Login Request (Allowed) ✅

**Input:**
- Key: `"auth:login:ip:192.168.1.100"`
- Window: `60000` milliseconds
- Limit: `25`

**Redis State Before:**
```
(Key doesn't exist)
```

**Lua Script Execution:**

```lua
Line 1: local current = redis.call('incr', "auth:login:ip:192.168.1.100")
        → Creates key, sets value to 1
        → current = 1

Line 2: if current == 1 then
        → TRUE (this is first request)

Line 3:     redis.call('pexpire', "auth:login:ip:192.168.1.100", 60000)
        → Sets expiration to 60 seconds

Line 5: if current > tonumber("25") then
        → 1 > 25? FALSE, skip

Line 8: return 0
        → Returns 0 (ALLOWED)
```

**Redis State After:**
```
Key: "auth:login:ip:192.168.1.100"
Value: "1"
TTL: 60 seconds
```

**Java Result:**
```java
ttlMillis = 0
→ Return RateLimitResult.allowed()
→ Request proceeds to authentication ✅
```

---

### Example 2: 25th Login Request (Still Allowed) ✅

**Input:**
- Key: `"auth:login:ip:192.168.1.100"`
- Window: `60000` milliseconds
- Limit: `25`

**Redis State Before:**
```
Key: "auth:login:ip:192.168.1.100"
Value: "24"
TTL: 15 seconds remaining
```

**Lua Script Execution:**

```lua
Line 1: local current = redis.call('incr', "auth:login:ip:192.168.1.100")
        → Increments 24 to 25
        → current = 25

Line 2: if current == 1 then
        → FALSE (not first request), skip

Line 5: if current > tonumber("25") then
        → 25 > 25? FALSE, skip

Line 8: return 0
        → Returns 0 (ALLOWED)
```

**Redis State After:**
```
Key: "auth:login:ip:192.168.1.100"
Value: "25"
TTL: 14 seconds remaining
```

**Java Result:**
```java
ttlMillis = 0
→ Return RateLimitResult.allowed()
→ Request proceeds to authentication ✅
```

---

### Example 3: 26th Login Request (Blocked) ❌

**Input:**
- Key: `"auth:login:ip:192.168.1.100"`
- Window: `60000` milliseconds
- Limit: `25`

**Redis State Before:**
```
Key: "auth:login:ip:192.168.1.100"
Value: "25"
TTL: 35 seconds remaining
```

**Lua Script Execution:**

```lua
Line 1: local current = redis.call('incr', "auth:login:ip:192.168.1.100")
        → Increments 25 to 26
        → current = 26

Line 2: if current == 1 then
        → FALSE, skip

Line 5: if current > tonumber("25") then
        → 26 > 25? TRUE!

Line 6:     return redis.call('pttl', "auth:login:ip:192.168.1.100")
        → Returns 34000 (34 seconds remaining)
        → Script exits here (doesn't reach line 8)
```

**Redis State After:**
```
Key: "auth:login:ip:192.168.1.100"
Value: "26"
TTL: 34 seconds remaining
```

**Java Result:**
```java
ttlMillis = 34000
→ retryAfterSeconds = 34
→ Return RateLimitResult.blocked(34)
→ Request BLOCKED ❌
→ HTTP 429 response sent immediately
```

---

## 🎯 Part 5: How Everything Works Together

### Complete Flow Diagram

```
┌─────────────────────────────────────────────────────────────┐
│ 1. Java: RateLimiterService.checkLimit() called             │
│    Key: "auth:login:ip:192.168.1.100"                      │
│    Limit: 25, Window: 60s                                   │
└────────────────────────────┬────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────┐
│ 2. Java: Prepare Lua script parameters                      │
│    KEYS[1] = "auth:login:ip:192.168.1.100"                │
│    ARGV[1] = "60000" (window in milliseconds)              │
│    ARGV[2] = "25" (limit)                                  │
└────────────────────────────┬────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────┐
│ 3. Java → Redis: Execute Lua script via RedisTemplate      │
│    redisTemplate.execute(script, keys, args)               │
└────────────────────────────┬────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────┐
│ 4. Redis: Execute Lua script ATOMICALLY                     │
│                                                             │
│    Step 1: INCR key → Increment counter                    │
│    Step 2: If first request, SET expiration                │
│    Step 3: If exceeded limit, RETURN time remaining        │
│    Step 4: Otherwise RETURN 0                              │
└────────────────────────────┬────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────┐
│ 5. Redis → Java: Return result                              │
│    Result: 0 (allowed) OR 35000 (blocked, 35s remaining)   │
└────────────────────────────┬────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────┐
│ 6. Java: Process result                                     │
│    If 0 → RateLimitResult.allowed()                        │
│    If >0 → RateLimitResult.blocked(retryAfterSeconds)      │
└────────────────────────────┬────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────┐
│ 7. Java: Return to Controller                               │
│    If allowed → Continue to authentication                 │
│    If blocked → Return HTTP 429 immediately                │
└─────────────────────────────────────────────────────────────┘
```

---

## 🔑 Key Concepts Summary

### What Redis is Doing:

1. ✅ **Storing counters** - Keeps track of request counts per IP/username
2. ✅ **Managing expiration** - Automatically deletes keys after TTL
3. ✅ **Executing commands atomically** - Via Lua script execution
4. ✅ **Providing fast in-memory storage** - Sub-millisecond operations

### What the Lua Script is Doing:

1. ✅ **Atomic increment** - Safely increments counter
2. ✅ **Window management** - Sets expiration only on first request
3. ✅ **Limit checking** - Compares count against limit
4. ✅ **Return decision** - Returns 0 (allow) or TTL (block)

### Why This Approach Works:

1. ✅ **Thread-safe** - Lua script ensures atomic operations
2. ✅ **Fast** - Single network round-trip to Redis
3. ✅ **Accurate** - No race conditions or timing issues
4. ✅ **Efficient** - Keys auto-expire, no manual cleanup needed
5. ✅ **Scalable** - Redis handles high concurrency well

---

## 📊 Redis Memory View

**What you'd see if you connect to Redis CLI:**

```bash
redis-cli

# Check all rate limit keys
KEYS auth:login:*

# Output:
1) "auth:login:ip:192.168.1.100"
2) "auth:login:ip:203.0.113.50"
3) "auth:login:user:john@example.com"

# Check value and TTL
GET "auth:login:ip:192.168.1.100"
# Output: "15"

TTL "auth:login:ip:192.168.1.100"
# Output: 45 (seconds remaining)

# Watch in real-time
MONITOR
# (Shows all Redis commands as they execute)
```

---

## 🎓 Summary

**Redis Role:**
- Fast in-memory database
- Stores request counters
- Manages automatic expiration
- Executes Lua scripts atomically

**Lua Script Role:**
- Ensures atomic operations (all-or-nothing)
- Increments counter safely
- Sets expiration window
- Checks limit and returns decision

**Together:**
- Prevents race conditions
- Provides accurate rate limiting
- Handles high concurrency
- Auto-cleans expired keys

This combination makes rate limiting **fast, accurate, and reliable**! 🚀

