package com.pos.inventsight.config;

import com.pos.inventsight.filter.IdempotencyKeyFilter;
import com.pos.inventsight.filter.RateLimitingFilter;
import com.pos.inventsight.service.UserService;
import com.pos.inventsight.tenant.CompanyTenantFilter;
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
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {
    
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
    
    @Autowired(required = false)
    private JwtDecoder jwtDecoder;
    
    @Autowired(required = false)
    private com.pos.inventsight.service.CustomOAuth2UserService customOAuth2UserService;
    
    @Value("${inventsight.security.oauth2.resource-server.enabled:false}")
    private boolean oauth2Enabled;
    
    @Value("${inventsight.security.oauth2.login.enabled:false}")
    private boolean oauth2LoginEnabled;
    
    @Value("${inventsight.security.saml.enabled:false}")
    private boolean samlEnabled;
    
    @Bean
    public AuthTokenFilter authenticationJwtTokenFilter() {
        return new AuthTokenFilter();
    }
    
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
        System.out.println("🔒 InventSight - Initializing Spring Security Configuration");
        System.out.println("📅 OAuth2 Resource Server enabled: " + oauth2Enabled);
        System.out.println("👤 Current User's Login: WinKyaw");
        
        http.cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .exceptionHandling(exception -> exception.authenticationEntryPoint(unauthorizedHandler))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> 
                auth
                    // Authentication endpoints - PUBLIC ACCESS (no JWT required)
                    .requestMatchers("/auth/**").permitAll()
                    .requestMatchers("/api/register").permitAll()      // Direct /api/register endpoint
                    .requestMatchers("/api/auth/register").permitAll() // Full context path registration
                    .requestMatchers("/api/auth/signup").permitAll()   // Signup alias endpoint
                    .requestMatchers("/register").permitAll()          // Alternative register route
                    
                    // Other public endpoints
                    .requestMatchers("/dashboard/live-data").permitAll() // Allow live sync for React Native
                    .requestMatchers("/health/**").permitAll()
                    .requestMatchers("/actuator/**").permitAll()
                    .requestMatchers("/swagger-ui/**").permitAll()
                    .requestMatchers("/v3/api-docs/**").permitAll()
                    .requestMatchers("/docs/**").permitAll()
                    .requestMatchers("/favicon.ico").permitAll()
                    .anyRequest().authenticated()
            );
        
        // Configure OAuth2 Resource Server if enabled
        if (oauth2Enabled && jwtDecoder != null) {
            System.out.println("✅ Enabling OAuth2 Resource Server with JWKS validation");
            http.oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.decoder(jwtDecoder))
            );
        }
        
        // Configure OAuth2 Login if enabled
        // To enable OAuth2 login: set spring.profiles.active=oauth-login (or add it to active profiles)
        // and provide the required client environment variables (GOOGLE_CLIENT_ID, MICROSOFT_CLIENT_ID, etc.)
        if (oauth2LoginEnabled && customOAuth2UserService != null) {
            System.out.println("✅ Enabling OAuth2 Login (Google, Microsoft, Okta)");
            http.oauth2Login(oauth2 -> oauth2
                .userInfoEndpoint(userInfo -> userInfo
                    .userService(customOAuth2UserService)
                )
            );
        }
        
        // SAML2 Login support disabled - dependency not available
        // To enable SAML2, add spring-security-saml2-service-provider dependency
        if (samlEnabled) {
            System.out.println("⚠️ SAML2 Login requested but dependency not available");
        }
        
        http.authenticationProvider(authenticationProvider());
        
        // Add filters in correct order:
        // 1. RateLimitingFilter (earliest - before any processing)
        // 2. CompanyTenantFilter (tenant context)
        // 3. Auth layer (JWT filter)
        // 4. IdempotencyKeyFilter (after auth/tenant, so cache keys include tenant)
        http.addFilterBefore(rateLimitingFilter, UsernamePasswordAuthenticationFilter.class);
        http.addFilterBefore(companyTenantFilter, UsernamePasswordAuthenticationFilter.class);
        http.addFilterBefore(authenticationJwtTokenFilter(), UsernamePasswordAuthenticationFilter.class);
        http.addFilterAfter(idempotencyKeyFilter, UsernamePasswordAuthenticationFilter.class);
        
        System.out.println("✅ InventSight Spring Security Configuration completed with all filters");
        return http.build();
    }
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        System.out.println("🌐 InventSight - Configuring CORS for React Native app");
        
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.asList("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        
        System.out.println("✅ InventSight CORS configuration completed for all origins");
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