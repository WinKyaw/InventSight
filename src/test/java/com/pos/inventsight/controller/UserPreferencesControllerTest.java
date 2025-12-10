package com.pos.inventsight.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pos.inventsight.dto.UserPreferencesRequest;
import com.pos.inventsight.model.sql.User;
import com.pos.inventsight.model.sql.UserPreferences;
import com.pos.inventsight.service.UserPreferencesService;
import com.pos.inventsight.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserPreferencesController.class)
public class UserPreferencesControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserPreferencesService userPreferencesService;

    @MockBean
    private UserService userService;

    @MockBean
    private com.pos.inventsight.config.AuthEntryPointJwt authEntryPointJwt;

    @MockBean
    private com.pos.inventsight.tenant.CompanyTenantFilter companyTenantFilter;

    @MockBean
    private com.pos.inventsight.config.JwtUtils jwtUtils;

    @Autowired
    private ObjectMapper objectMapper;

    private User testUser;
    private UserPreferences testPreferences;
    private UUID testUserId;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testUser = new User("testuser", "test@example.com", "password", "Test", "User");
        testUser.setId(testUserId);
        
        testPreferences = new UserPreferences(testUserId);
        testPreferences.setId(UUID.randomUUID());
        testPreferences.setPreferredLanguage("en");
        testPreferences.setTheme("light");
    }

    @Test
    @WithMockUser(username = "testuser")
    void getUserPreferences_Success() throws Exception {
        // Given
        when(userService.getUserByUsername("testuser")).thenReturn(testUser);
        when(userPreferencesService.getUserPreferences(testUserId)).thenReturn(testPreferences);

        // When & Then
        mockMvc.perform(get("/api/users/me/preferences"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value(testUserId.toString()))
                .andExpect(jsonPath("$.data.preferredLanguage").value("en"))
                .andExpect(jsonPath("$.data.theme").value("light"));

        verify(userService).getUserByUsername("testuser");
        verify(userPreferencesService).getUserPreferences(testUserId);
    }

    @Test
    @WithMockUser(username = "testuser")
    void updateLanguagePreference_Success() throws Exception {
        // Given
        UserPreferencesRequest request = new UserPreferencesRequest("es");
        testPreferences.setPreferredLanguage("es");
        
        when(userService.getUserByUsername("testuser")).thenReturn(testUser);
        when(userPreferencesService.updateLanguagePreference(testUserId, "es")).thenReturn(testPreferences);

        // When & Then
        mockMvc.perform(put("/api/users/me/preferences/language")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.preferredLanguage").value("es"));

        verify(userService).getUserByUsername("testuser");
        verify(userPreferencesService).updateLanguagePreference(testUserId, "es");
    }

    @Test
    @WithMockUser(username = "testuser")
    void updateFavoriteTabs_Success() throws Exception {
        // Given
        UserPreferencesRequest request = new UserPreferencesRequest();
        request.setTabs(Arrays.asList("dashboard", "inventory", "employees"));
        testPreferences.setFavoriteTabs(Arrays.asList("dashboard", "inventory", "employees"));
        
        when(userService.getUserByUsername("testuser")).thenReturn(testUser);
        when(userPreferencesService.updateFavoriteTabs(eq(testUserId), anyList())).thenReturn(testPreferences);

        // When & Then
        mockMvc.perform(put("/api/users/me/preferences/favorite-tabs")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.favoriteTabs").isArray())
                .andExpect(jsonPath("$.data.favoriteTabs[0]").value("dashboard"));

        verify(userService).getUserByUsername("testuser");
        verify(userPreferencesService).updateFavoriteTabs(eq(testUserId), anyList());
    }

    @Test
    @WithMockUser(username = "testuser")
    void updateTheme_Success() throws Exception {
        // Given
        UserPreferencesRequest request = new UserPreferencesRequest();
        request.setTheme("dark");
        testPreferences.setTheme("dark");
        
        when(userService.getUserByUsername("testuser")).thenReturn(testUser);
        when(userPreferencesService.updateTheme(testUserId, "dark")).thenReturn(testPreferences);

        // When & Then
        mockMvc.perform(put("/api/users/me/preferences/theme")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.theme").value("dark"));

        verify(userService).getUserByUsername("testuser");
        verify(userPreferencesService).updateTheme(testUserId, "dark");
    }
}
