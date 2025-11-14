package com.pos.inventsight.integration;

import com.pos.inventsight.config.JwtUtils;
import com.pos.inventsight.model.sql.Company;
import com.pos.inventsight.model.sql.CompanyRole;
import com.pos.inventsight.model.sql.CompanyStoreUser;
import com.pos.inventsight.model.sql.User;
import com.pos.inventsight.repository.sql.CompanyRepository;
import com.pos.inventsight.repository.sql.CompanyStoreUserRepository;
import com.pos.inventsight.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for JWT Authentication Flow
 * Tests the complete authentication chain:
 * 1. User authentication with JWT token
 * 2. SecurityContext population
 * 3. CompanyTenantFilter authentication checks
 * 4. API endpoint access with valid JWT
 */
@SpringBootTest(properties = {
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration," +
    "org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration," +
    "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class JwtAuthenticationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CompanyRepository companyRepository;

    @MockBean
    private CompanyStoreUserRepository companyStoreUserRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private UserService userService;

    private User testUser;
    private Company testCompany;
    private String jwtToken;

    @BeforeEach
    public void setUp() {
        // Create test user
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("jwttest");
        testUser.setEmail("jwttest@inventsight.com");
        testUser.setPassword(passwordEncoder.encode("password123"));
        testUser.setFirstName("JWT");
        testUser.setLastName("Test");
        testUser.setEmailVerified(true);
        testUser.setIsActive(true);

        // Create test company
        testCompany = new Company();
        testCompany.setId(UUID.randomUUID());
        testCompany.setName("JWT Test Company");
        testCompany.setDescription("Test company for JWT auth");
        testCompany.setEmail(testUser.getEmail());
        testCompany.setCreatedBy(testUser.getUsername());
        testCompany.setCreatedAt(LocalDateTime.now());
        testCompany.setUpdatedAt(LocalDateTime.now());
        testCompany.setIsActive(true);

        // Create company-user membership
        CompanyStoreUser membership = new CompanyStoreUser(
            testCompany,
            testUser,
            CompanyRole.FOUNDER,
            testUser.getUsername()
        );

        // Set default tenant for user
        testUser.setDefaultTenantId(testCompany.getId());

        // Generate JWT token with tenant_id
        jwtToken = jwtUtils.generateJwtToken(testUser, testCompany.getId().toString());

        // Mock repository responses
        when(companyRepository.existsById(testCompany.getId())).thenReturn(true);
        
        List<CompanyStoreUser> memberships = new ArrayList<>();
        memberships.add(membership);
        when(companyStoreUserRepository.findByUserAndIsActiveTrue(any(User.class)))
            .thenReturn(memberships);
    }

    @Test
    public void testJwtTokenGeneration() {
        // Verify token is generated
        assertThat(jwtToken).isNotNull();
        assertThat(jwtToken).isNotEmpty();

        // Verify token is valid
        assertThat(jwtUtils.validateJwtToken(jwtToken)).isTrue();

        // Verify username can be extracted
        String username = jwtUtils.getUsernameFromJwtToken(jwtToken);
        assertThat(username).isEqualTo(testUser.getEmail());

        // Verify tenant_id can be extracted
        String tenantId = jwtUtils.getTenantIdFromJwtToken(jwtToken);
        assertThat(tenantId).isEqualTo(testCompany.getId().toString());
    }

    @Test
    public void testAuthTokenFilterSetsAuthentication() throws Exception {
        // Make a request to a protected endpoint with JWT token
        // This should trigger AuthTokenFilter to set authentication in SecurityContext
        MvcResult result = mockMvc.perform(get("/api/products")
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        // The request might fail due to other reasons (e.g., no products),
        // but we're testing that AuthTokenFilter processed the JWT
        // Log output should show authentication was set
        int status = result.getResponse().getStatus();
        
        // Should not be 401 if authentication was successful
        // Could be 200 (success), 403 (forbidden), or 404 (not found)
        // but NOT 401 (unauthorized)
        assertThat(status).isNotEqualTo(401);
    }

    @Test
    public void testCompanyTenantFilterReceivesAuthentication() throws Exception {
        // Test that CompanyTenantFilter receives authenticated context
        // from AuthTokenFilter
        MvcResult result = mockMvc.perform(get("/api/products")
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        int status = result.getResponse().getStatus();
        String content = result.getResponse().getContentAsString();

        // Should not fail with "Authentication required" error from CompanyTenantFilter
        // If authentication was properly set, we should not get 401 with that specific error
        if (status == 401) {
            assertThat(content).doesNotContain("Authentication required");
        }
    }

    @Test
    public void testApiEndpointAccessWithValidJwt() throws Exception {
        // Test accessing an API endpoint with valid JWT token
        // This tests the complete flow: JWT parsing -> Auth setting -> Tenant filter -> Endpoint
        MvcResult result = mockMvc.perform(get("/api/products")
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        int status = result.getResponse().getStatus();

        // Should successfully pass authentication
        // Status could be 200 (success) or 404 (no products found)
        // but should NOT be 401 (unauthorized)
        assertThat(status).isIn(200, 404);
    }

    @Test
    public void testApiEndpointAccessWithoutJwt() throws Exception {
        // Test accessing an API endpoint without JWT token
        // Should fail with 401 Unauthorized
        mockMvc.perform(get("/api/products")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testApiEndpointAccessWithInvalidJwt() throws Exception {
        // Test accessing an API endpoint with invalid JWT token
        // Should fail with 401 Unauthorized
        mockMvc.perform(get("/api/products")
                .header("Authorization", "Bearer invalid.jwt.token")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testJwtTokenWithoutTenantId() throws Exception {
        // Generate token without tenant_id
        String tokenWithoutTenant = jwtUtils.generateJwtToken(testUser);

        // Request should fail because tenant_id is required in JWT-only mode
        MvcResult result = mockMvc.perform(get("/api/products")
                .header("Authorization", "Bearer " + tokenWithoutTenant)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        assertThat(content).contains("tenant_id claim is required in JWT token");
    }

    @Test
    public void testSecurityContextPopulation() throws Exception {
        // Test that SecurityContext is properly populated after JWT authentication
        // by making a request and verifying we can access the endpoint
        // (which requires authentication to be set in SecurityContext)
        
        MvcResult result = mockMvc.perform(get("/api/products")
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        // If SecurityContext is properly populated, we should not get
        // "Authentication required" error from CompanyTenantFilter
        int status = result.getResponse().getStatus();
        String content = result.getResponse().getContentAsString();

        // Verify that we passed authentication (no 401 with "Authentication required")
        if (status == 401) {
            assertThat(content).doesNotContain("Authentication required");
        } else {
            // If not 401, authentication was successful
            assertThat(status).isIn(200, 404, 403);
        }
    }
}
