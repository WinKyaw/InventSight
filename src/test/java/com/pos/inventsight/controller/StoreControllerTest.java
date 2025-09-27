package com.pos.inventsight.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pos.inventsight.dto.StoreRequest;
import com.pos.inventsight.model.sql.User;
import com.pos.inventsight.service.StoreService;
import com.pos.inventsight.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(StoreController.class)
@DisplayName("Store Controller Integration Tests")
class StoreControllerTest {

    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @MockBean
    private StoreService storeService;
    
    @MockBean
    private UserService userService;
    
    private StoreRequest storeRequest;
    
    @BeforeEach
    void setUp() {
        storeRequest = new StoreRequest();
        storeRequest.setStoreName("Test Store");
        storeRequest.setDescription("Test store description");
        storeRequest.setAddress("123 Test Street");
        storeRequest.setCity("Test City");
        storeRequest.setState("Test State");
        storeRequest.setCountry("Test Country");
        storeRequest.setPhone("555-1234");
        storeRequest.setEmail("test@store.com");
    }
    
    @Test
    @DisplayName("POST /stores should create store when authenticated")
    @WithMockUser(username = "testuser")
    void testCreateStore() throws Exception {
        mockMvc.perform(post("/stores")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(storeRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Store created successfully"));
        
        System.out.println("✅ Test passed: POST /stores creates store");
    }
    
    @Test
    @DisplayName("GET /stores should return user stores when authenticated")
    @WithMockUser(username = "testuser")
    void testGetUserStores() throws Exception {
        mockMvc.perform(get("/stores"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Stores retrieved successfully"));
        
        System.out.println("✅ Test passed: GET /stores returns user stores");
    }
    
    @Test
    @DisplayName("POST /stores/{id}/activate should activate store when authenticated")
    @WithMockUser(username = "testuser")
    void testActivateStore() throws Exception {
        UUID storeId = UUID.randomUUID();
        
        mockMvc.perform(post("/stores/" + storeId + "/activate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Store activated successfully"));
        
        System.out.println("✅ Test passed: POST /stores/{id}/activate activates store");
    }
    
    @Test
    @DisplayName("GET /stores/current should return current store")
    @WithMockUser(username = "testuser")
    void testGetCurrentStore() throws Exception {
        mockMvc.perform(get("/stores/current"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Current store retrieved successfully"));
        
        System.out.println("✅ Test passed: GET /stores/current returns current store");
    }
    
    @Test
    @DisplayName("POST /stores should require authentication")
    void testCreateStoreRequiresAuth() throws Exception {
        mockMvc.perform(post("/stores")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(storeRequest)))
                .andExpect(status().isUnauthorized());
        
        System.out.println("✅ Test passed: POST /stores requires authentication");
    }
}