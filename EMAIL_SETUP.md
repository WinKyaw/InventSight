# Email Configuration Setup Guide

This guide explains how to configure email functionality for InventSight, including email verification, password reset, and MFA OTP codes.

## Overview

InventSight supports multiple email providers:
- **SMTP** - Standard email via any SMTP server (Gmail, Outlook, custom servers)
- **AWS SES** - Amazon Simple Email Service (production-ready, scalable)
- **SendGrid** - SendGrid Email API (production-ready, easy to use)

## Required Configuration

All email configuration is done via environment variables or `application.yml`.

### Basic Settings

```yaml
inventsight:
  email:
    provider: smtp                              # Options: smtp, ses, sendgrid
    from-address: noreply@inventsight.com       # "From" email address
    from-name: InventSight                      # "From" display name
    verification-url: http://localhost:3000/verify-email  # Frontend verification page URL
```

## SMTP Configuration (Recommended for Development)

SMTP is the simplest option and works with most email providers.

### Environment Variables

```bash
EMAIL_PROVIDER=smtp
EMAIL_HOST=smtp.example.com
EMAIL_PORT=587
EMAIL_USERNAME=your-email@example.com
EMAIL_PASSWORD=your-password
EMAIL_FROM=noreply@inventsight.com
EMAIL_VERIFICATION_URL=http://localhost:3000/verify-email
```

### Gmail Configuration

**Important:** Gmail requires an "App Password" instead of your regular password.

1. Enable 2-Factor Authentication on your Google account
2. Generate an App Password:
   - Go to: https://myaccount.google.com/apppasswords
   - Select "Mail" and "Other (Custom name)"
   - Copy the generated 16-character password

```bash
EMAIL_PROVIDER=smtp
EMAIL_HOST=smtp.gmail.com
EMAIL_PORT=587
EMAIL_USERNAME=your-email@gmail.com
EMAIL_PASSWORD=your-16-char-app-password
EMAIL_FROM=your-email@gmail.com
```

### Outlook/Office 365 Configuration

```bash
EMAIL_PROVIDER=smtp
EMAIL_HOST=smtp-mail.outlook.com
EMAIL_PORT=587
EMAIL_USERNAME=your-email@outlook.com
EMAIL_PASSWORD=your-password
EMAIL_FROM=your-email@outlook.com
```

### Custom SMTP Server

```bash
EMAIL_PROVIDER=smtp
EMAIL_HOST=mail.yourdomain.com
EMAIL_PORT=587                    # Or 465 for SSL, 25 for plain
EMAIL_USERNAME=your-smtp-username
EMAIL_PASSWORD=your-smtp-password
EMAIL_FROM=noreply@yourdomain.com
```

## AWS SES Configuration (Production)

AWS Simple Email Service is recommended for production deployments.

### Prerequisites

1. AWS Account with SES enabled
2. Verify sender email address in SES console
3. Request production access (remove sandbox limits)
4. Create SMTP credentials in SES console

### Environment Variables

```bash
EMAIL_PROVIDER=smtp                          # Use SMTP with SES
EMAIL_HOST=email-smtp.us-east-1.amazonaws.com  # Replace region
EMAIL_PORT=587
EMAIL_USERNAME=your-ses-smtp-username        # From SES console
EMAIL_PASSWORD=your-ses-smtp-password        # From SES console
EMAIL_FROM=verified-email@yourdomain.com     # Must be verified in SES
```

### AWS Region SMTP Endpoints

| Region | SMTP Endpoint |
|--------|---------------|
| US East (N. Virginia) | email-smtp.us-east-1.amazonaws.com |
| US West (Oregon) | email-smtp.us-west-2.amazonaws.com |
| EU (Ireland) | email-smtp.eu-west-1.amazonaws.com |
| Asia Pacific (Tokyo) | email-smtp.ap-northeast-1.amazonaws.com |

For complete list: https://docs.aws.amazon.com/ses/latest/dg/regions.html

## SendGrid Configuration (Production)

SendGrid is a popular email service with good deliverability and analytics.

### Prerequisites

1. Create a SendGrid account: https://sendgrid.com
2. Verify sender email/domain
3. Create an API key with "Mail Send" permission

### Environment Variables

```bash
EMAIL_PROVIDER=smtp                  # Use SMTP with SendGrid
EMAIL_HOST=smtp.sendgrid.net
EMAIL_PORT=587
EMAIL_USERNAME=apikey                # Literal string "apikey"
EMAIL_PASSWORD=your-sendgrid-api-key # Your actual API key
EMAIL_FROM=verified-email@yourdomain.com
```

## Testing Email Configuration

### 1. Start the Application

```bash
./mvnw spring-boot:run
```

### 2. Test Registration with Email Verification

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "username": "testuser",
    "password": "Test123!@#",
    "fullName": "Test User"
  }'
```

### 3. Check Application Logs

Successful email sending will show:
```
✅ Verification email sent successfully to: test@example.com
```

Failed email sending will show:
```
❌ Failed to send verification email to: test@example.com
Error: [error message]
```

### 4. Test Resend Verification Email

```bash
curl -X POST http://localhost:8080/api/auth/resend-verification \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com"
  }'
```

## Troubleshooting

### "Email service not configured" Error

**Cause:** JavaMailSender bean is not configured (missing SMTP settings)

**Solution:** 
- Ensure all EMAIL_* environment variables are set
- Verify EMAIL_HOST and EMAIL_PORT are correct
- Check that EMAIL_USERNAME and EMAIL_PASSWORD are provided

### "Authentication failed" Error

**Cause:** Invalid SMTP credentials

**Solution:**
- For Gmail: Use App Password, not regular password
- Verify credentials in your email provider console
- Check if 2FA/MFA is required by your provider

### "Connection timeout" Error

**Cause:** Cannot connect to SMTP server

**Solution:**
- Verify EMAIL_HOST is correct
- Check if port 587 (or 465) is not blocked by firewall
- Try alternative ports: 587 (TLS), 465 (SSL), or 25 (plain)

### "Sender address rejected" Error

**Cause:** Email address not verified or domain not authorized

**Solution:**
- For AWS SES: Verify sender email in SES console
- For SendGrid: Verify sender email or domain
- Ensure EMAIL_FROM matches a verified address

### Emails Not Received

**Cause:** Multiple possible reasons

**Solution:**
1. Check spam/junk folder
2. Verify recipient email address is valid
3. Check application logs for send confirmation
4. For AWS SES: Check you're not in sandbox mode
5. For production: Check sender reputation and SPF/DKIM records

## Email Features in InventSight

### 1. Email Verification
- Sent after user registration
- Contains verification link with token
- Token expires after 24 hours
- Endpoint: POST `/api/auth/resend-verification`

### 2. Password Reset
- Sent when user requests password reset
- Contains reset link with token
- Token expires after 1 hour
- Endpoint: POST `/api/auth/forgot-password`

### 3. MFA OTP Codes
- Sent during login when MFA is enabled
- Contains 6-digit verification code
- Code expires after 5 minutes
- Endpoint: POST `/api/auth/mfa/send-login-otp`

## Production Recommendations

1. **Use a dedicated email service** (AWS SES or SendGrid) instead of Gmail/Outlook
2. **Set up SPF, DKIM, and DMARC records** for your domain to improve deliverability
3. **Monitor email bounce rates** and handle hard bounces
4. **Implement email rate limiting** to prevent abuse
5. **Use a verified domain** instead of a generic email address
6. **Set up email webhook notifications** to track delivery status
7. **Test email deliverability** before going live

## Security Best Practices

1. **Never commit credentials** - Always use environment variables
2. **Use App Passwords** for Gmail instead of account passwords
3. **Rotate API keys regularly** for SendGrid and AWS
4. **Enable MFA** on your email service provider account
5. **Monitor for suspicious activity** in your email service logs
6. **Implement rate limiting** on email sending endpoints
7. **Validate email addresses** before sending
8. **Use HTTPS** for verification links

## Environment Variables Reference

| Variable | Description | Required | Default |
|----------|-------------|----------|---------|
| EMAIL_PROVIDER | Email provider type | No | smtp |
| EMAIL_HOST | SMTP server hostname | Yes | smtp.example.com |
| EMAIL_PORT | SMTP server port | Yes | 587 |
| EMAIL_USERNAME | SMTP username/email | Yes | - |
| EMAIL_PASSWORD | SMTP password/API key | Yes | - |
| EMAIL_FROM | From email address | No | noreply@inventsight.com |
| EMAIL_VERIFICATION_URL | Frontend verification page URL | No | http://localhost:3000/verify-email |

## Support

For issues with email configuration:
1. Check application logs for error messages
2. Verify environment variables are set correctly
3. Test SMTP connection using telnet: `telnet smtp.example.com 587`
4. Review your email provider's documentation
5. Check InventSight GitHub issues: https://github.com/WinKyaw/InventSight/issues

## Additional Resources

- [Spring Boot Mail Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/io.html#io.email)
- [AWS SES Documentation](https://docs.aws.amazon.com/ses/)
- [SendGrid Documentation](https://docs.sendgrid.com/)
- [Gmail App Passwords](https://support.google.com/accounts/answer/185833)
