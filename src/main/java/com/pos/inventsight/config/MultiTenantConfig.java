package com.pos.inventsight.config;

import com.pos.inventsight.tenant.SchemaBasedMultiTenantConnectionProvider;
import com.pos.inventsight.tenant.TenantIdentifierResolver;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MultiTenantConfig configures Hibernate multi-tenancy beans for schema-based tenancy.
 * Only active when multi-tenancy is enabled (not for H2 simple profile).
 */
@Configuration
@ConditionalOnProperty(name = "spring.jpa.properties.hibernate.multiTenancy", havingValue = "SCHEMA")
public class MultiTenantConfig {
    
    /**
     * Register the multi-tenant connection provider bean
     * @return the connection provider for schema-based multi-tenancy
     */
    @Bean
    @ConditionalOnProperty(name = "spring.jpa.properties.hibernate.multiTenancy", havingValue = "SCHEMA")
    public MultiTenantConnectionProvider<String> multiTenantConnectionProvider(SchemaBasedMultiTenantConnectionProvider connectionProvider) {
        return connectionProvider;
    }
    
    /**
     * Register the tenant identifier resolver bean
     * @return the tenant identifier resolver
     */
    @Bean
    @ConditionalOnProperty(name = "spring.jpa.properties.hibernate.multiTenancy", havingValue = "SCHEMA")
    public CurrentTenantIdentifierResolver<String> currentTenantIdentifierResolver(TenantIdentifierResolver tenantIdentifierResolver) {
        return tenantIdentifierResolver;
    }
}