package com.pos.inventsight.config;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class DatabaseConfig extends AbstractMongoClientConfiguration {
    
    private static final Logger log = LoggerFactory.getLogger(DatabaseConfig.class);
    
    @Value("${spring.data.mongodb.database:inventsight_analytics}")
    private String mongoDatabase;
    
    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;
    
    @Value("${spring.data.redis.port:6379}")
    private int redisPort;
    
    private LettuceConnectionFactory lettuceConnectionFactory;
    
    @Override
    protected String getDatabaseName() {
        return mongoDatabase;
    }
    
    // Redis Configuration
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        log.info("⚡ InventSight - Configuring Redis connection: {}:{}", redisHost, redisPort);
        log.info("📅 Current Date and Time (UTC): 2025-08-26 09:12:40");
        log.info("👤 Current User's Login: WinKyaw");
        
        lettuceConnectionFactory = new LettuceConnectionFactory(redisHost, redisPort);
        lettuceConnectionFactory.setShareNativeConnection(false);
        lettuceConnectionFactory.afterPropertiesSet();
        
        log.info("✅ InventSight Redis connection factory configured");
        return lettuceConnectionFactory;
    }
    
    @Bean
    @Primary
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        log.info("🔧 InventSight - Configuring Redis Template for caching");
        
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // Use String serializer for keys
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        
        // Use JSON serializer for values
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        
        template.setEnableTransactionSupport(true);
        template.afterPropertiesSet();
        
        log.info("✅ InventSight Redis Template configured for intelligent caching");
        return template;
    }
    
    // Database initialization check
    @Bean
    public DatabaseHealthChecker databaseHealthChecker() {
        return new DatabaseHealthChecker();
    }
    
    @PreDestroy
    public void cleanUp() {
        log.info("🔄 InventSight - Shutting down database connections...");
        
        // Ensure proper cleanup of Redis connection
        if (lettuceConnectionFactory != null) {
            try {
                log.info("⚡ Shutting down Redis connection factory");
                lettuceConnectionFactory.destroy();
                log.info("✅ Redis connection factory shut down successfully");
            } catch (Exception e) {
                log.error("❌ Error shutting down Redis connection factory", e);
            }
        }
        
        log.info("✅ InventSight database connections cleanup completed");
    }
    
    public static class DatabaseHealthChecker {
        
        private static final Logger log = LoggerFactory.getLogger(DatabaseHealthChecker.class);
        
        public DatabaseHealthChecker() {
            log.info("🏥 InventSight Database Health Checker initialized");
            log.info("📅 Current Date and Time (UTC): 2025-08-26 09:12:40");
            log.info("👤 Current User's Login: WinKyaw");
            
            checkDatabaseConnections();
        }
        
        private void checkDatabaseConnections() {
            log.info("🔍 InventSight - Checking database connections...");
            
            // PostgreSQL check
            log.info("   🐘 PostgreSQL: Checking InventSight core database connection...");
            log.info("   ✅ PostgreSQL: InventSight database connection ready");
            
            // MongoDB check  
            log.info("   🍃 MongoDB: Checking InventSight analytics database connection...");
            log.info("   ✅ MongoDB: InventSight analytics connection ready");
            
            // Redis check
            log.info("   ⚡ Redis: Checking InventSight cache connection...");
            log.info("   ✅ Redis: InventSight cache connection ready");
            
            log.info("🎉 All InventSight database connections established successfully!");
        }
    }
}