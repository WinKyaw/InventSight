package com.pos.inventsight.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pos.inventsight.dto.TranslationRequest;
import com.pos.inventsight.dto.BulkImportRequest;
import com.pos.inventsight.model.sql.Translation;
import com.pos.inventsight.service.TranslationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TranslationController.class)
public class TranslationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TranslationService translationService;

    @MockBean
    private com.pos.inventsight.config.AuthEntryPointJwt authEntryPointJwt;

    @MockBean
    private com.pos.inventsight.tenant.CompanyTenantFilter companyTenantFilter;

    @MockBean
    private com.pos.inventsight.config.JwtUtils jwtUtils;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void getLanguages_Success() throws Exception {
        // Given
        List<String> languages = Arrays.asList("en", "es", "zh");
        when(translationService.getAllLanguages()).thenReturn(languages);

        // When & Then
        mockMvc.perform(get("/api/i18n/languages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.languages").isArray())
                .andExpect(jsonPath("$.languages[0]").value("en"))
                .andExpect(jsonPath("$.defaultLanguage").value("en"));

        verify(translationService).getAllLanguages();
    }

    @Test
    void getTranslations_Success() throws Exception {
        // Given
        Map<String, String> translations = new HashMap<>();
        translations.put("auth.login", "Login");
        translations.put("auth.signup", "Sign Up");
        when(translationService.getTranslations("en")).thenReturn(translations);

        // When & Then
        mockMvc.perform(get("/api/i18n/translations/en"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$['auth.login']").value("Login"))
                .andExpect(jsonPath("$['auth.signup']").value("Sign Up"));

        verify(translationService).getTranslations("en");
    }

    @Test
    void getTranslationsByCategory_Success() throws Exception {
        // Given
        Map<String, String> translations = new HashMap<>();
        translations.put("auth.login", "Login");
        when(translationService.getTranslationsByCategory("en", "auth")).thenReturn(translations);

        // When & Then
        mockMvc.perform(get("/api/i18n/translations/en/category/auth"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$['auth.login']").value("Login"));

        verify(translationService).getTranslationsByCategory("en", "auth");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createTranslation_Success() throws Exception {
        // Given
        TranslationRequest request = new TranslationRequest("auth.login", "en", "Login", "auth");
        Translation translation = new Translation("auth.login", "en", "Login", "auth");
        translation.setId(UUID.randomUUID());
        
        when(translationService.createTranslation(any(Translation.class))).thenReturn(translation);

        // When & Then
        mockMvc.perform(post("/api/i18n/translations")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.key").value("auth.login"));

        verify(translationService).createTranslation(any(Translation.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateTranslation_Success() throws Exception {
        // Given
        UUID id = UUID.randomUUID();
        TranslationRequest request = new TranslationRequest("auth.login", "en", "Updated Login", "auth");
        Translation translation = new Translation("auth.login", "en", "Updated Login", "auth");
        translation.setId(id);
        
        when(translationService.updateTranslation(eq(id), any(Translation.class))).thenReturn(translation);

        // When & Then
        mockMvc.perform(put("/api/i18n/translations/" + id)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.value").value("Updated Login"));

        verify(translationService).updateTranslation(eq(id), any(Translation.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteTranslation_Success() throws Exception {
        // Given
        UUID id = UUID.randomUUID();
        doNothing().when(translationService).deleteTranslation(id);

        // When & Then
        mockMvc.perform(delete("/api/i18n/translations/" + id)
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(translationService).deleteTranslation(id);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void bulkImportTranslations_Success() throws Exception {
        // Given
        Map<String, String> translations = new HashMap<>();
        translations.put("auth.login", "Login");
        translations.put("auth.signup", "Sign Up");
        
        BulkImportRequest request = new BulkImportRequest("en", translations);
        doNothing().when(translationService).bulkImportTranslations(eq("en"), anyMap());

        // When & Then
        mockMvc.perform(post("/api/i18n/translations/bulk-import")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(translationService).bulkImportTranslations(eq("en"), anyMap());
    }
}
