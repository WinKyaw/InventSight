package com.pos.inventsight.service;

import com.pos.inventsight.model.sql.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Map;

/**
 * Custom OAuth2User implementation that wraps our local User entity.
 * Allows OAuth2-authenticated users to be treated as local users throughout the application.
 */
public class CustomOAuth2User implements OAuth2User {
    
    private final User user;
    private final Map<String, Object> attributes;
    
    public CustomOAuth2User(User user, Map<String, Object> attributes) {
        this.user = user;
        this.attributes = attributes;
    }
    
    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }
    
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return user.getAuthorities();
    }
    
    @Override
    public String getName() {
        return user.getUsername();
    }
    
    /**
     * Get the wrapped local user entity
     * @return Local user
     */
    public User getUser() {
        return user;
    }
}
