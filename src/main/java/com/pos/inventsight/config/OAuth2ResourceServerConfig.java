package com.pos.inventsight.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

import java.time.Duration;
import java.util.List;

/**
 * OAuth2 Resource Server configuration with JWKS validation
 * Enables external OAuth2/OIDC token validation when feature flag is enabled
 */
@Configuration
@ConditionalOnProperty(name = "inventsight.security.oauth2.resource-server.enabled", havingValue = "true")
public class OAuth2ResourceServerConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri:}")
    private String issuerUri;

    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri:}")
    private String jwkSetUri;

    @Value("${inventsight.security.oauth2.resource-server.audiences:inventsight-api}")
    private List<String> audiences;

    @Value("${inventsight.security.oauth2.resource-server.clock-skew-seconds:60}")
    private Long clockSkewSeconds;

    /**
     * JWT Decoder with issuer, audience validation and clock skew tolerance
     */
    @Bean
    public JwtDecoder jwtDecoder() {
        NimbusJwtDecoder jwtDecoder;
        
        // Prefer JWK Set URI if provided, otherwise use issuer URI
        if (jwkSetUri != null && !jwkSetUri.isEmpty()) {
            jwtDecoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
        } else if (issuerUri != null && !issuerUri.isEmpty()) {
            jwtDecoder = JwtDecoders.fromIssuerLocation(issuerUri);
        } else {
            throw new IllegalStateException(
                "OAuth2 Resource Server enabled but no issuer-uri or jwk-set-uri configured"
            );
        }

        // Add validators for issuer, audience, and timestamps with clock skew
        OAuth2TokenValidator<Jwt> withClockSkew = new DelegatingOAuth2TokenValidator<>(
            new JwtTimestampValidator(Duration.ofSeconds(clockSkewSeconds)),
            new JwtIssuerValidator(issuerUri),
            new JwtClaimValidator<List<String>>("aud", aud -> aud != null && 
                aud.stream().anyMatch(audiences::contains))
        );
        
        jwtDecoder.setJwtValidator(withClockSkew);
        
        return jwtDecoder;
    }

    /**
     * JWT Authentication Converter to extract authorities and custom claims
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        grantedAuthoritiesConverter.setAuthoritiesClaimName("roles");
        grantedAuthoritiesConverter.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);
        
        return jwtAuthenticationConverter;
    }
}
