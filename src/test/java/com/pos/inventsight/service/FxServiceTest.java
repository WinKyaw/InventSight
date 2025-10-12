package com.pos.inventsight.service;

import com.pos.inventsight.model.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for FxService
 */
public class FxServiceTest {
    
    private FxService fxService;
    
    @BeforeEach
    void setUp() {
        fxService = new FxService();
    }
    
    @Test
    void testConvertSameCurrency() {
        Money usd100 = Money.of(new BigDecimal("100.00"), "USD");
        Money result = fxService.convert(usd100, "USD");
        
        assertEquals(usd100.getAmount(), result.getAmount());
        assertEquals(usd100.getCurrencyCode(), result.getCurrencyCode());
    }
    
    @Test
    void testConvertUsdToEur() {
        Money usd100 = Money.of(new BigDecimal("100.00"), "USD");
        Money result = fxService.convert(usd100, "EUR");
        
        assertEquals("EUR", result.getCurrencyCode());
        // EUR should be less than USD (rate is 0.85)
        assertTrue(result.getAmount().compareTo(usd100.getAmount()) < 0);
    }
    
    @Test
    void testConvertUsdToJpy() {
        Money usd100 = Money.of(new BigDecimal("100.00"), "USD");
        Money result = fxService.convert(usd100, "JPY");
        
        assertEquals("JPY", result.getCurrencyCode());
        // JPY should be much more than USD (rate is 110.50)
        assertTrue(result.getAmount().compareTo(usd100.getAmount()) > 0);
        // Note: Money value object sets scale to 2 by default in constructor
        // In production, this would be handled at presentation layer
    }
    
    @Test
    void testConvertUsdToKwd() {
        Money usd100 = Money.of(new BigDecimal("100.00"), "USD");
        Money result = fxService.convert(usd100, "KWD");
        
        assertEquals("KWD", result.getCurrencyCode());
        // KWD should be less than USD
        assertTrue(result.getAmount().compareTo(usd100.getAmount()) < 0);
        // Note: Money value object sets scale to 2 by default in constructor
        // In production, this would be handled at presentation layer
    }
    
    @Test
    void testAddSameCurrency() {
        Money usd50 = Money.of(new BigDecimal("50.00"), "USD");
        Money usd30 = Money.of(new BigDecimal("30.00"), "USD");
        
        Money result = fxService.add(usd50, usd30);
        
        assertEquals("USD", result.getCurrencyCode());
        assertEquals(new BigDecimal("80.00"), result.getAmount());
    }
    
    @Test
    void testAddDifferentCurrencies() {
        Money usd100 = Money.of(new BigDecimal("100.00"), "USD");
        Money eur50 = Money.of(new BigDecimal("50.00"), "EUR");
        
        // Should convert EUR to USD and add
        Money result = fxService.add(usd100, eur50);
        
        assertEquals("USD", result.getCurrencyCode());
        // Result should be more than 100 USD (50 EUR converts to ~58.82 USD at 0.85 rate)
        assertTrue(result.getAmount().compareTo(usd100.getAmount()) > 0);
    }
    
    @Test
    void testSubtractSameCurrency() {
        Money usd100 = Money.of(new BigDecimal("100.00"), "USD");
        Money usd30 = Money.of(new BigDecimal("30.00"), "USD");
        
        Money result = fxService.subtract(usd100, usd30);
        
        assertEquals("USD", result.getCurrencyCode());
        assertEquals(new BigDecimal("70.00"), result.getAmount());
    }
    
    @Test
    void testSubtractDifferentCurrencies() {
        Money usd100 = Money.of(new BigDecimal("100.00"), "USD");
        Money eur50 = Money.of(new BigDecimal("50.00"), "EUR");
        
        // Should convert EUR to USD and subtract
        Money result = fxService.subtract(usd100, eur50);
        
        assertEquals("USD", result.getCurrencyCode());
        // Result should be less than 100 USD
        assertTrue(result.getAmount().compareTo(usd100.getAmount()) < 0);
    }
    
    @Test
    void testConvertInvalidCurrency() {
        Money usd100 = Money.of(new BigDecimal("100.00"), "USD");
        
        // Should throw exception for unknown currency pair
        assertThrows(IllegalArgumentException.class, () -> {
            fxService.convert(usd100, "XYZ");
        });
    }
    
    @Test
    void testUpdateExchangeRate() {
        // Update exchange rate
        fxService.updateExchangeRate("USD", "TEST", new BigDecimal("2.0"));
        
        Money usd100 = Money.of(new BigDecimal("100.00"), "USD");
        Money result = fxService.convert(usd100, "TEST");
        
        assertEquals("TEST", result.getCurrencyCode());
        assertEquals(new BigDecimal("200.00"), result.getAmount());
    }
}
