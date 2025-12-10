package com.pos.inventsight.repository.sql;

import com.pos.inventsight.model.sql.Translation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for Translation entity operations.
 */
@Repository
public interface TranslationRepository extends JpaRepository<Translation, UUID> {
    
    /**
     * Find all translations for a specific language code.
     * 
     * @param languageCode the language code (e.g., "en", "es", "zh")
     * @return list of translations for the language
     */
    List<Translation> findByLanguageCode(String languageCode);
    
    /**
     * Find a translation by key and language code.
     * 
     * @param key the translation key
     * @param languageCode the language code
     * @return optional translation
     */
    Optional<Translation> findByKeyAndLanguageCode(String key, String languageCode);
    
    /**
     * Find all distinct language codes available in the system.
     * 
     * @return list of unique language codes
     */
    @Query("SELECT DISTINCT t.languageCode FROM Translation t ORDER BY t.languageCode")
    List<String> findAllLanguageCodes();
    
    /**
     * Find all translations for a specific category and language.
     * 
     * @param category the category name
     * @param languageCode the language code
     * @return list of translations
     */
    List<Translation> findByCategoryAndLanguageCode(String category, String languageCode);
    
    /**
     * Find all translations for a specific category.
     * 
     * @param category the category name
     * @return list of translations
     */
    List<Translation> findByCategory(String category);
}
