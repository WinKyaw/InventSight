package com.pos.inventsight.config;

import com.pos.inventsight.filter.IdempotencyKeyFilter;
import com.pos.inventsight.filter.RateLimitingFilter;
import com.pos.inventsight.service.UserService;
import com.pos.inventsight.tenant.CompanyTenantFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;

/**
 * Security Configuration for InventSight
 * 
 * OAuth2 Login Configuration:
 * - By default, OAuth2 login is NOT enabled to avoid startup failures
 * - To enable OAuth2 login with Google/Microsoft/Okta providers:
 *   1. Activate the 'oauth-login' profile: --spring.profiles.active=oauth-login
 *   2. Set required environment variables:
 *      - GOOGLE_CLIENT_ID, GOOGLE_CLIENT_SECRET
 *      - MICROSOFT_CLIENT_ID, MICROSOFT_CLIENT_SECRET, MICROSOFT_TENANT_ID
 *      - OKTA_CLIENT_ID, OKTA_CLIENT_SECRET, OKTA_ISSUER_URI
 *   3. Set inventsight.security.oauth2.login.enabled=true
 * 
 * OAuth2 Resource Server is gated behind null defaults and requires:
 *   - inventsight.security.oauth2.resource-server.enabled=true
 *   - JWT_ISSUER_URI or JWT_JWK_SET_URI environment variables
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private AuthEntryPointJwt unauthorizedHandler;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private CompanyTenantFilter companyTenantFilter;
    
    @Autowired
    private RateLimitingFilter rateLimitingFilter;
    
    @Autowired
    private IdempotencyKeyFilter idempotencyKeyFilter;
    
    @Autowired
    private AuthTokenFilter authTokenFilter;
    
    @Autowired(required = false)
    private JwtDecoder jwtDecoder;
    
    @Autowired(required = false)
    private com.pos.inventsight.service.CustomOAuth2UserService customOAuth2UserService;
    
    @Autowired(required = false)
    private org.springframework.security.oauth2.client.registration.ClientRegistrationRepository clientRegistrationRepository;
    
    @Value("${inventsight.security.oauth2.resource-server.enabled:false}")
    private boolean oauth2Enabled;
    
    @Value("${inventsight.security.oauth2.login.enabled:true}")
    private boolean oauth2LoginEnabled;
    
    @Value("${inventsight.security.saml.enabled:false}")
    private boolean samlEnabled;
    
    @Value("${inventsight.security.local-login.enabled:false}")
    private boolean localLoginEnabled;
    
    @Value("${inventsight.security.offline-mode.enabled:false}")
    private boolean offlineModeEnabled;
    
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userService);
        authProvider.setPasswordEncoder(passwordEncoder);
        return authProvider;
    }
    
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        logger.info("=== Initializing Spring Security Configuration ===");
        logger.info("Offline Mode enabled: {}", offlineModeEnabled);
        logger.info("OAuth2 Resource Server enabled: {}", oauth2Enabled);
        logger.info("OAuth2 Login enabled: {}", oauth2LoginEnabled);
        logger.info("Local Login enabled: {}", localLoginEnabled);
        
        if (offlineModeEnabled) {
            logger.warn("⚠️ WARNING: Security is DISABLED (Offline Mode) - ALL endpoints are accessible without authentication!");
            
            http.cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
            
            return http.build();
        }
        
        http.cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .exceptionHandling(exception -> exception.authenticationEntryPoint(unauthorizedHandler))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> {
                // Local login endpoints - only permit if local login is enabled
                if (localLoginEnabled) {
                    auth.requestMatchers("/auth/login", "/auth/login/**").permitAll()
                        .requestMatchers("/auth/register", "/auth/signup").permitAll()
                        .requestMatchers("/api/register").permitAll()
                        .requestMatchers("/api/auth/register").permitAll()
                        .requestMatchers("/api/auth/signup").permitAll()
                        .requestMatchers("/register").permitAll();
                } else {
                    // Explicitly deny local login endpoints when disabled
                    auth.requestMatchers("/auth/login", "/auth/login/**").denyAll()
                        .requestMatchers("/auth/register", "/auth/signup").denyAll()
                        .requestMatchers("/api/register").denyAll()
                        .requestMatchers("/api/auth/register").denyAll()
                        .requestMatchers("/api/auth/signup").denyAll()
                        .requestMatchers("/register").denyAll();
                }
                
                // Other authentication endpoints - always permit
                auth.requestMatchers("/auth/check-email").permitAll()
                    .requestMatchers("/auth/verify-email").permitAll()
                    .requestMatchers("/auth/resend-verification").permitAll()
                    .requestMatchers("/auth/validate-password").permitAll()
                    .requestMatchers("/auth/invite/accept").permitAll()
                    
                    // OAuth2 login endpoints - permit when OAuth2 login is enabled
                    .requestMatchers("/oauth2/**").permitAll()
                    .requestMatchers("/login/**").permitAll()
                    
                    // Other public endpoints
                    .requestMatchers("/dashboard/live-data").permitAll() // Allow live sync for React Native
                    .requestMatchers("/health/**").permitAll()
                    .requestMatchers("/actuator/**").permitAll()
                    .requestMatchers("/swagger-ui/**").permitAll()
                    .requestMatchers("/v3/api-docs/**").permitAll()
                    .requestMatchers("/docs/**").permitAll()
                    .requestMatchers("/favicon.ico").permitAll()
                    .anyRequest().authenticated();
            });
        
        // Configure OAuth2 Resource Server if enabled
        if (oauth2Enabled && jwtDecoder != null) {
            logger.info("Enabling OAuth2 Resource Server with JWKS validation");
            http.oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.decoder(jwtDecoder))
            );
        }
        
        // Configure OAuth2 Login if enabled and client registrations are available
        // OAuth2 login requires the 'oauth-login' profile with proper client credentials
        if (oauth2LoginEnabled && customOAuth2UserService != null && clientRegistrationRepository != null) {
            logger.info("Enabling OAuth2 Login (Google, Microsoft, Okta)");
            http.oauth2Login(oauth2 -> oauth2
                .userInfoEndpoint(userInfo -> userInfo
                    .userService(customOAuth2UserService)
                )
            );
        } else if (oauth2LoginEnabled) {
            logger.warn("OAuth2 Login requested but client registrations not configured");
            logger.warn("To enable OAuth2: use --spring.profiles.active=oauth-login and set provider credentials");
        }
        
        // SAML2 Login support disabled - dependency not available
        // To enable SAML2, add spring-security-saml2-service-provider dependency
        if (samlEnabled) {
            logger.warn("SAML2 Login requested but dependency not available");
        }
        
        http.authenticationProvider(authenticationProvider());
        
        // Add filters in correct order:
        // 1. RateLimitingFilter (earliest - rate limiting check before any processing)
        // 2. AuthTokenFilter (JWT authentication - MUST run before CompanyTenantFilter)
        // 3. CompanyTenantFilter (tenant context - requires authenticated user from step 2)
        // 4. IdempotencyKeyFilter (idempotency check - after auth/tenant, so cache keys include tenant)
        
        logger.info("=== Configuring Security Filter Chain ===");
        logger.info("Filter Order:");
        logger.info("  1. RateLimitingFilter (before SecurityContextHolderFilter)");
        logger.info("  2. Spring Security Internal Filters");
        logger.info("  3. AuthTokenFilter (before AnonymousAuthenticationFilter)");
        logger.info("  4. CompanyTenantFilter (after AuthTokenFilter)");
        logger.info("  5. IdempotencyKeyFilter (after CompanyTenantFilter)");
        
        logger.info("Adding RateLimitingFilter before SecurityContextHolderFilter");
        http.addFilterBefore(rateLimitingFilter, SecurityContextHolderFilter.class);
        
        logger.info("Adding AuthTokenFilter before AnonymousAuthenticationFilter");
        http.addFilterBefore(authTokenFilter, AnonymousAuthenticationFilter.class);
        
        logger.info("Adding CompanyTenantFilter after AuthTokenFilter");
        http.addFilterAfter(companyTenantFilter, AuthTokenFilter.class);
        
        logger.info("Adding IdempotencyKeyFilter after CompanyTenantFilter");
        http.addFilterAfter(idempotencyKeyFilter, CompanyTenantFilter.class);
        
        logger.info("=== Filter chain configured successfully ===");
        logger.info("Spring Security Configuration completed");
        
        // Build and log filter chain for verification
        SecurityFilterChain chain = http.build();
        logger.info("=== Spring Security Filter Chain Verification ===");
        logger.info("Total filters in chain: {}", chain.getFilters().size());
        for (int i = 0; i < chain.getFilters().size(); i++) {
            jakarta.servlet.Filter filter = chain.getFilters().get(i);
            logger.info("  {}. {}", i + 1, filter.getClass().getSimpleName());
        }
        logger.info("=== End Filter Chain Verification ===");
        return chain;
    }
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        logger.debug("Configuring CORS for React Native app");
        
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.asList("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        
        logger.debug("CORS configuration completed for all origins");
        return source;
    }

    // @Bean
    // public WebMvcConfigurer corsConfigurer() {
    // return new WebMvcConfigurer() {
    //     @Override
    //     public void addCorsMappings(CorsRegistry registry) {
    //         registry.addMapping("/api/**")
    //                 .allowedOrigins("http://localhost:8080") // or the origin of your frontend
    //                 .allowedMethods("*")
    //                 .allowedHeaders("*");
    //     }
    //     };
    // }

    @Bean
    public WebMvcConfigurer corsConfigurer() {
    return new WebMvcConfigurer() {
        @Override
        public void addCorsMappings(CorsRegistry registry) {
            registry.addMapping("/api/**")
                    .allowedOrigins("http://localhost:8080") // or the origin of your frontend
                    .allowedMethods("*")
                    .allowedHeaders("*");
        }
        };
    }
}