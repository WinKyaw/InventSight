package com.pos.inventsight.config;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.hibernate6.Hibernate6Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

/**
 * Jackson configuration for Hibernate lazy loading support
 * 
 * Fixes: "No serializer found for class ByteBuddyInterceptor" error
 * when serializing JPA entities with lazy-loaded relationships
 */
@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilder jackson2ObjectMapperBuilder() {
        Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();
        
        // Add Hibernate6 module with proper configuration
        builder.modules(configureHibernate6Module(), new JavaTimeModule());
        builder.featuresToDisable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        builder.featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        return builder;
    }
    
    /**
     * Configure Hibernate6Module to handle lazy-loaded relationships
     * 
     * @return configured Hibernate6Module
     */
    private Hibernate6Module configureHibernate6Module() {
        Hibernate6Module hibernate6Module = new Hibernate6Module();
        
        // Force lazy loading to be ignored (don't try to serialize uninitialized proxies)
        hibernate6Module.configure(Hibernate6Module.Feature.FORCE_LAZY_LOADING, false);
        
        // Serialize only initialized lazy properties
        hibernate6Module.configure(Hibernate6Module.Feature.SERIALIZE_IDENTIFIER_FOR_LAZY_NOT_LOADED_OBJECTS, true);
        
        return hibernate6Module;
    }
}
