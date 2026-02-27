# 🏁 Race Condition Example - Simple Explanation

## 🔴 The Problem in Simple Terms

Imagine you have a **shared counter** (like a number on a whiteboard), and **two people** want to check and update it at the **same time**.

---

## ❌ Without Lua Script: The Race Condition

### Scenario: Counter is at 24, Limit is 25

```
Two requests arrive at EXACTLY the same time:

┌──────────────────────────────────────────────────────────────┐
│           Person A (Request A)          Person B (Request B) │
└──────────────────────────────────────────────────────────────┘

Time 0: Both look at counter
        ┌──────────────────┐
        │  Counter = 24    │  ← Both see this
        └──────────────────┘

Time 1: Both think: "24 < 25? Yes, I can proceed!"
        
        Person A thinks: ✅ "I'm allowed!"
        Person B thinks: ✅ "I'm allowed!"

Time 2: Both increment the counter
        Person A: 24 → 25 ✅
        Person B: 24 → 26 ❌ (but they don't know!)

Time 3: Both proceed
        Person A: ✅ Allowed (correct)
        Person B: ✅ Allowed (WRONG! Should be blocked!)

Result: 25 requests became 27 requests! ❌
        Rate limit bypassed!
```

### Visual Timeline:

```
Shared Counter (Redis):
[Counter: 24]  [Counter: 24]  [Counter: 25]  [Counter: 26]
     │              │              │              │
     │              │              │              │
  Both read      Both check    Person A      Person B
   at same        at same      increments    increments
     time          time                      

❌ PROBLEM: Both saw 24, both passed check, both incremented!
```

---

## ✅ With Lua Script: No Race Condition

### Same Scenario: Counter is at 24, Limit is 25

```
Two requests arrive, but Lua script ensures they execute ONE AT A TIME:

┌──────────────────────────────────────────────────────────────┐
│           Person A (Request A)          Person B (Request B) │
└──────────────────────────────────────────────────────────────┘

Time 0: Person A's script executes FIRST
        ┌────────────────────────────────────────────┐
        │ Lua Script (Atomic - All steps together): │
        │  1. Read counter: 24                       │
        │  2. Check: 24 < 25? Yes                    │
        │  3. Increment: 24 → 25                     │
        │  4. Check again: 25 <= 25? Yes             │
        │  5. Return: ALLOWED ✅                      │
        └────────────────────────────────────────────┘
        
        Counter is now: 25
        Person A: ✅ ALLOWED (correct)

Time 1: Person B's script executes NEXT (waits for A to finish)
        ┌────────────────────────────────────────────┐
        │ Lua Script (Atomic - All steps together): │
        │  1. Read counter: 25                       │
        │  2. Check: 25 < 25? NO!                    │
        │  3. Increment: 25 → 26                     │
        │  4. Check: 26 > 25? YES!                   │
        │  5. Return: BLOCKED ❌                      │
        └────────────────────────────────────────────┘
        
        Counter is now: 26
        Person B: ❌ BLOCKED (correct!)

Result: Only 25 requests allowed, 26th blocked! ✅
        Rate limit works correctly!
```

### Visual Timeline:

```
Shared Counter (Redis):
[Counter: 24]  [Counter: 25]  [Counter: 26]
     │              │              │
     │              │              │
  Person A      Person A      Person B
  executes      finishes      executes
  script        (allowed)     script
  (atomic)                    (atomic)
                              (blocked)

✅ SOLUTION: Scripts execute one at a time, no overlap!
```

---

## 🎬 Real Example: Two Requests at Same Millisecond

### WITHOUT Lua (What Actually Happens):

```java
// Request A Thread
Thread 1:
  Step 1: GET key → Returns "24"
  Step 2: Check if 24 < 25 → YES ✅
  Step 3: Sleep for 1ms (network delay)
  Step 4: INCR key → Counter becomes 25
  Step 5: Return ALLOWED ✅

// Request B Thread (executing at same time)
Thread 2:
  Step 1: GET key → Returns "24" (same value!)
  Step 2: Check if 24 < 25 → YES ✅
  Step 3: Sleep for 1ms (network delay)
  Step 4: INCR key → Counter becomes 26
  Step 5: Return ALLOWED ✅

Result: BOTH allowed! Counter went 24 → 26!
```

**Timeline Visualization:**

```
Time     Thread 1              Redis              Thread 2
──────────────────────────────────────────────────────────────
0ms      GET key ────────────> │
         │                     │ (value = "24")
         │ <───────────────────│
         │                     │
1ms      Check: 24 < 25 ✅     │
         │                     │
         │                     │<─────────────── GET key
         │                     │ (value = "24")
         │                     │───────────────>
         │                     │
2ms      (processing...)       │                Check: 24 < 25 ✅
         │                     │
         │                     │                (processing...)
         │                     │
3ms      INCR key ───────────> │
         │                     │ (value = "25")
         │ <───────────────────│
         │                     │
         │                     │<─────────────── INCR key
         │                     │ (value = "26")
         │                     │───────────────>
         │                     │
4ms      Return ALLOWED ✅     │                Return ALLOWED ✅

❌ BOTH THREADS ALLOWED! WRONG!
```

---

### WITH Lua (What Actually Happens):

```java
// Request A Thread
Thread 1:
  Execute Lua Script (ATOMIC):
    - INCR key → 25
    - Check: 25 <= 25? YES
    - Return: 0 (ALLOWED)
  Return ALLOWED ✅

// Request B Thread (waits for Thread 1 to finish)
Thread 2:
  Execute Lua Script (ATOMIC):
    - INCR key → 26
    - Check: 26 <= 25? NO
    - Return: 34000 (BLOCKED)
  Return BLOCKED ❌

Result: Only Thread 1 allowed! Counter: 24 → 25 → 26 (blocked)
```

**Timeline Visualization:**

```
Time     Thread 1              Redis (Lua Queue)      Thread 2
──────────────────────────────────────────────────────────────────────
0ms      Execute Script ─────> │                      │
         │                     │ [Script A executing] │ (waiting)
         │                     │  INCR → 25           │
         │                     │  Check → OK          │
         │                     │  Return 0            │
         │ <───────────────────│                      │
2ms      Result: ALLOWED ✅    │                      │
         │                     │                      │
         │                     │<───────────────────── Execute Script
         │                     │ [Script B executing] │
         │                     │  INCR → 26           │
         │                     │  Check → EXCEEDED!   │
         │                     │  Return 34000        │
         │                     │─────────────────────>│
4ms      │                     │                      Result: BLOCKED ❌

✅ CORRECT: Only Thread 1 allowed!
```

---

## 🔑 Key Difference

### Without Lua:
```
Two requests can READ and WRITE at the same time
→ They see the same value
→ Both pass the check
→ Both increment
→ BUG! ❌
```

### With Lua:
```
Two requests execute scripts ONE AT A TIME
→ Request A finishes completely
→ Then Request B starts
→ Request B sees updated value
→ Request B correctly gets blocked
→ CORRECT! ✅
```

---

## 🎯 Simple Analogy

### Without Lua = Two People Trying to Enter Through One Door

```
Person A and Person B both run to the door at the same time.

❌ WITHOUT LUA:
   Person A: "Is there space? Yes!" (sees 24 people inside)
   Person B: "Is there space? Yes!" (sees 24 people inside)
   Both enter → Now 26 people inside! (limit was 25)

✅ WITH LUA:
   Person A: Enters door (now 25 people inside)
   Person B: Tries to enter (sees 25 people)
   Person B: "Room is full!" (correctly blocked)
```

---

## 📊 Summary Table

| Aspect | Without Lua | With Lua |
|--------|-------------|----------|
| **Execution** | Multiple separate commands | Single atomic script |
| **Race Conditions** | ❌ Yes, possible | ✅ No, impossible |
| **Speed** | Slow (4-8ms) | Fast (1-2ms) |
| **Correctness** | ❌ Can fail | ✅ Always correct |
| **Security** | ❌ Vulnerable | ✅ Secure |
| **Network Calls** | 4+ calls | 1 call |
| **Concurrency** | ❌ Problems | ✅ Safe |

---

## 🎓 Bottom Line

**Without Lua Scripts:**
- ❌ Multiple requests can see the same counter value
- ❌ They all pass the check
- ❌ They all increment
- ❌ Rate limit gets bypassed
- ❌ **BUG!**

**With Lua Scripts:**
- ✅ Requests execute one at a time
- ✅ Each request sees the updated value
- ✅ Rate limit works correctly
- ✅ **FIXED!**

**Lua scripts = Solution to race condition problems!** 🎯

