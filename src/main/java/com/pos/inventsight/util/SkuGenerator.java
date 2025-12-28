package com.pos.inventsight.util;

import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;

/**
 * Utility class for generating unique 11-digit SKUs.
 * Uses ThreadLocalRandom for better randomness and thread-safety.
 */
@Component
public class SkuGenerator {
    
    private static final long MIN_SKU = 10000000000L; // 11 digits minimum
    private static final long MAX_SKU = 99999999999L; // 11 digits maximum
    private static final int MAX_ATTEMPTS = 100;
    
    /**
     * Generate a unique 11-digit SKU.
     * 
     * @param existsChecker A predicate that checks if a SKU already exists
     * @return A unique 11-digit SKU as a string
     * @throws IllegalStateException if unable to generate unique SKU after max attempts
     */
    public String generateUniqueSku(Predicate<String> existsChecker) {
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            String sku = generate11DigitSku();
            if (!existsChecker.test(sku)) {
                return sku;
            }
        }
        throw new IllegalStateException("Unable to generate unique SKU after " + MAX_ATTEMPTS + " attempts");
    }
    
    /**
     * Generate a random 11-digit SKU without uniqueness check.
     * 
     * @return An 11-digit SKU as a string
     */
    public String generate11DigitSku() {
        // Generate a random long between MIN_SKU and MAX_SKU (inclusive)
        long randomLong = ThreadLocalRandom.current().nextLong(MIN_SKU, MAX_SKU + 1);
        return String.valueOf(randomLong);
    }
}
