package com.pos.inventsight.service;

import com.pos.inventsight.model.sql.User;
import com.pos.inventsight.repository.sql.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Custom OAuth2 User Service that maps OAuth2 users to local application users.
 * Maps verified email addresses to existing local users.
 * Does not automatically grant tenant access - membership must exist.
 */
@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {
    
    private static final Logger logger = LoggerFactory.getLogger(CustomOAuth2UserService.class);
    
    private final UserRepository userRepository;
    
    public CustomOAuth2UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    
    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        // Load OAuth2 user from provider
        OAuth2User oauth2User = super.loadUser(userRequest);
        
        try {
            // Extract email and name from OAuth2 attributes
            String email = extractEmail(oauth2User);
            String name = extractName(oauth2User);
            
            if (email == null || email.isEmpty()) {
                logger.error("No email found in OAuth2 user attributes");
                throw new OAuth2AuthenticationException("Email not provided by OAuth2 provider");
            }
            
            logger.info("OAuth2 login attempt for email: {}", email);
            
            // Find local user by email
            User localUser = userRepository.findByEmail(email).orElse(null);
            
            if (localUser == null) {
                logger.warn("No local user found for OAuth2 email: {}", email);
                throw new OAuth2AuthenticationException("No local user account found for email: " + email);
            }
            
            if (!localUser.isEnabled()) {
                logger.warn("Local user account disabled for OAuth2 email: {}", email);
                throw new OAuth2AuthenticationException("User account is disabled");
            }
            
            logger.info("Successfully mapped OAuth2 user to local user: {}", localUser.getUsername());
            
            // Return custom OAuth2User that wraps our User entity
            return new CustomOAuth2User(localUser, oauth2User.getAttributes());
            
        } catch (OAuth2AuthenticationException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error processing OAuth2 user: {}", e.getMessage(), e);
            throw new OAuth2AuthenticationException("Failed to process OAuth2 user");
        }
    }
    
    /**
     * Extract email from OAuth2 user attributes
     * Tries common attribute names used by different providers
     */
    private String extractEmail(OAuth2User oauth2User) {
        Map<String, Object> attributes = oauth2User.getAttributes();
        
        // Try standard "email" attribute
        Object email = attributes.get("email");
        if (email != null) {
            return email.toString();
        }
        
        // Try "preferred_username" (sometimes used by enterprise IdPs)
        Object preferredUsername = attributes.get("preferred_username");
        if (preferredUsername != null && preferredUsername.toString().contains("@")) {
            return preferredUsername.toString();
        }
        
        return null;
    }
    
    /**
     * Extract name from OAuth2 user attributes
     */
    private String extractName(OAuth2User oauth2User) {
        Map<String, Object> attributes = oauth2User.getAttributes();
        
        // Try "name" attribute first
        Object name = attributes.get("name");
        if (name != null) {
            return name.toString();
        }
        
        // Try combining given_name and family_name
        Object givenName = attributes.get("given_name");
        Object familyName = attributes.get("family_name");
        if (givenName != null && familyName != null) {
            return givenName.toString() + " " + familyName.toString();
        }
        
        return null;
    }
}
