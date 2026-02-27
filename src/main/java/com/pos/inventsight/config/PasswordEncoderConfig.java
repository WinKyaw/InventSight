package com.pos.inventsight.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashMap;
import java.util.Map;

/**
 * Password Encoder Configuration
 *
 * Uses Argon2id - the OWASP #1 recommended password hashing algorithm (2024).
 * Argon2id is memory-hard, resistant to GPU/ASIC and side-channel attacks.
 *
 * OWASP recommended minimum parameters:
 * - Memory: 19MB (19456 KB)
 * - Iterations: 2
 * - Parallelism: 1
 * - Output length: 32 bytes
 * - Salt length: 16 bytes
 *
 * A DelegatingPasswordEncoder is used to maintain backward compatibility with
 * existing BCrypt-encoded passwords while encoding new passwords with Argon2id.
 */
@Configuration
public class PasswordEncoderConfig {

    // OWASP 2024 recommended Argon2id parameters
    private static final int SALT_LENGTH = 16;   // bytes
    private static final int HASH_LENGTH = 32;   // bytes
    private static final int PARALLELISM = 1;    // threads
    private static final int MEMORY = 19456;     // kilobytes (19MB)
    private static final int ITERATIONS = 2;     // passes

    @Bean
    public PasswordEncoder passwordEncoder() {
        Map<String, PasswordEncoder> encoders = new HashMap<>();
        encoders.put("argon2id", new Argon2PasswordEncoder(SALT_LENGTH, HASH_LENGTH, PARALLELISM, MEMORY, ITERATIONS));
        encoders.put("bcrypt", new BCryptPasswordEncoder());

        DelegatingPasswordEncoder delegating = new DelegatingPasswordEncoder("argon2id", encoders);
        // Fall back to BCrypt for existing passwords stored without an {id} prefix
        delegating.setDefaultPasswordEncoderForMatches(new BCryptPasswordEncoder());
        return delegating;
    }
}