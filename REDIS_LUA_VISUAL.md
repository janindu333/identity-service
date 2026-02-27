# 🎨 Redis & Lua Script - Visual Guide

## 🔄 Complete Interaction Flow

```
┌─────────────────────────────────────────────────────────────────┐
│                     JAVA APPLICATION                             │
│                                                                 │
│  RateLimiterService.checkLimit()                                │
│    Key: "auth:login:ip:192.168.1.100"                         │
│    Limit: 25, Window: 60 seconds                               │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             │ redisTemplate.execute()
                             │ (Sends Lua script to Redis)
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                        REDIS SERVER                              │
│                                                                 │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │              LUA SCRIPT EXECUTION                         │  │
│  │                                                           │  │
│  │  1. INCR key → Increment counter                         │  │
│  │     "auth:login:ip:192.168.1.100" = "15"                │  │
│  │                                                           │  │
│  │  2. IF first request → SET expiration                    │  │
│  │     PEXPIRE key 60000 (60 seconds)                       │  │
│  │                                                           │  │
│  │  3. IF count > limit → RETURN remaining time            │  │
│  │     ELSE → RETURN 0                                      │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                 │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │              MEMORY STORAGE                               │  │
│  │                                                           │  │
│  │  Key:   "auth:login:ip:192.168.1.100"                   │  │
│  │  Value: "15"                                              │  │
│  │  TTL:   45 seconds remaining                             │  │
│  └──────────────────────────────────────────────────────────┘  │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             │ Returns result
                             │ (0 = allowed, >0 = blocked)
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                     JAVA APPLICATION                             │
│                                                                 │
│  Process result:                                                │
│    If 0 → ALLOWED ✅ (continue to authentication)              │
│    If >0 → BLOCKED ❌ (return HTTP 429)                        │
└─────────────────────────────────────────────────────────────────┘
```

---

## 📦 What Redis Stores

### Data Structure

```
┌─────────────────────────────────────────────────────────┐
│  Redis Key-Value Pair                                   │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  Key:   "auth:login:ip:192.168.1.100"                 │
│         └──────────┬──────────┘                         │
│                    │                                    │
│                    Unique identifier                    │
│                    (IP address in this case)            │
│                                                         │
│  ───────────────────────────────────────────            │
│                                                         │
│  Value: "15"                                           │
│         └──┬──┘                                         │
│            │                                            │
│            Request count (as string)                    │
│                                                         │
│  ───────────────────────────────────────────            │
│                                                         │
│  TTL:    45 seconds                                     │
│          └──┬──┘                                        │
│             │                                           │
│             Time until auto-deletion                    │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

### Multiple Keys Example

```
Redis Memory:

┌─────────────────────────────────────┬──────────┬──────────┐
│ Key                                 │ Value    │ TTL      │
├─────────────────────────────────────┼──────────┼──────────┤
│ auth:login:ip:192.168.1.100        │ "15"     │ 45s      │
│ auth:login:ip:203.0.113.50         │ "8"      │ 52s      │
│ auth:login:user:john@example.com   │ "3"      │ 280s     │
│ auth:login:user:jane@example.com   │ "1"      │ 295s     │
└─────────────────────────────────────┴──────────┴──────────┘
```

---

## 🎬 Step-by-Step: What Happens Inside Redis

### Request #1 (First Request)

```
┌──────────────────────────────────────────────────────────┐
│ BEFORE (Redis Memory):                                   │
│   (empty - key doesn't exist)                            │
└──────────────────────────────────────────────────────────┘
                    │
                    ▼
┌──────────────────────────────────────────────────────────┐
│ LUA SCRIPT EXECUTION:                                    │
│                                                          │
│ Line 1: INCR "auth:login:ip:192.168.1.100"             │
│   → Creates key, value = 1                              │
│                                                          │
│ Line 2: IF current == 1 THEN                            │
│   → TRUE (first request)                                │
│                                                          │
│ Line 3:   PEXPIRE key 60000                             │
│   → Sets expiration to 60 seconds                       │
│                                                          │
│ Line 5: IF 1 > 25 THEN                                  │
│   → FALSE, skip                                         │
│                                                          │
│ Line 8: RETURN 0                                        │
│   → ALLOWED ✅                                          │
└──────────────────────────────────────────────────────────┘
                    │
                    ▼
┌──────────────────────────────────────────────────────────┐
│ AFTER (Redis Memory):                                    │
│                                                          │
│   Key: "auth:login:ip:192.168.1.100"                   │
│   Value: "1"                                             │
│   TTL: 60 seconds                                        │
└──────────────────────────────────────────────────────────┘
```

---

### Request #15 (Within Limit)

```
┌──────────────────────────────────────────────────────────┐
│ BEFORE (Redis Memory):                                   │
│                                                          │
│   Key: "auth:login:ip:192.168.1.100"                   │
│   Value: "14"                                            │
│   TTL: 48 seconds remaining                             │
└──────────────────────────────────────────────────────────┘
                    │
                    ▼
┌──────────────────────────────────────────────────────────┐
│ LUA SCRIPT EXECUTION:                                    │
│                                                          │
│ Line 1: INCR "auth:login:ip:192.168.1.100"             │
│   → Increments 14 → 15                                  │
│                                                          │
│ Line 2: IF current == 1 THEN                            │
│   → FALSE (not first request)                           │
│   → Skip expiration setting                             │
│                                                          │
│ Line 5: IF 15 > 25 THEN                                 │
│   → FALSE, skip                                         │
│                                                          │
│ Line 8: RETURN 0                                        │
│   → ALLOWED ✅                                          │
└──────────────────────────────────────────────────────────┘
                    │
                    ▼
┌──────────────────────────────────────────────────────────┐
│ AFTER (Redis Memory):                                    │
│                                                          │
│   Key: "auth:login:ip:192.168.1.100"                   │
│   Value: "15"                                            │
│   TTL: 47 seconds remaining                             │
└──────────────────────────────────────────────────────────┘
```

---

### Request #26 (Blocked)

```
┌──────────────────────────────────────────────────────────┐
│ BEFORE (Redis Memory):                                   │
│                                                          │
│   Key: "auth:login:ip:192.168.1.100"                   │
│   Value: "25"                                            │
│   TTL: 35 seconds remaining                             │
└──────────────────────────────────────────────────────────┘
                    │
                    ▼
┌──────────────────────────────────────────────────────────┐
│ LUA SCRIPT EXECUTION:                                    │
│                                                          │
│ Line 1: INCR "auth:login:ip:192.168.1.100"             │
│   → Increments 25 → 26                                  │
│                                                          │
│ Line 2: IF current == 1 THEN                            │
│   → FALSE, skip                                         │
│                                                          │
│ Line 5: IF 26 > 25 THEN                                 │
│   → TRUE! ✅                                            │
│                                                          │
│ Line 6:   RETURN PTTL key                               │
│   → Returns 34000 (34 seconds remaining)                │
│   → BLOCKED ❌                                          │
│                                                          │
│ (Line 8 never reached - script exits early)             │
└──────────────────────────────────────────────────────────┘
                    │
                    ▼
┌──────────────────────────────────────────────────────────┐
│ AFTER (Redis Memory):                                    │
│                                                          │
│   Key: "auth:login:ip:192.168.1.100"                   │
│   Value: "26"                                            │
│   TTL: 34 seconds remaining                             │
│                                                          │
│ (Counter still incremented - attacker can't bypass)     │
└──────────────────────────────────────────────────────────┘
```

---

## 🔐 Why Atomic Operations Matter

### ❌ Without Lua (Race Condition Problem)

```
Request A                    Request B
    │                            │
    ├─> INCR key (value: 1)      │
    │                            │
    │                            ├─> INCR key (value: 2)
    │                            │
    ├─> GET key → Returns 1      │
    │   (thinks limit OK)        │
    │                            │
    │                            ├─> GET key → Returns 2
    │                            │   (thinks limit OK)
    │                            │
    ├─> ALLOW request            │
    │                            │
    │                            ├─> ALLOW request
    │                            │
    ❌ BOTH ALLOWED!             ❌ (Should have blocked)
```

**Problem:** Two requests can check the limit at the same time and both pass!

---

### ✅ With Lua (Atomic - No Race Condition)

```
Request A                    Request B
    │                            │
    ├─> Execute Lua Script       │
    │   (INCR + CHECK + RETURN)  │
    │                            │
    │                            ├─> Execute Lua Script
    │                            │   (WAITS for Request A)
    │                            │
    ├─> Returns: ALLOWED ✅      │
    │                            │
    │                            ├─> Returns: ALLOWED or BLOCKED
    │                            │
    ✅ Only one can execute      ✅ Thread-safe!
      at a time (atomic)
```

**Solution:** Lua script executes completely before next one starts!

---

## 📊 Timeline: Redis State Changes

```
Time    Request    Redis Key Value    TTL     Result
─────────────────────────────────────────────────────
0s      #1         1                  60s     ✅ ALLOWED
5s      #2         2                  55s     ✅ ALLOWED
10s     #3         3                  50s     ✅ ALLOWED
15s     #4         4                  45s     ✅ ALLOWED
...
55s     #25        25                 5s      ✅ ALLOWED
56s     #26        26                 4s      ❌ BLOCKED (4s)
57s     #27        27                 3s      ❌ BLOCKED (3s)
60s     (expires)  (deleted)          -       (Window reset)
61s     #28        1                  60s     ✅ ALLOWED (new window)
```

---

## 🎯 Key Takeaways

### What Redis Does:
1. ✅ Stores counters in memory (fast access)
2. ✅ Executes Lua scripts atomically
3. ✅ Automatically expires keys (cleanup)
4. ✅ Handles concurrent requests safely

### What Lua Script Does:
1. ✅ Increments counter atomically
2. ✅ Sets expiration on first request
3. ✅ Checks limit and makes decision
4. ✅ Returns result (0 = allow, >0 = block)

### Why This Works:
1. ✅ **Atomic** - All operations happen together
2. ✅ **Fast** - Single network call
3. ✅ **Accurate** - No race conditions
4. ✅ **Automatic** - Keys auto-expire
5. ✅ **Scalable** - Handles many requests

**Redis + Lua = Perfect rate limiting solution!** 🚀

