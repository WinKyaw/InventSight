package com.pos.inventsight.tenant;

import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * SchemaBasedMultiTenantConnectionProvider implements MultiTenantConnectionProvider
 * to switch PostgreSQL schema using SET search_path for each tenant.
 * Only active when multi-tenancy is enabled.
 */
@Component
@ConditionalOnProperty(name = "spring.jpa.properties.hibernate.multiTenancy", havingValue = "SCHEMA")
public class SchemaBasedMultiTenantConnectionProvider implements MultiTenantConnectionProvider<String> {
    
    private static final Logger logger = LoggerFactory.getLogger(SchemaBasedMultiTenantConnectionProvider.class);
    
    private final DataSource dataSource;
    
    public SchemaBasedMultiTenantConnectionProvider(@Autowired DataSource dataSource) {
        this.dataSource = dataSource;
    }
    
    /**
     * Get connection for a specific tenant (schema)
     * @param tenantId the tenant identifier (schema name)
     * @return database connection with search_path set to the tenant schema
     * @throws SQLException if connection cannot be obtained or schema cannot be set
     */
    @Override
    public Connection getConnection(String tenantId) throws SQLException {
        logger.debug("Getting connection for tenant: {}", tenantId);
        
        Connection connection = dataSource.getConnection();
        
        // Set the search_path to the tenant schema
        // This makes PostgreSQL look for tables in the specified schema first
        try {
            // Validate tenant name to prevent SQL injection
            if (isValidSchemaName(tenantId)) {
                // For company schemas (company_*), set search_path WITHOUT public fallback
                // For other schemas, include public for backward compatibility
                String sql;
                if (tenantId.startsWith("company_")) {
                    sql = "SET search_path TO " + tenantId;
                    logger.debug("Setting company schema search_path (no public): {}", sql);
                } else {
                    sql = "SET search_path TO " + tenantId + ", public";
                    logger.debug("Setting schema search_path with public fallback: {}", sql);
                }
                connection.createStatement().execute(sql);
            } else {
                logger.warn("Invalid schema name: {}, using default", tenantId);
                connection.createStatement().execute("SET search_path TO public");
            }
        } catch (SQLException e) {
            logger.error("Error setting search_path for tenant {}: {}", tenantId, e.getMessage());
            // Don't fail - let it use the default schema
            connection.createStatement().execute("SET search_path TO public");
        }
        
        return connection;
    }
    
    /**
     * Get any available connection (uses default tenant)
     * @return database connection with default search_path
     * @throws SQLException if connection cannot be obtained
     */
    @Override
    public Connection getAnyConnection() throws SQLException {
        logger.debug("Getting any connection (default tenant)");
        return getConnection(TenantContext.DEFAULT_TENANT);
    }
    
    /**
     * Release a tenant-specific connection
     * @param tenantId the tenant identifier
     * @param connection the connection to release
     * @throws SQLException if connection cannot be released
     */
    @Override
    public void releaseConnection(String tenantId, Connection connection) throws SQLException {
        logger.debug("Releasing connection for tenant: {}", tenantId);
        
        // Reset search_path to default before releasing
        try {
            connection.createStatement().execute("SET search_path TO public");
        } catch (SQLException e) {
            logger.warn("Error resetting search_path on connection release: {}", e.getMessage());
        }
        
        connection.close();
    }
    
    /**
     * Release any connection
     * @param connection the connection to release
     * @throws SQLException if connection cannot be released
     */
    @Override
    public void releaseAnyConnection(Connection connection) throws SQLException {
        logger.debug("Releasing any connection");
        releaseConnection(TenantContext.DEFAULT_TENANT, connection);
    }
    
    /**
     * Check if connection provider supports aggressive release
     * @return true if aggressive release is supported
     */
    @Override
    public boolean supportsAggressiveRelease() {
        return false;
    }
    
    /**
     * Check if this provider is unwrappable
     * @param unwrapType the type to unwrap to
     * @return true if unwrappable to the specified type
     */
    @Override
    public boolean isUnwrappableAs(Class<?> unwrapType) {
        return MultiTenantConnectionProvider.class.isAssignableFrom(unwrapType) ||
               DataSource.class.isAssignableFrom(unwrapType);
    }
    
    /**
     * Unwrap the provider to the specified type
     * @param unwrapType the type to unwrap to
     * @return the unwrapped object
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> T unwrap(Class<T> unwrapType) {
        if (MultiTenantConnectionProvider.class.isAssignableFrom(unwrapType)) {
            return (T) this;
        } else if (DataSource.class.isAssignableFrom(unwrapType)) {
            return (T) dataSource;
        } else {
            throw new IllegalArgumentException("Cannot unwrap to type: " + unwrapType);
        }
    }
    
    /**
     * Validate schema name to prevent SQL injection
     * @param schemaName the schema name to validate
     * @return true if valid, false otherwise
     */
    private boolean isValidSchemaName(String schemaName) {
        if (schemaName == null || schemaName.trim().isEmpty()) {
            return false;
        }
        
        // Allow alphanumeric characters, underscores, and dashes
        // PostgreSQL schema names can contain these characters
        return schemaName.matches("^[a-zA-Z0-9_-]+$") && schemaName.length() <= 63;
    }
}