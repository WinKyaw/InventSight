package com.pos.inventsight.service;

import com.pos.inventsight.exception.DuplicateResourceException;
import com.pos.inventsight.exception.ResourceNotFoundException;
import com.pos.inventsight.model.sql.Translation;
import com.pos.inventsight.repository.sql.TranslationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TranslationServiceTest {
    
    @Mock
    private TranslationRepository translationRepository;
    
    @InjectMocks
    private TranslationService translationService;
    
    private Translation testTranslation;
    private UUID testId;
    
    @BeforeEach
    void setUp() {
        testId = UUID.randomUUID();
        testTranslation = new Translation("auth.login", "en", "Login", "auth");
        testTranslation.setId(testId);
    }
    
    @Test
    void getAllLanguages_Success() {
        // Given
        List<String> expectedLanguages = Arrays.asList("en", "es", "zh");
        when(translationRepository.findAllLanguageCodes()).thenReturn(expectedLanguages);
        
        // When
        List<String> result = translationService.getAllLanguages();
        
        // Then
        assertNotNull(result);
        assertEquals(3, result.size());
        assertTrue(result.contains("en"));
        assertTrue(result.contains("es"));
        verify(translationRepository).findAllLanguageCodes();
    }
    
    @Test
    void getAllLanguages_IncludesMyanmar() {
        // Given - Include Myanmar in the list of available languages
        List<String> expectedLanguages = Arrays.asList("en", "es", "zh", "ja", "my");
        when(translationRepository.findAllLanguageCodes()).thenReturn(expectedLanguages);
        
        // When
        List<String> result = translationService.getAllLanguages();
        
        // Then
        assertNotNull(result);
        assertEquals(5, result.size());
        assertTrue(result.contains("my"), "Myanmar language code 'my' should be included");
        assertTrue(result.contains("en"));
        assertTrue(result.contains("es"));
        assertTrue(result.contains("zh"));
        assertTrue(result.contains("ja"));
        verify(translationRepository).findAllLanguageCodes();
    }
    
    @Test
    void getTranslations_MyanmarLanguage() {
        // Given - Myanmar translations
        List<Translation> myanmarTranslations = Arrays.asList(
            new Translation("auth.login", "my", "အကောင့်ဝင်ရန်", "auth"),
            new Translation("auth.signup", "my", "အကောင့်ဖွင့်ရန်", "auth"),
            new Translation("tabs.dashboard", "my", "ဒက်ရှ်ဘုတ်", "tabs")
        );
        when(translationRepository.findByLanguageCode("my")).thenReturn(myanmarTranslations);
        
        // When
        Map<String, String> result = translationService.getTranslations("my");
        
        // Then
        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals("အကောင့်ဝင်ရန်", result.get("auth.login"));
        assertEquals("အကောင့်ဖွင့်ရန်", result.get("auth.signup"));
        assertEquals("ဒက်ရှ်ဘုတ်", result.get("tabs.dashboard"));
        verify(translationRepository).findByLanguageCode("my");
    }
    
    @Test
    void getTranslations_Success() {
        // Given
        List<Translation> translations = Arrays.asList(
            new Translation("auth.login", "en", "Login", "auth"),
            new Translation("auth.signup", "en", "Sign Up", "auth")
        );
        when(translationRepository.findByLanguageCode("en")).thenReturn(translations);
        
        // When
        Map<String, String> result = translationService.getTranslations("en");
        
        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Login", result.get("auth.login"));
        assertEquals("Sign Up", result.get("auth.signup"));
        verify(translationRepository).findByLanguageCode("en");
    }
    
    @Test
    void getTranslationsByCategory_Success() {
        // Given
        List<Translation> translations = Arrays.asList(
            new Translation("auth.login", "en", "Login", "auth"),
            new Translation("auth.signup", "en", "Sign Up", "auth")
        );
        when(translationRepository.findByCategoryAndLanguageCode("auth", "en")).thenReturn(translations);
        
        // When
        Map<String, String> result = translationService.getTranslationsByCategory("en", "auth");
        
        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Login", result.get("auth.login"));
        verify(translationRepository).findByCategoryAndLanguageCode("auth", "en");
    }
    
    @Test
    void getTranslationById_Success() {
        // Given
        when(translationRepository.findById(testId)).thenReturn(Optional.of(testTranslation));
        
        // When
        Translation result = translationService.getTranslationById(testId);
        
        // Then
        assertNotNull(result);
        assertEquals("auth.login", result.getKey());
        assertEquals("en", result.getLanguageCode());
        verify(translationRepository).findById(testId);
    }
    
    @Test
    void getTranslationById_NotFound() {
        // Given
        when(translationRepository.findById(testId)).thenReturn(Optional.empty());
        
        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> {
            translationService.getTranslationById(testId);
        });
        verify(translationRepository).findById(testId);
    }
    
    @Test
    void createTranslation_Success() {
        // Given
        when(translationRepository.findByKeyAndLanguageCode("auth.login", "en")).thenReturn(Optional.empty());
        when(translationRepository.save(any(Translation.class))).thenReturn(testTranslation);
        
        // When
        Translation result = translationService.createTranslation(testTranslation);
        
        // Then
        assertNotNull(result);
        assertEquals("auth.login", result.getKey());
        verify(translationRepository).findByKeyAndLanguageCode("auth.login", "en");
        verify(translationRepository).save(any(Translation.class));
    }
    
    @Test
    void createTranslation_DuplicateKey() {
        // Given
        when(translationRepository.findByKeyAndLanguageCode("auth.login", "en"))
            .thenReturn(Optional.of(testTranslation));
        
        // When & Then
        assertThrows(DuplicateResourceException.class, () -> {
            translationService.createTranslation(testTranslation);
        });
        verify(translationRepository).findByKeyAndLanguageCode("auth.login", "en");
        verify(translationRepository, never()).save(any(Translation.class));
    }
    
    @Test
    void updateTranslation_Success() {
        // Given
        Translation updatedData = new Translation();
        updatedData.setValue("Updated Login");
        updatedData.setCategory("auth");
        
        when(translationRepository.findById(testId)).thenReturn(Optional.of(testTranslation));
        when(translationRepository.save(any(Translation.class))).thenReturn(testTranslation);
        
        // When
        Translation result = translationService.updateTranslation(testId, updatedData);
        
        // Then
        assertNotNull(result);
        assertEquals("Updated Login", result.getValue());
        verify(translationRepository).findById(testId);
        verify(translationRepository).save(any(Translation.class));
    }
    
    @Test
    void deleteTranslation_Success() {
        // Given
        when(translationRepository.findById(testId)).thenReturn(Optional.of(testTranslation));
        doNothing().when(translationRepository).delete(any(Translation.class));
        
        // When
        translationService.deleteTranslation(testId);
        
        // Then
        verify(translationRepository).findById(testId);
        verify(translationRepository).delete(testTranslation);
    }
    
    @Test
    void bulkImportTranslations_Success() {
        // Given
        Map<String, String> translations = new HashMap<>();
        translations.put("auth.login", "Login");
        translations.put("auth.signup", "Sign Up");
        
        when(translationRepository.findByKeyAndLanguageCode(anyString(), anyString()))
            .thenReturn(Optional.empty());
        when(translationRepository.save(any(Translation.class))).thenReturn(testTranslation);
        
        // When
        translationService.bulkImportTranslations("en", translations);
        
        // Then
        verify(translationRepository, times(2)).findByKeyAndLanguageCode(anyString(), eq("en"));
        verify(translationRepository, times(2)).save(any(Translation.class));
    }
    
    @Test
    void bulkImportTranslations_UpdateExisting() {
        // Given
        Map<String, String> translations = new HashMap<>();
        translations.put("auth.login", "Updated Login");
        
        when(translationRepository.findByKeyAndLanguageCode("auth.login", "en"))
            .thenReturn(Optional.of(testTranslation));
        when(translationRepository.save(any(Translation.class))).thenReturn(testTranslation);
        
        // When
        translationService.bulkImportTranslations("en", translations);
        
        // Then
        verify(translationRepository).findByKeyAndLanguageCode("auth.login", "en");
        verify(translationRepository).save(any(Translation.class));
        assertEquals("Updated Login", testTranslation.getValue());
    }
}
