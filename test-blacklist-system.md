# Token Blacklist System Test Guide

## 🧪 Testing the Hybrid Redis + MySQL Blacklist System

### **Prerequisites**
- Identity Service running
- Redis running (Docker or local)
- MySQL running with `saloon-service` database

### **1. Health Check**
```bash
GET http://localhost:8080/auth/admin/tokens/health
```
**Expected Response:**
```json
{
  "success": true,
  "status": "HEALTHY",
  "redisCount": 0,
  "databaseCount": 0,
  "totalCount": 0,
  "message": "Blacklist system is healthy",
  "timestamp": "2025-08-04T15:31:44"
}
```

### **2. Blacklist a Token**
```bash
POST http://localhost:8080/auth/admin/tokens/blacklist
Content-Type: application/json

{
  "token": "eyJhbGciOiJIUzI1NiJ9.eyJyb2xlIjoiVVNFUiIsInRva2VuVHlwZSI6IkFDQ0VTUyIsInN1YiI6ImphbmluZHUiLCJpYXQi0jE3NTQwNDgzMzYsImV4cCI6MTc1NDA1NTUzNn0.nSnBu6hiEwHb0cWWsRzhe3h7jiW6SJ9wf4_NW06tU7A",
  "username": "janindu",
  "reason": "Testing blacklist functionality",
  "expirationHours": 2
}
```
**Expected Response:**
```json
{
  "success": true,
  "message": "Token blacklisted successfully",
  "blacklistCount": 1
}
```

### **3. Check Token Status**
```bash
GET http://localhost:8080/auth/admin/tokens/check?token=eyJhbGciOiJIUzI1NiJ9.eyJyb2xlIjoiVVNFUiIsInRva2VuVHlwZSI6IkFDQ0VTUyIsInN1YiI6ImphbmluZHUiLCJpYXQi0jE3NTQwNDgzMzYsImV4cCI6MTc1NDA1NTUzNn0.nSnBu6hiEwHb0cWWsRzhe3h7jiW6SJ9wf4_NW06tU7A
```
**Expected Response:**
```json
{
  "success": true,
  "isBlacklisted": true,
  "blacklistCount": 1
}
```

### **4. Get Statistics**
```bash
GET http://localhost:8080/auth/admin/tokens/stats
```
**Expected Response:**
```json
{
  "success": true,
  "redisCount": 1,
  "databaseCount": 1,
  "totalCount": 1,
  "message": "Blacklist statistics retrieved"
}
```

### **5. Test Redis Restart Recovery**

#### **Step 1: Restart Redis**
```bash
# Stop Redis
docker stop redis-container

# Start Redis
docker start redis-container
```

#### **Step 2: Check Health (should show Redis=0, Database=1)**
```bash
GET http://localhost:8080/auth/admin/tokens/health
```
**Expected Response:**
```json
{
  "success": true,
  "status": "HEALTHY",
  "redisCount": 0,
  "databaseCount": 1,
  "totalCount": 1,
  "message": "Blacklist system is healthy"
}
```

#### **Step 3: Check Token (should still be blacklisted via database)**
```bash
GET http://localhost:8080/auth/admin/tokens/check?token=eyJhbGciOiJIUzI1NiJ9.eyJyb2xlIjoiVVNFUiIsInRva2VuVHlwZSI6IkFDQ0VTUyIsInN1YiI6ImphbmluZHUiLCJpYXQi0jE3NTQwNDgzMzYsImV4cCI6MTc1NDA1NTUzNn0.nSnBu6hiEwHb0cWWsRzhe3h7jiW6SJ9wf4_NW06tU7A
```
**Expected Response:**
```json
{
  "success": true,
  "isBlacklisted": true,
  "blacklistCount": 1
}
```

#### **Step 4: Rebuild Cache**
```bash
POST http://localhost:8080/auth/admin/tokens/rebuild-cache
```
**Expected Response:**
```json
{
  "success": true,
  "message": "Redis cache rebuilt from database successfully"
}
```

#### **Step 5: Verify Redis is Restored**
```bash
GET http://localhost:8080/auth/admin/tokens/stats
```
**Expected Response:**
```json
{
  "success": true,
  "redisCount": 1,
  "databaseCount": 1,
  "totalCount": 1,
  "message": "Blacklist statistics retrieved"
}
```

### **6. Remove Token from Blacklist**
```bash
DELETE http://localhost:8080/auth/admin/tokens/blacklist
Content-Type: application/json

{
  "token": "eyJhbGciOiJIUzI1NiJ9.eyJyb2xlIjoiVVNFUiIsInRva2VuVHlwZSI6IkFDQ0VTUyIsInN1YiI6ImphbmluZHUiLCJpYXQi0jE3NTQwNDgzMzYsImV4cCI6MTc1NDA1NTUzNn0.nSnBu6hiEwHb0cWWsRzhe3h7jiW6SJ9wf4_NW06tU7A"
}
```
**Expected Response:**
```json
{
  "success": true,
  "message": "Token removed from blacklist",
  "blacklistCount": 0
}
```

## **🔍 Database Verification**

### **Check MySQL Table**
```sql
USE saloon-service;
SELECT * FROM blacklisted_tokens;
```

### **Check Redis Keys**
```bash
redis-cli
> KEYS blacklist:*
```

## **✅ Success Criteria**

1. **Database Initialization**: Table created automatically on startup
2. **Token Blacklisting**: Stored in both Redis and MySQL
3. **Redis Restart**: System continues working with database fallback
4. **Cache Rebuild**: Redis restored from MySQL after restart
5. **Token Removal**: Removed from both Redis and MySQL
6. **Health Monitoring**: All endpoints return correct status

## **🚨 Troubleshooting**

### **If Database Initialization Fails:**
- Check MySQL connection in `application.properties`
- Verify database exists: `saloon-service`
- Check MySQL user permissions

### **If Redis Connection Fails:**
- Verify Redis is running: `docker ps | grep redis`
- Check Redis port: `6379`
- Verify Redis host: `localhost`

### **If Token Blacklisting Fails:**
- Check logs for specific error messages
- Verify JPA entity mapping
- Check database table structure 