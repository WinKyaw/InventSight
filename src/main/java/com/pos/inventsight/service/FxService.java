package com.pos.inventsight.service;

import com.pos.inventsight.model.Money;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Foreign exchange service for currency conversion.
 * Provides configurable FX rates with caching.
 * 
 * Note: This is a basic implementation. In production, integrate with a real FX provider
 * like ECB, Open Exchange Rates, or similar.
 */
@Service
public class FxService {
    
    private static final Logger logger = LoggerFactory.getLogger(FxService.class);
    
    private final Map<String, BigDecimal> exchangeRates = new ConcurrentHashMap<>();
    
    @Value("${inventsight.currency.default-code:USD}")
    private String defaultCurrency;
    
    @Value("${inventsight.currency.fx.provider:mock}")
    private String fxProvider;
    
    public FxService() {
        // Initialize with mock rates for testing
        // In production, these would be fetched from an external provider
        initializeMockRates();
    }
    
    /**
     * Convert money from one currency to another
     * @param amount Source amount with currency
     * @param targetCurrency Target currency code
     * @return Converted amount in target currency
     * @throws IllegalArgumentException if conversion is not supported
     */
    public Money convert(Money amount, String targetCurrency) {
        if (amount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }
        
        String sourceCurrency = amount.getCurrencyCode();
        
        // No conversion needed if same currency
        if (sourceCurrency.equals(targetCurrency)) {
            return amount;
        }
        
        // Get exchange rate
        BigDecimal rate = getExchangeRate(sourceCurrency, targetCurrency);
        if (rate == null) {
            throw new IllegalArgumentException(
                String.format("Exchange rate not available for %s to %s", sourceCurrency, targetCurrency)
            );
        }
        
        // Convert amount
        BigDecimal convertedAmount = amount.getAmount().multiply(rate);
        
        // Round according to target currency's minor units
        int scale = getCurrencyScale(targetCurrency);
        convertedAmount = convertedAmount.setScale(scale, RoundingMode.HALF_UP);
        
        logger.debug("Converted {} {} to {} {} (rate: {})", 
                    amount.getAmount(), sourceCurrency, convertedAmount, targetCurrency, rate);
        
        return Money.of(convertedAmount, targetCurrency);
    }
    
    /**
     * Add two money amounts, converting if necessary
     * @param a First amount
     * @param b Second amount
     * @return Sum in the currency of first amount
     * @throws IllegalArgumentException if currencies don't match and conversion is not available
     */
    public Money add(Money a, Money b) {
        if (a == null || b == null) {
            throw new IllegalArgumentException("Money amounts cannot be null");
        }
        
        // If same currency, add directly
        if (a.getCurrencyCode().equals(b.getCurrencyCode())) {
            return Money.of(a.getAmount().add(b.getAmount()), a.getCurrencyCode());
        }
        
        // Different currencies - convert b to a's currency
        Money bConverted = convert(b, a.getCurrencyCode());
        return Money.of(a.getAmount().add(bConverted.getAmount()), a.getCurrencyCode());
    }
    
    /**
     * Subtract two money amounts, converting if necessary
     * @param a First amount
     * @param b Second amount to subtract
     * @return Difference in the currency of first amount
     * @throws IllegalArgumentException if currencies don't match and conversion is not available
     */
    public Money subtract(Money a, Money b) {
        if (a == null || b == null) {
            throw new IllegalArgumentException("Money amounts cannot be null");
        }
        
        // If same currency, subtract directly
        if (a.getCurrencyCode().equals(b.getCurrencyCode())) {
            return Money.of(a.getAmount().subtract(b.getAmount()), a.getCurrencyCode());
        }
        
        // Different currencies - convert b to a's currency
        Money bConverted = convert(b, a.getCurrencyCode());
        return Money.of(a.getAmount().subtract(bConverted.getAmount()), a.getCurrencyCode());
    }
    
    /**
     * Get exchange rate from source to target currency
     * @param sourceCurrency Source currency code
     * @param targetCurrency Target currency code
     * @return Exchange rate or null if not available
     */
    private BigDecimal getExchangeRate(String sourceCurrency, String targetCurrency) {
        String key = sourceCurrency + "_" + targetCurrency;
        
        // Check cache first
        BigDecimal rate = exchangeRates.get(key);
        if (rate != null) {
            return rate;
        }
        
        // Try inverse rate
        String inverseKey = targetCurrency + "_" + sourceCurrency;
        BigDecimal inverseRate = exchangeRates.get(inverseKey);
        if (inverseRate != null && inverseRate.compareTo(BigDecimal.ZERO) > 0) {
            rate = BigDecimal.ONE.divide(inverseRate, 6, RoundingMode.HALF_UP);
            exchangeRates.put(key, rate); // Cache for next time
            return rate;
        }
        
        logger.warn("No exchange rate found for {} to {}", sourceCurrency, targetCurrency);
        return null;
    }
    
    /**
     * Get the decimal scale for a currency based on its minor units
     * @param currencyCode Currency code
     * @return Number of decimal places (0 for JPY, 2 for USD, 3 for KWD, etc.)
     */
    private int getCurrencyScale(String currencyCode) {
        try {
            Currency currency = Currency.getInstance(currencyCode);
            return currency.getDefaultFractionDigits();
        } catch (IllegalArgumentException e) {
            logger.warn("Unknown currency code: {}, using default scale 2", currencyCode);
            return 2; // Default to 2 decimal places
        }
    }
    
    /**
     * Initialize mock exchange rates for testing
     * In production, this would fetch from an external provider
     */
    private void initializeMockRates() {
        // Base rates against USD
        exchangeRates.put("USD_EUR", new BigDecimal("0.85"));
        exchangeRates.put("USD_GBP", new BigDecimal("0.73"));
        exchangeRates.put("USD_JPY", new BigDecimal("110.50"));
        exchangeRates.put("USD_MMK", new BigDecimal("2100.00")); // Myanmar Kyat
        exchangeRates.put("USD_KWD", new BigDecimal("0.30"));    // Kuwaiti Dinar
        exchangeRates.put("USD_BHD", new BigDecimal("0.38"));    // Bahraini Dinar
        
        // EUR rates
        exchangeRates.put("EUR_GBP", new BigDecimal("0.86"));
        exchangeRates.put("EUR_JPY", new BigDecimal("130.00"));
        
        logger.info("Initialized mock FX rates for {} currency pairs", exchangeRates.size());
    }
    
    /**
     * Update exchange rate (for testing or manual updates)
     * @param sourceCurrency Source currency
     * @param targetCurrency Target currency
     * @param rate Exchange rate
     */
    public void updateExchangeRate(String sourceCurrency, String targetCurrency, BigDecimal rate) {
        String key = sourceCurrency + "_" + targetCurrency;
        exchangeRates.put(key, rate);
        logger.info("Updated exchange rate: {} to {} = {}", sourceCurrency, targetCurrency, rate);
    }
}
