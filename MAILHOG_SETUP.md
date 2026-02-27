# MailHog Setup Guide for Password Reset Testing

This guide explains how to set up and use MailHog for testing the forgot password functionality.

## What is MailHog?

MailHog is an email testing tool that captures emails sent by your application. It provides:
- SMTP server (for sending emails) - Port 1025
- Web UI (for viewing emails) - Port 8085
- No authentication required (perfect for local testing)

## Setup Steps

### 1. Install MailHog

**Windows:**
```powershell
# Using Chocolatey
choco install mailhog

# Or download from: https://github.com/mailhog/MailHog/releases
# Extract and run MailHog.exe
```

**macOS:**
```bash
brew install mailhog
```

**Linux:**
```bash
# Download binary
wget https://github.com/mailhog/MailHog/releases/download/v1.0.1/MailHog_linux_amd64
chmod +x MailHog_linux_amd64
sudo mv MailHog_linux_amd64 /usr/local/bin/mailhog
```

**Docker (Recommended):**
```bash
docker run -d -p 1025:1025 -p 8085:8025 mailhog/mailhog
```

### 2. Start MailHog

**Windows (Direct):**
```powershell
MailHog.exe
```

**Docker:**
```bash
docker run -d -p 1025:1025 -p 8025:8025 --name mailhog mailhog/mailhog
```

MailHog will start on:
- **SMTP Server**: `localhost:1025`
- **Web UI**: `http://localhost:8085`

### 3. Configuration

The application is already configured in `application.properties`:

```properties
# MailHog SMTP Configuration
spring.mail.host=localhost
spring.mail.port=1025
spring.mail.properties.mail.smtp.auth=false
spring.mail.properties.mail.smtp.starttls.enable=false

# Email service configuration
app.email.enabled=true
app.email.from=noreply@baberbooking.com
app.email.use-smtp=true
```

### 4. Test the Forgot Password Flow

1. **Start MailHog** (if not already running)
2. **Start your application**
3. **Request password reset:**
   ```bash
   POST http://localhost:8080/auth/forgot-password
   Content-Type: application/json
   
   {
     "email": "test@example.com"
   }
   ```

4. **View the email in MailHog:**
   - Open `http://localhost:8085` in your browser
   - You should see the password reset email
   - Click on it to view the full HTML content
   - Copy the reset link from the email

5. **Test password reset:**
   ```bash
   POST http://localhost:8080/auth/reset-password/token?token={token_from_email}&newPassword=NewPassword123!
   ```

## Features

### Email Templates

The application sends two types of emails:

1. **Password Reset Email**
   - HTML formatted email
   - Contains reset link with token
   - Shows expiration time
   - Includes IP address for security

2. **Password Reset Confirmation Email**
   - Sent after successful password reset
   - Confirmation message
   - Security warning if user didn't make the change

### Fallback Mode

If MailHog is not running or SMTP fails, the application will:
- Log email details to console
- Continue functioning normally
- Not throw errors (graceful degradation)

## Configuration Options

### Disable Email Sending
```properties
app.email.enabled=false
```

### Disable SMTP (Log Only Mode)
```properties
app.email.use-smtp=false
```

### Use Different SMTP Server

If you want to use a different SMTP server (e.g., Gmail, SendGrid):

```properties
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your-email@gmail.com
spring.mail.password=your-app-password
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
```

## Troubleshooting

### Emails Not Appearing in MailHog

1. **Check if MailHog is running:**
   ```bash
   # Check if port 8085 is accessible
   curl http://localhost:8085
   ```

2. **Check application logs:**
   - Look for "Email sent successfully via SMTP" message
   - Check for any SMTP connection errors

3. **Verify configuration:**
   - Ensure `spring.mail.host=localhost`
   - Ensure `spring.mail.port=1025`
   - Ensure `app.email.use-smtp=true`

### SMTP Connection Errors

If you see connection errors:
1. Make sure MailHog is running
2. Check firewall settings
3. Verify port 1025 is not blocked

### Using Docker Compose

Add MailHog to your `docker-compose.yml`:

```yaml
services:
  mailhog:
    image: mailhog/mailhog
    ports:
      - "1025:1025"  # SMTP
      - "8085:8025"  # Web UI
    networks:
      - app-network
```

## Benefits of MailHog

✅ **No authentication required** - Simple local testing  
✅ **View emails instantly** - Web UI shows all captured emails  
✅ **HTML rendering** - See emails exactly as users will see them  
✅ **No external dependencies** - Works offline  
✅ **Free and open source** - No cost  
✅ **Perfect for development** - Prevents accidental email sending  

## Next Steps

Once testing is complete, you can:
1. Switch to production SMTP (SendGrid, AWS SES, etc.)
2. Keep MailHog for local development
3. Integrate with notification-service for microservices architecture
4. Add email queue with Kafka/RabbitMQ for better reliability

