# JWT Authentication Setup for Identity Service

## Overview

The identity-service now implements JWT-based authentication for secured endpoints using industry best practices. This setup allows the service to handle its own authentication while maintaining compatibility with the API Gateway.

## Architecture

### Components

1. **JwtAuthenticationFilter**: Validates JWT tokens and sets up authentication context
2. **CustomUserDetailsService**: Loads user details from the database
3. **JwtAuthenticationEntryPoint**: Handles unauthorized access with proper error responses
4. **SecurityConfig**: Configures Spring Security with JWT authentication

### Flow

1. Client sends request with JWT token in Authorization header
2. JwtAuthenticationFilter intercepts the request
3. Filter validates the JWT token using JwtService
4. If valid, user details are loaded and authentication context is set
5. Spring Security checks if the endpoint requires authentication
6. If authenticated, request proceeds to the controller

## Secured Endpoints

Currently, the following endpoint requires JWT authentication:
- `PATCH /auth/location` - Update user location

## Open Endpoints

The following endpoints are publicly accessible:
- `POST /auth/register` - User registration
- `POST /auth/login` - User login
- `GET /auth/validate` - Token validation
- `GET /auth/getToken` - Get access token from refresh token
- `POST /auth/reset-password` - Password reset
- `GET /roles/**` - Role management
- `GET /permissions/**` - Permission management
- Swagger documentation endpoints

## Usage

### Making Authenticated Requests

```bash
# Example: Update user location
curl -X PATCH http://localhost:8080/auth/location \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "latitude": 40.7128,
    "longitude": -74.0060
  }'
```

### Error Responses

When authentication fails, the service returns a structured error response:

```json
{
  "status": 401,
  "error": "Unauthorized",
  "message": "Full authentication is required to access this resource",
  "path": "/auth/location"
}
```

## Configuration

### JWT Secret

Ensure the JWT secret is configured in `application.properties`:

```properties
jwt.secret=your-base64-encoded-secret-key
```

### Security Configuration

The security configuration is defined in `SecurityConfig.java` and includes:
- Stateless session management
- JWT filter integration
- Custom authentication entry point
- Endpoint-specific authorization rules

## Best Practices Implemented

1. **Stateless Authentication**: No server-side session storage
2. **Proper Error Handling**: Structured error responses for authentication failures
3. **Role-Based Access Control**: JWT tokens include user roles
4. **Secure Token Validation**: Comprehensive JWT validation
5. **Clean Separation of Concerns**: Authentication logic separated from business logic
6. **Comprehensive Logging**: Detailed logging for debugging and monitoring

## Integration with API Gateway

The identity-service handles its own authentication, so the API Gateway's AuthenticationFilter is not applied to identity-service routes. This provides:

1. **Service Autonomy**: Identity service can validate its own tokens
2. **Reduced Gateway Load**: Authentication processing is distributed
3. **Better Error Handling**: Service-specific authentication error messages
4. **Flexibility**: Service can implement custom authentication logic

## Testing

To test the authentication:

1. Register a user: `POST /auth/register`
2. Login to get JWT token: `POST /auth/login`
3. Use the token to access secured endpoint: `PATCH /auth/location`

## Security Considerations

1. **Token Expiration**: JWT tokens have configurable expiration times
2. **Secret Management**: JWT secret should be stored securely
3. **HTTPS**: Always use HTTPS in production
4. **Token Storage**: Store tokens securely on the client side
5. **Logout**: Implement token blacklisting if needed 