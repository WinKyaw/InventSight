package com.pos.inventsight.controller;

import com.pos.inventsight.dto.*;
import com.pos.inventsight.model.sql.Translation;
import com.pos.inventsight.service.TranslationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for managing translations and internationalization.
 * Public endpoints for fetching translations, admin endpoints for management.
 */
@RestController
@RequestMapping("/i18n")
@CrossOrigin(origins = "*", maxAge = 3600)
@Tag(name = "Internationalization", description = "Translation and language management")
public class TranslationController {
    
    @Autowired
    private TranslationService translationService;
    
    /**
     * Get all available languages in the system.
     * Public endpoint accessible without authentication.
     * 
     * @return list of available language codes with default
     */
    @GetMapping("/languages")
    @Operation(summary = "Get available languages", description = "Retrieve all available language codes in the system")
    public ResponseEntity<LanguageResponse> getLanguages() {
        try {
            List<String> languages = translationService.getAllLanguages();
            
            // If no languages exist, return default
            if (languages.isEmpty()) {
                languages = List.of("en");
            }
            
            LanguageResponse response = new LanguageResponse(languages, "en");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new LanguageResponse(List.of("en"), "en"));
        }
    }
    
    /**
     * Get all translations for a specific language.
     * Returns flat key-value pairs. Public endpoint for frontend consumption.
     * 
     * @param languageCode the language code (e.g., "en", "es", "zh")
     * @return map of translation keys to values
     */
    @GetMapping("/translations/{languageCode}")
    @Operation(summary = "Get translations for language", description = "Retrieve all translations for a specific language as key-value pairs")
    public ResponseEntity<Map<String, String>> getTranslations(@PathVariable String languageCode) {
        try {
            Map<String, String> translations = translationService.getTranslations(languageCode);
            
            // Return empty map if no translations found (fallback to keys in frontend)
            return ResponseEntity.ok(translations);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of());
        }
    }
    
    /**
     * Get translations for a specific category and language.
     * Public endpoint for frontend consumption.
     * 
     * @param languageCode the language code
     * @param category the category name (e.g., "auth", "inventory")
     * @return map of translation keys to values
     */
    @GetMapping("/translations/{languageCode}/category/{category}")
    @Operation(summary = "Get translations by category", description = "Retrieve translations for a specific category and language")
    public ResponseEntity<Map<String, String>> getTranslationsByCategory(
            @PathVariable String languageCode,
            @PathVariable String category) {
        try {
            Map<String, String> translations = translationService.getTranslationsByCategory(languageCode, category);
            return ResponseEntity.ok(translations);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of());
        }
    }
    
    /**
     * Create a new translation.
     * Admin only endpoint.
     * 
     * @param request the translation request
     * @return the created translation
     */
    @PostMapping("/translations")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create translation", description = "Create a new translation entry (Admin only)")
    public ResponseEntity<GenericApiResponse<TranslationResponse>> createTranslation(
            @Valid @RequestBody TranslationRequest request) {
        try {
            Translation translation = new Translation(
                    request.getKey(),
                    request.getLanguageCode(),
                    request.getValue(),
                    request.getCategory()
            );
            
            Translation created = translationService.createTranslation(translation);
            TranslationResponse response = new TranslationResponse(created);
            
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new GenericApiResponse<>(true, "Translation created successfully", response));
                    
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new GenericApiResponse<>(false, e.getMessage(), null));
        }
    }
    
    /**
     * Update an existing translation.
     * Admin only endpoint.
     * 
     * @param id the translation ID
     * @param request the updated translation data
     * @return the updated translation
     */
    @PutMapping("/translations/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update translation", description = "Update an existing translation (Admin only)")
    public ResponseEntity<GenericApiResponse<TranslationResponse>> updateTranslation(
            @PathVariable UUID id,
            @Valid @RequestBody TranslationRequest request) {
        try {
            Translation translation = new Translation();
            translation.setValue(request.getValue());
            translation.setCategory(request.getCategory());
            
            Translation updated = translationService.updateTranslation(id, translation);
            TranslationResponse response = new TranslationResponse(updated);
            
            return ResponseEntity.ok(new GenericApiResponse<>(true, "Translation updated successfully", response));
                    
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new GenericApiResponse<>(false, e.getMessage(), null));
        }
    }
    
    /**
     * Delete a translation.
     * Admin only endpoint.
     * 
     * @param id the translation ID
     * @return success response
     */
    @DeleteMapping("/translations/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete translation", description = "Delete a translation (Admin only)")
    public ResponseEntity<GenericApiResponse<Void>> deleteTranslation(@PathVariable UUID id) {
        try {
            translationService.deleteTranslation(id);
            return ResponseEntity.ok(new GenericApiResponse<>(true, "Translation deleted successfully", null));
                    
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new GenericApiResponse<>(false, e.getMessage(), null));
        }
    }
    
    /**
     * Bulk import translations from JSON.
     * Admin only endpoint.
     * 
     * @param request the bulk import request with language code and translations map
     * @return success response
     */
    @PostMapping("/translations/bulk-import")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Bulk import translations", description = "Import multiple translations at once (Admin only)")
    public ResponseEntity<GenericApiResponse<Void>> bulkImportTranslations(
            @Valid @RequestBody BulkImportRequest request) {
        try {
            translationService.bulkImportTranslations(request.getLanguageCode(), request.getTranslations());
            
            String message = String.format("Successfully imported %d translations for language '%s'", 
                    request.getTranslations().size(), request.getLanguageCode());
            
            return ResponseEntity.ok(new GenericApiResponse<>(true, message, null));
                    
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new GenericApiResponse<>(false, e.getMessage(), null));
        }
    }
}
