package com.pos.inventsight.service;

import com.pos.inventsight.exception.DuplicateResourceException;
import com.pos.inventsight.exception.ResourceNotFoundException;
import com.pos.inventsight.model.sql.Translation;
import com.pos.inventsight.repository.sql.TranslationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing translations in the system.
 */
@Service
@Transactional
public class TranslationService {
    
    private static final Logger logger = LoggerFactory.getLogger(TranslationService.class);
    
    @Autowired
    private TranslationRepository translationRepository;
    
    /**
     * Get all available language codes in the system.
     * 
     * @return list of language codes
     */
    public List<String> getAllLanguages() {
        logger.debug("Fetching all available language codes");
        return translationRepository.findAllLanguageCodes();
    }
    
    /**
     * Get all translations for a specific language as key-value pairs.
     * 
     * @param languageCode the language code
     * @return map of translation keys to values
     */
    public Map<String, String> getTranslations(String languageCode) {
        logger.debug("Fetching all translations for language: {}", languageCode);
        List<Translation> translations = translationRepository.findByLanguageCode(languageCode);
        
        return translations.stream()
                .collect(Collectors.toMap(
                        Translation::getKey,
                        Translation::getValue,
                        (existing, replacement) -> replacement
                ));
    }
    
    /**
     * Get translations for a specific category and language.
     * 
     * @param languageCode the language code
     * @param category the category name
     * @return map of translation keys to values
     */
    public Map<String, String> getTranslationsByCategory(String languageCode, String category) {
        logger.debug("Fetching translations for language: {} and category: {}", languageCode, category);
        List<Translation> translations = translationRepository.findByCategoryAndLanguageCode(category, languageCode);
        
        return translations.stream()
                .collect(Collectors.toMap(
                        Translation::getKey,
                        Translation::getValue,
                        (existing, replacement) -> replacement
                ));
    }
    
    /**
     * Get a single translation by ID.
     * 
     * @param id the translation ID
     * @return the translation
     * @throws ResourceNotFoundException if translation not found
     */
    public Translation getTranslationById(UUID id) {
        logger.debug("Fetching translation by ID: {}", id);
        return translationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Translation not found with id: " + id));
    }
    
    /**
     * Create a new translation.
     * 
     * @param translation the translation to create
     * @return the created translation
     * @throws DuplicateResourceException if translation with same key and language already exists
     */
    public Translation createTranslation(Translation translation) {
        logger.debug("Creating translation with key: {} for language: {}", 
                translation.getKey(), translation.getLanguageCode());
        
        // Check for duplicate
        Optional<Translation> existing = translationRepository.findByKeyAndLanguageCode(
                translation.getKey(), translation.getLanguageCode());
        
        if (existing.isPresent()) {
            throw new DuplicateResourceException(
                    "Translation already exists for key '" + translation.getKey() + 
                    "' and language '" + translation.getLanguageCode() + "'");
        }
        
        translation.setCreatedAt(LocalDateTime.now());
        translation.setUpdatedAt(LocalDateTime.now());
        
        return translationRepository.save(translation);
    }
    
    /**
     * Update an existing translation.
     * 
     * @param id the translation ID
     * @param updatedTranslation the updated translation data
     * @return the updated translation
     * @throws ResourceNotFoundException if translation not found
     */
    public Translation updateTranslation(UUID id, Translation updatedTranslation) {
        logger.debug("Updating translation with ID: {}", id);
        
        Translation existing = getTranslationById(id);
        
        // Update fields
        if (updatedTranslation.getValue() != null) {
            existing.setValue(updatedTranslation.getValue());
        }
        if (updatedTranslation.getCategory() != null) {
            existing.setCategory(updatedTranslation.getCategory());
        }
        
        existing.setUpdatedAt(LocalDateTime.now());
        
        return translationRepository.save(existing);
    }
    
    /**
     * Delete a translation.
     * 
     * @param id the translation ID
     * @throws ResourceNotFoundException if translation not found
     */
    public void deleteTranslation(UUID id) {
        logger.debug("Deleting translation with ID: {}", id);
        
        Translation translation = getTranslationById(id);
        translationRepository.delete(translation);
        
        logger.info("Deleted translation with key: {} for language: {}", 
                translation.getKey(), translation.getLanguageCode());
    }
    
    /**
     * Bulk import translations for a language.
     * Creates new translations or updates existing ones.
     * 
     * @param languageCode the language code
     * @param translations map of translation keys to values
     */
    public void bulkImportTranslations(String languageCode, Map<String, String> translations) {
        logger.info("Bulk importing {} translations for language: {}", translations.size(), languageCode);
        
        for (Map.Entry<String, String> entry : translations.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            
            // Determine category from key (e.g., "auth.login" -> "auth")
            String category = key.contains(".") ? key.substring(0, key.indexOf(".")) : "general";
            
            Optional<Translation> existing = translationRepository.findByKeyAndLanguageCode(key, languageCode);
            
            if (existing.isPresent()) {
                // Update existing translation
                Translation translation = existing.get();
                translation.setValue(value);
                translation.setCategory(category);
                translation.setUpdatedAt(LocalDateTime.now());
                translationRepository.save(translation);
            } else {
                // Create new translation
                Translation translation = new Translation(key, languageCode, value, category);
                translationRepository.save(translation);
            }
        }
        
        logger.info("Successfully imported translations for language: {}", languageCode);
    }
}
