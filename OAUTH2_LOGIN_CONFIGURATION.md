# OAuth2 Login Configuration Guide

## Overview
InventSight supports OAuth2 login with Google, Microsoft, and Okta providers. By default, OAuth2 login is **disabled** to ensure the application starts successfully without requiring OAuth2 credentials.

## Default Behavior
- Application starts without OAuth2 client configuration
- No OAuth2 provider credentials required
- JWT-based authentication using local user database is enabled by default

## Enabling OAuth2 Login

### Prerequisites
1. Register your application with OAuth2 providers:
   - **Google**: [Google Cloud Console](https://console.cloud.google.com/)
   - **Microsoft**: [Azure Portal](https://portal.azure.com/)
   - **Okta**: [Okta Developer Console](https://developer.okta.com/)

2. Obtain client credentials (client-id and client-secret) for each provider

### Configuration Steps

#### 1. Set Environment Variables
Configure the following environment variables based on which providers you want to enable:

**Google:**
```bash
export GOOGLE_CLIENT_ID="your-google-client-id"
export GOOGLE_CLIENT_SECRET="your-google-client-secret"
```

**Microsoft:**
```bash
export MICROSOFT_CLIENT_ID="your-microsoft-client-id"
export MICROSOFT_CLIENT_SECRET="your-microsoft-client-secret"
export MICROSOFT_TENANT_ID="your-tenant-id"  # Optional, defaults to 'common' for multi-tenant apps
```

*Note: Microsoft's 'common' endpoint supports both personal Microsoft accounts and organizational accounts. Use a specific tenant ID for single-tenant applications.*

**Okta:**
```bash
export OKTA_CLIENT_ID="your-okta-client-id"
export OKTA_CLIENT_SECRET="your-okta-client-secret"
export OKTA_ISSUER_URI="https://your-domain.okta.com/oauth2/default"
```

#### 2. Activate the OAuth2 Login Profile

**Option A: Command Line**
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=oauth-login
```

Or with jar file:
```bash
java -jar inventsight.jar --spring.profiles.active=oauth-login
```

**Option B: Application Configuration**
Add to `application.yml` or `application.properties`:
```yaml
spring:
  profiles:
    active: oauth-login
```

#### 3. Enable OAuth2 Login Feature Flag
Set the feature flag to enable OAuth2 login functionality:
```bash
export OAUTH2_LOGIN_ENABLED=true
```

Or add to configuration:
```yaml
inventsight:
  security:
    oauth2:
      login:
        enabled: true
```

## Profile Configuration Details

The `application-oauth-login.yml` profile contains:
- OAuth2 client registrations for Google, Microsoft, and Okta
- Provider-specific configurations
- No default values for credentials (prevents accidental exposure)

## Testing OAuth2 Login

### Local Development
1. Set environment variables as described above
2. Start application with oauth-login profile
3. Navigate to the OAuth2 login endpoints
4. Verify successful authentication and user creation

### Expected Behavior
- **With profile + env vars**: OAuth2 login works, users can authenticate with external providers
- **Without profile**: Application starts normally, only local JWT authentication available
- **With profile but missing env vars**: Application fails to start with clear error message (by design)

## Troubleshooting

### Application fails to start with "Client id must not be empty"
- **Cause**: The oauth-login profile is active but environment variables are not set
- **Solution**: Either set the required environment variables or remove the oauth-login profile

### OAuth2 login not working even with profile active
- **Verify**: Check that `OAUTH2_LOGIN_ENABLED=true` is set
- **Verify**: Ensure all required environment variables for your provider are set correctly
- **Check**: Review application logs for OAuth2-related errors

### How to disable OAuth2 login
- Remove `oauth-login` from active profiles
- Or set `OAUTH2_LOGIN_ENABLED=false`

## Security Considerations
- Never commit OAuth2 credentials to version control
- Use environment variables or secure secret management systems
- Rotate credentials regularly
- Use different credentials for development, staging, and production environments
- Review and configure redirect URIs carefully in provider consoles

## Related Configuration
- **OAuth2 Resource Server**: Separate feature for validating external JWT tokens
  - Controlled by `inventsight.security.oauth2.resource-server.enabled`
  - Requires `JWT_ISSUER_URI` or `JWT_JWK_SET_URI`
- **SAML**: Not yet implemented (dependency not available)

## Additional Resources
- [Spring Security OAuth2 Client Documentation](https://docs.spring.io/spring-security/reference/servlet/oauth2/client/index.html)
- [SecurityConfig.java](src/main/java/com/pos/inventsight/config/SecurityConfig.java) - See JavaDoc for detailed configuration information
