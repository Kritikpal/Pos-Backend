# Password Reset API - Changes Documentation

## Overview
Added two new public APIs for password reset functionality with secure JWT-based token validation. The implementation includes:
- Password reset request table to maintain reset request state
- JWT utility integration for token generation and validation (24-hour expiry)
- Proper status management (PENDING, VERIFIED, EXPIRED, USED)
- Email notification system

---

## New Entities & Tables

### PasswordResetRequest Entity
**Table**: `tbl_password_reset_request`

**Fields**:
- `id` (Long): Primary key
- `userId` (Long): Reference to the user requesting password reset (NOT NULL)
- `email` (String): User's email address (NOT NULL)
- `token` (String): JWT token for password reset (NOT NULL, UNIQUE)
- `status` (Enum): Current state - PENDING | VERIFIED | EXPIRED | USED (NOT NULL)
- `createdAt` (LocalDateTime): When the request was created (NOT NULL)
- `expiryDate` (LocalDateTime): When the token expires (NOT NULL)
- `verifiedAt` (LocalDateTime): When the token was verified (nullable)
- `usedAt` (LocalDateTime): When password was reset using this token (nullable)

**Indexes**:
- email
- token
- status

### PasswordResetStatus Enum
Status values for password reset requests:
- **PENDING**: Reset request created, email sent, awaiting confirmation
- **VERIFIED**: Token verified, password successfully changed
- **EXPIRED**: Token expired without use
- **USED**: Token already used for password reset

---

## API Endpoints

### 1. Send Password Reset Email

**Endpoint**: `POST /auth/password-reset/request`

**Access**: Public (No authentication required)

**Request**:
```json
{
  "email": "user@example.com"
}
```

**Request Parameters**:
| Field | Type | Required | Validation |
|-------|------|----------|-----------|
| email | String | Yes | Valid email format |

**Response (Success - 200 OK)**:
```json
{
  "data": {
    "message": "Password reset email has been sent",
    "status": "SUCCESS"
  },
  "responseCode": "SUCCESS",
  "message": "Email sent successfully"
}
```

**Response (User Not Found - 400 Bad Request)**:
```json
{
  "data": null,
  "errorMessage": "User with this email not found",
  "responseCode": "ERROR",
  "message": "User with this email not found"
}
```

**Logic Flow**:
1. Validates that user exists with given email
2. Invalidates any existing PENDING password reset requests for this user
3. Generates JWT token with 24-hour expiry containing user email and ID
4. Creates PasswordResetRequest record with PENDING status
5. Sends reset link to user's email

**Token Claims**:
```json
{
  "email": "user@example.com",
  "userId": 123,
  "type": "PASSWORD_RESET",
  "iat": 1234567890,
  "exp": 1234654290
}
```

**Email Content**:
```
Hello,

We received a request to reset your POS account password.

Click the link below to reset your password:
http://localhost:8080/reset-password?token=[JWT_TOKEN]

This link will expire in 24 hours.

If you did not request this, please ignore this email.

Thanks.
```

---

### 2. Change Password Using Reset Token

**Endpoint**: `POST /auth/password-reset/confirm`

**Access**: Public (No authentication required)

**Request**:
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "newPassword": "newSecurePassword123"
}
```

**Request Parameters**:
| Field | Type | Required | Validation |
|-------|------|----------|-----------|
| token | String | Yes | Valid JWT reset token |
| newPassword | String | Yes | Min 6 characters |

**Response (Success - 200 OK)**:
```json
{
  "data": {
    "message": "Password has been reset successfully",
    "status": "SUCCESS"
  },
  "responseCode": "SUCCESS",
  "message": "Password reset successful"
}
```

**Response (Invalid Token - 400 Bad Request)**:
```json
{
  "data": null,
  "errorMessage": "Invalid or already used reset token",
  "responseCode": "ERROR",
  "message": "Invalid or already used reset token"
}
```

**Response (Expired Token - 400 Bad Request)**:
```json
{
  "data": null,
  "errorMessage": "Reset token has expired",
  "responseCode": "ERROR",
  "message": "Reset token has expired"
}
```

**Logic Flow**:
1. Validates JWT token format and extracts claims
2. Finds matching PasswordResetRequest with PENDING status
3. Checks if token has expired
4. Updates user's password (encoded with PasswordEncoder)
5. Marks reset request as VERIFIED and sets usedAt timestamp
6. Returns success response

---

## Implementation Details

### JWT Token Generation
- **Expiry**: 24 hours (86400 seconds)
- **Algorithm**: HS256 (HMAC with SHA-256)
- **Issuer**: pos-backend
- **Claims**:
  - `email`: User's email address
  - `userId`: User ID
  - `type`: "PASSWORD_RESET"
  - `iat`: Issued at timestamp
  - `exp`: Expiration timestamp

### Security Features
1. **Token Validation**: All tokens validated using JwtUtil for signature and expiry
2. **Single Use**: Reset requests marked as VERIFIED after use to prevent reuse
3. **Time Bound**: Tokens expire after 24 hours
4. **Pending Invalidation**: Previous PENDING reset requests automatically invalidated when new request made
5. **Password Encoding**: New passwords encoded using Spring's PasswordEncoder

### Database Constraints
- `token` column is UNIQUE to prevent duplicate tokens
- Indexed on `email`, `token`, and `status` for efficient lookups
- Timestamps tracked for audit trail

---

## Files Modified/Created

### New Files
1. `src/main/java/com/kritik/POS/user/entity/PasswordResetRequest.java` - Entity class
2. `src/main/java/com/kritik/POS/user/entity/PasswordResetStatus.java` - Status enum
3. `src/main/java/com/kritik/POS/user/repository/PasswordResetRequestRepository.java` - Repository interface
4. `src/main/java/com/kritik/POS/user/model/request/SendPasswordResetRequest.java` - DTO
5. `src/main/java/com/kritik/POS/user/model/request/ChangePasswordFromTokenRequest.java` - DTO
6. `src/main/java/com/kritik/POS/user/model/response/PasswordResetResponse.java` - Response DTO

### Modified Files
1. `src/main/java/com/kritik/POS/user/service/MailService.java` - Added `sendPasswordResetEmail()` method
2. `src/main/java/com/kritik/POS/user/service/impl/MailServiceImpl.java` - Implemented password reset email sending
3. `src/main/java/com/kritik/POS/user/service/UserService.java` - Added two new method signatures
4. `src/main/java/com/kritik/POS/user/service/impl/UserServiceImpl.java` - Implemented password reset logic
5. `src/main/java/com/kritik/POS/user/route/UserRoute.java` - Added new route constants
6. `src/main/java/com/kritik/POS/user/controller/PreAuthController.java` - Added two new endpoints

---

## Configuration

### application.yml
Add the following properties (optional, with defaults):
```yaml
app:
  login-url: http://localhost:8080/login
  password-reset-url: http://localhost:8080/reset-password
```

---

## Testing Scenarios

### Scenario 1: Successful Password Reset
1. POST `/auth/password-reset/request` with valid email → Success
2. Receive email with reset token
3. POST `/auth/password-reset/confirm` with token and new password → Success
4. Login with new password → Success

### Scenario 2: Expired Token
1. Generate reset token and wait 24+ hours
2. POST `/auth/password-reset/confirm` with expired token → 400 Bad Request
3. Reset request status changed to EXPIRED

### Scenario 3: Token Reuse Prevention
1. Generate reset token
2. POST `/auth/password-reset/confirm` with token → Success
3. POST `/auth/password-reset/confirm` with same token again → 400 Bad Request (marked as VERIFIED)

### Scenario 4: Multiple Reset Requests
1. POST `/auth/password-reset/request` - Creates request 1 (PENDING)
2. POST `/auth/password-reset/request` again - Creates request 2 (PENDING), request 1 marked EXPIRED
3. Only request 2 token is valid

---

## Error Handling

| Scenario | Status | Error Message |
|----------|--------|---------------|
| User email not found | 400 | User with this email not found |
| Invalid/malformed token | 400 | Invalid or expired reset token |
| Token already used | 400 | Invalid or already used reset token |
| Token expired | 400 | Reset token has expired |
| Invalid JWT claims | 400 | Invalid reset token |
| Password too short | 400 | Password must be at least 6 characters |

---

## Future Enhancements
- Add rate limiting on password reset requests (max 3 per hour per email)
- Add CAPTCHA verification to prevent abuse
- Add admin audit logging for password resets
- Implement password reset via SMS
- Add password reset analytics dashboard
- Support for multiple reset tokens per user
