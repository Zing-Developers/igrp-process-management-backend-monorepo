# IRN System Integration - Customization Guide

This document describes the configuration and customization options for integrating the IGRP Process Management Backend with the IRN (Sistema de Administração da República) system.

## Overview

The IRN integration consists of two modules:

- **`process-runtime-auth-irn`**: Handles IRN authentication and authorization with caching support
- **`process-runtime-irn-integration`**: Provides JWT token signing and signed RestClient for secure API communication

## Modules

### 1. process-runtime-auth-irn

This module provides authentication and authorization through the IRN API. It validates user sessions by calling the IRN `/api/v1/Auth/me` endpoint.

**Maven Dependency:**
```xml
<dependency>
    <groupId>cv.igrp.framework</groupId>
    <artifactId>process-runtime-auth-irn</artifactId>
    <version>0.1.0-beta.23</version>
</dependency>
```

### 2. process-runtime-irn-integration

This module provides JWT token generation and signing for secure communication with IRN APIs. It uses RS256 algorithm with private key signing.

**Maven Dependency:**
```xml
<dependency>
    <groupId>cv.igrp.framework</groupId>
    <artifactId>process-runtime-irn-integration</artifactId>
    <version>0.1.0-beta.23</version>
</dependency>
```

## Configuration Properties

### IRN API Configuration (process-runtime-auth-irn)

These properties configure the IRN API connection for authentication.

| Property | Description | Required | Default | Environment Variable |
|----------|-------------|----------|---------|---------------------|
| `igrp.authorization.service.adapter` | Authorization service adapter type | ✅ Yes | - | `IGRP_AUTHORIZATION_SERVICE_ADAPTER` |
| `irn.api.base-url` | Base URL of the IRN API | ✅ Yes | - | `IRN_API_BASE_URL` |
| `irn.api.super-admin-email` | Email of the super admin user | ✅ Yes | - | `IRN_API_SUPER_ADMIN_EMAIL` |
| `irn.api.session-cookie-name` | Name of the session cookie | ❌ No | `session_id` | `IRN_API_SESSION_COOKIE_NAME` |

> **Important:** Set `igrp.authorization.service.adapter=irn` to enable the IRN authorization adapter. This is required for the IRN authentication module to be activated.

**Example application.yml:**
```yaml
igrp:
  authorization:
    service:
      adapter: irn

irn:
  api:
    base-url: https://api.irn.gov.cv
    super-admin-email: admin@irn.gov.cv
    session-cookie-name: session_id
```

**Example application.properties:**
```properties
igrp.authorization.service.adapter=irn
irn.api.base-url=https://api.irn.gov.cv
irn.api.super-admin-email=admin@irn.gov.cv
irn.api.session-cookie-name=session_id
```

**Environment Variables:**
```bash
export IGRP_AUTHORIZATION_SERVICE_ADAPTER=irn
export IRN_API_BASE_URL=https://api.irn.gov.cv
export IRN_API_SUPER_ADMIN_EMAIL=admin@irn.gov.cv
export IRN_API_SESSION_COOKIE_NAME=session_id
```

### JWT Token Configuration (process-runtime-irn-integration)

These properties configure JWT token generation and signing for secure API communication.

| Property | Description | Required | Default | Environment Variable |
|----------|-------------|----------|---------|---------------------|
| `igrp.restclient.provider` | RestClient provider type | ✅ Yes | - | `IGRP_RESTCLIENT_PROVIDER` |
| `igrp.authorization.jwt.key` | JWT payload key identifier | ✅ Yes | `default` | `IGRP_AUTHORIZATION_JWT_KEY` |
| `igrp.authorization.jwt.private-key` | Path to RSA private key (PKCS#1) | ✅ Yes | `default` | `IGRP_AUTHORIZATION_JWT_PRIVATE_KEY` |

**Example application.yml:**
```yaml
igrp:
  restclient:
    provider: irn
  authorization:
    jwt:
      key: my-api-key
      private-key: classpath:keys/irn-private-key.pem
```

**Example application.properties:**
```properties
igrp.restclient.provider=irn
igrp.authorization.jwt.key=my-api-key
igrp.authorization.jwt.private-key=classpath:keys/irn-private-key.pem
```

**Environment Variables:**
```bash
export IGRP_RESTCLIENT_PROVIDER=irn
export IGRP_AUTHORIZATION_JWT_KEY=my-api-key
export IGRP_AUTHORIZATION_JWT_PRIVATE_KEY=file:/path/to/irn-private-key.pem
```

## Complete Configuration Examples

### Docker Environment

**docker-compose.yml:**
```yaml
version: '3.8'

services:
  igrp-process-backend:
    image: igrp-process-management-backend:0.1.0-beta.20
    environment:
      # Authorization Service Configuration
      - IGRP_AUTHORIZATION_SERVICE_ADAPTER=irn

      # IRN API Configuration
      - IRN_API_BASE_URL=https://api.irn.gov.cv
      - IRN_API_SUPER_ADMIN_EMAIL=admin@irn.gov.cv
      - IRN_API_SESSION_COOKIE_NAME=session_id

      # JWT Token Configuration
      - IGRP_RESTCLIENT_PROVIDER=irn
      - IGRP_AUTHORIZATION_JWT_KEY=${JWT_KEY}
      - IGRP_AUTHORIZATION_JWT_PRIVATE_KEY=file:/app/config/irn-private-key.pem
    volumes:
      - ./config/irn-private-key.pem:/app/config/irn-private-key.pem:ro
    ports:
      - "8080:8080"
```

### Kubernetes ConfigMap & Secret

**configmap.yaml:**
```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: igrp-irn-config
data:
  IGRP_AUTHORIZATION_SERVICE_ADAPTER: "irn"
  IRN_API_BASE_URL: "https://api.irn.gov.cv"
  IRN_API_SUPER_ADMIN_EMAIL: "admin@irn.gov.cv"
  IRN_API_SESSION_COOKIE_NAME: "session_id"
  IGRP_RESTCLIENT_PROVIDER: "irn"
```

**secret.yaml:**
```yaml
apiVersion: v1
kind: Secret
metadata:
  name: igrp-irn-secrets
type: Opaque
stringData:
  IGRP_AUTHORIZATION_JWT_KEY: "your-jwt-key"
  irn-private-key.pem: |
    -----BEGIN RSA PRIVATE KEY-----
    MIIEpAIBAAKCAQEA...
    -----END RSA PRIVATE KEY-----
```

**deployment.yaml:**
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: igrp-process-backend
spec:
  template:
    spec:
      containers:
      - name: backend
        image: igrp-process-management-backend:0.1.0-beta.23
        envFrom:
        - configMapRef:
            name: igrp-irn-config
        - secretRef:
            name: igrp-irn-secrets
        env:
        - name: IGRP_AUTHORIZATION_JWT_PRIVATE_KEY
          value: "file:/app/secrets/irn-private-key.pem"
        volumeMounts:
        - name: private-key
          mountPath: /app/secrets
          readOnly: true
      volumes:
      - name: private-key
        secret:
          secretName: igrp-irn-secrets
          items:
          - key: irn-private-key.pem
            path: irn-private-key.pem
```

### Traditional Deployment (.env file)

**.env:**
```bash
# Authorization Service Configuration
IGRP_AUTHORIZATION_SERVICE_ADAPTER=irn

# IRN API Configuration
IRN_API_BASE_URL=https://api.irn.gov.cv
IRN_API_SUPER_ADMIN_EMAIL=admin@irn.gov.cv
IRN_API_SESSION_COOKIE_NAME=session_id

# JWT Token Configuration
IGRP_RESTCLIENT_PROVIDER=irn
IGRP_AUTHORIZATION_JWT_KEY=my-api-key
IGRP_AUTHORIZATION_JWT_PRIVATE_KEY=file:/etc/igrp/keys/irn-private-key.pem
```

**Start application:**
```bash
# Load environment variables
source .env

# Or use with java command
java -jar igrp-process-management-backend.jar
```

## Features

### 1. JWT Token Caching with JwtTokenService

The `JwtTokenService` automatically caches JWT tokens using Spring Cache abstraction to optimize performance:

- **Cache Strategy**: Time-window based caching (55-minute intervals)
- **Token Expiration**: Tokens are valid for 1 hour
- **Auto-Refresh**: New tokens are automatically generated 5 minutes before expiration
- **Thread-Safe**: Uses Spring's caching mechanism for concurrent access

**How it works:**
1. Token is generated and cached on first request
2. Subsequent requests use the cached token
3. Token is automatically refreshed every 55 minutes
4. No manual cache management needed

**Benefits:**
- Reduces token generation overhead
- Improves API performance
- Prevents token expiration during active use
- Thread-safe concurrent access

### 2. IRN Authentication with Caching

User authentication results are cached to reduce API calls:

- **Cache Duration**: Configurable via Spring Cache
- **Cache Key**: Based on session ID
- **Auto-Eviction**: Tokens are refreshed before expiration

### 3. RestClient with JWT Interceptor

Automatic JWT token injection in all RestClient requests:

- Uses `ClientHttpRequestInterceptor`
- Tokens retrieved from `JwtTokenService` (cached)
- Bearer token automatically added to Authorization header
- No manual token management required

## RSA Private Key Format

The module expects a PKCS#1 RSA private key in PEM format:

```
-----BEGIN RSA PRIVATE KEY-----
MIIEpAIBAAKCAQEA...
...
-----END RSA PRIVATE KEY-----
```

**Generate a new key pair:**
```bash
# Generate private key (PKCS#1 format)
openssl genrsa -out irn-private-key.pem 2048

# Extract public key
openssl rsa -in irn-private-key.pem -pubout -out irn-public-key.pem
```

**Convert PKCS#8 to PKCS#1 (if needed):**
```bash
openssl rsa -in private-key-pkcs8.pem -out private-key-pkcs1.pem
```

## Security Best Practices

1. **Private Key Storage:**
   - Never commit private keys to version control
   - Use secrets management (Vault, AWS Secrets Manager, etc.)
   - Set restrictive file permissions (chmod 600)
   - Mount as read-only volume in containers

2. **Environment Variables:**
   - Use secure secret management systems
   - Rotate JWT keys regularly
   - Use different keys per environment

3. **API URLs:**
   - Use HTTPS for all IRN API calls
   - Validate SSL certificates
   - Configure appropriate timeouts

## Conditional Activation

The IRN integration modules use Spring's `@ConditionalOnProperty` to enable/disable features based on configuration:

### Authorization Service Adapter Activation

```java
@Component
@ConditionalOnProperty(
    name = "igrp.authorization.service.adapter",
    havingValue = "irn"
)
```

This annotation ensures that IRN-specific authorization components are only activated when `igrp.authorization.service.adapter=irn` is set. Without this property, the IRN authorization adapter will not be loaded.

### RestClient Provider Activation

```java
@Configuration
@ConditionalOnProperty(
    name = "igrp.restclient.provider",
    havingValue = "irn"
)
```

This annotation ensures that the JWT-signed RestClient is only created when `igrp.restclient.provider=irn` is set. This allows flexibility to use different RestClient implementations.

**Benefits:**
- ✅ Multiple authorization adapters can coexist (IGRP, IRN, etc.)
- ✅ Clean configuration switching between environments
- ✅ No code changes needed to switch adapters
- ✅ Prevents bean conflicts and unnecessary initialization

## Troubleshooting

### Issue: "IRN authorization adapter not loading"

**Solution:** Ensure `igrp.authorization.service.adapter=irn` is set in your configuration or environment variables.

### Issue: "Could not autowire RestClient"

**Solution:** Ensure `process-runtime-irn-integration` dependency is included and `igrp.restclient.provider=irn` is set.

### Issue: "Failed to load PKCS#1 RSA private key"

**Solution:**
- Verify key format (PKCS#1, not PKCS#8)
- Check file path and permissions
- Ensure key is readable by application

### Issue: "Authentication failed: 401"

**Solution:**
- Verify `irn.api.base-url` is correct
- Check session cookie name matches IRN configuration
- Ensure session ID is valid and not expired

### Issue: JWT Token expires during long-running operations

**Solution:** The `JwtTokenService` automatically refreshes tokens every 55 minutes (5 minutes before expiration). Ensure the cache is properly configured.

## Module Dependencies

```
process-runtime-auth-irn
├── spring-boot-starter-web
├── spring-boot-starter-cache
└── process-runtime-auth-core

process-runtime-irn-integration
├── spring-boot-starter-web
├── spring-boot-starter-cache
├── jjwt-api (0.12.6)
├── jjwt-impl (0.12.6)
└── bcprov-jdk18on (1.79)
```

## Support

For issues or questions, please contact the development team or open an issue in the project repository.

---

**Version:** 0.1.0-beta.23
**Last Updated:** 2026-03-05

```
<dependency>
   <groupId>cv.igrp.framework</groupId>
   <artifactId>process-runtime-irn-integration</artifactId>
   <version>0.1.0-beta.23</version>
</dependency>

<dependency>
   <groupId>cv.igrp.framework</groupId>
   <artifactId>process-runtime-auth-irn</artifactId>
   <version>0.1.0-beta.23</version>
</dependency>
```