package com.pos.inventsight.config;

import com.pos.inventsight.service.UserService;
import com.pos.inventsight.tenant.CompanyTenantFilter;
import org.springframework.beans.factory.annotation.Autowired;
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
        System.out.println("📅 Current Date and Time (UTC): 2025-08-26 09:04:35");
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
        
        http.authenticationProvider(authenticationProvider());
        // Add CompanyTenantFilter before JWT filter to ensure tenant context is set early
        http.addFilterBefore(companyTenantFilter, UsernamePasswordAuthenticationFilter.class);
        http.addFilterBefore(authenticationJwtTokenFilter(), UsernamePasswordAuthenticationFilter.class);
        
        System.out.println("✅ InventSight Spring Security Configuration completed with CompanyTenantFilter");
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