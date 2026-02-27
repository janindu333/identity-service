# 🔄 Rate Limiting Flow - Quick Reference

## Visual Flow

```
┌─────────────────────────────────────────┐
│  USER SENDS LOGIN REQUEST               │
│  POST /auth/login                       │
│  { username, password }                 │
└──────────────┬──────────────────────────┘
               │
               ▼
┌─────────────────────────────────────────┐
│  EXTRACT IP: "192.168.1.100"            │
└──────────────┬──────────────────────────┘
               │
               ▼
┌─────────────────────────────────────────┐
│  CHECK IP RATE LIMIT                    │
│  Key: "auth:login:ip:192.168.1.100"    │
│  Limit: 25 / 60 seconds                 │
└──────────────┬──────────────────────────┘
               │
    ┌──────────┴──────────┐
    │                     │
    ▼                     ▼
┌──────────┐      ┌──────────────────┐
│ BLOCKED  │      │ CHECK USERNAME   │
│ HTTP 429 │      │ RATE LIMIT       │
│ Retry    │      │ Key: "auth:...   │
│ After    │      │ Limit: 7 / 5 min │
└──────────┘      └──────┬───────────┘
                         │
            ┌────────────┴────────────┐
            │                         │
            ▼                         ▼
    ┌──────────────┐      ┌──────────────────┐
    │ BLOCKED      │      │ AUTHENTICATE     │
    │ HTTP 429     │      │ Check credentials│
    └──────────────┘      └──────┬───────────┘
                                 │
                    ┌────────────┴────────────┐
                    │                         │
                    ▼                         ▼
        ┌──────────────────┐      ┌──────────────────┐
        │ INVALID CREDS    │      │ GENERATE TOKENS  │
        │ HTTP 401         │      │ HTTP 200         │
        │ (Counter +1)     │      │ Access + Refresh │
        └──────────────────┘      └──────────────────┘
```

## Redis Lua Script Execution

### Request #1 (First Attempt)
```
Redis Command: EVAL <lua_script> 1 "auth:login:ip:192.168.1.100" "60000" "25"

1. INCR "auth:login:ip:192.168.1.100"  → 1
2. IF 1 == 1 THEN SET EXPIRE 60s       → Yes, set expiration
3. IF 1 > 25 THEN return TTL          → No
4. RETURN 0                            → ALLOWED ✅
```

### Request #25 (Last Allowed)
```
Redis Command: EVAL <lua_script> 1 "auth:login:ip:192.168.1.100" "60000" "25"

1. INCR "auth:login:ip:192.168.1.100"  → 25
2. IF 25 == 1 THEN SET EXPIRE          → No (already set)
3. IF 25 > 25 THEN return TTL         → No
4. RETURN 0                            → ALLOWED ✅
```

### Request #26 (Blocked)
```
Redis Command: EVAL <lua_script> 1 "auth:login:ip:192.168.1.100" "60000" "25"

1. INCR "auth:login:ip:192.168.1.100"  → 26
2. IF 26 == 1 THEN SET EXPIRE          → No
3. IF 26 > 25 THEN return TTL         → Yes! Returns 35000ms
4. RETURN 35000                        → BLOCKED ❌ (35 seconds remaining)
```

## Timeline Example

```
00:00 - Login #1   → Redis: count=1,   TTL=60s  → ✅ ALLOWED
00:05 - Login #2   → Redis: count=2,   TTL=55s  → ✅ ALLOWED
00:10 - Login #3   → Redis: count=3,   TTL=50s  → ✅ ALLOWED
...
00:58 - Login #25  → Redis: count=25,  TTL=2s   → ✅ ALLOWED
00:59 - Login #26  → Redis: count=26,  TTL=1s   → ❌ BLOCKED (1s remaining)
01:00 - Key expires automatically
01:01 - Login #27  → Redis: count=1,   TTL=60s  → ✅ ALLOWED (new window)
```

## Key Points

1. ✅ **Lua Script = Atomic**: All operations happen in one step (no race conditions)
2. ✅ **Automatic Expiration**: Keys delete themselves after TTL
3. ✅ **Two-Level Protection**: IP limit (25/60s) + Username limit (7/5min)
4. ✅ **Counter Increments Before Auth**: Even failed logins count (security)
5. ✅ **Early Exit**: Blocks before database authentication (saves resources)

