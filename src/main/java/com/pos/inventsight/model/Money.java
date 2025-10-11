package com.pos.inventsight.model;

import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Value object representing money with amount and currency code.
 * Embeddable in JPA entities for multi-currency support.
 */
@Embeddable
public class Money implements Serializable {
    
    @NotNull
    @DecimalMin("0.0")
    private BigDecimal amount;
    
    @NotBlank
    private String currencyCode; // ISO 4217 currency code (USD, EUR, MMK, etc.)
    
    // Default constructor for JPA
    protected Money() {
        this.amount = BigDecimal.ZERO;
        this.currencyCode = "USD";
    }
    
    public Money(BigDecimal amount, String currencyCode) {
        if (amount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }
        if (currencyCode == null || currencyCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Currency code cannot be null or empty");
        }
        this.amount = amount.setScale(2, RoundingMode.HALF_UP);
        this.currencyCode = currencyCode.toUpperCase();
    }
    
    public Money(double amount, String currencyCode) {
        this(BigDecimal.valueOf(amount), currencyCode);
    }
    
    // Factory methods
    public static Money of(BigDecimal amount, String currencyCode) {
        return new Money(amount, currencyCode);
    }
    
    public static Money zero(String currencyCode) {
        return new Money(BigDecimal.ZERO, currencyCode);
    }
    
    public static Money usd(BigDecimal amount) {
        return new Money(amount, "USD");
    }
    
    public static Money mmk(BigDecimal amount) {
        return new Money(amount, "MMK"); // Myanmar Kyat
    }
    
    // Getters
    public BigDecimal getAmount() {
        return amount;
    }
    
    public String getCurrencyCode() {
        return currencyCode;
    }
    
    // Arithmetic operations
    public Money add(Money other) {
        assertSameCurrency(other);
        return new Money(this.amount.add(other.amount), this.currencyCode);
    }
    
    public Money subtract(Money other) {
        assertSameCurrency(other);
        return new Money(this.amount.subtract(other.amount), this.currencyCode);
    }
    
    public Money multiply(BigDecimal multiplier) {
        return new Money(this.amount.multiply(multiplier), this.currencyCode);
    }
    
    public Money divide(BigDecimal divisor) {
        if (divisor.compareTo(BigDecimal.ZERO) == 0) {
            throw new ArithmeticException("Cannot divide by zero");
        }
        return new Money(this.amount.divide(divisor, 2, RoundingMode.HALF_UP), this.currencyCode);
    }
    
    // Comparison operations
    public boolean isGreaterThan(Money other) {
        assertSameCurrency(other);
        return this.amount.compareTo(other.amount) > 0;
    }
    
    public boolean isLessThan(Money other) {
        assertSameCurrency(other);
        return this.amount.compareTo(other.amount) < 0;
    }
    
    public boolean isZero() {
        return this.amount.compareTo(BigDecimal.ZERO) == 0;
    }
    
    public boolean isPositive() {
        return this.amount.compareTo(BigDecimal.ZERO) > 0;
    }
    
    public boolean isNegative() {
        return this.amount.compareTo(BigDecimal.ZERO) < 0;
    }
    
    // Helper methods
    private void assertSameCurrency(Money other) {
        if (!this.currencyCode.equals(other.currencyCode)) {
            throw new IllegalArgumentException(
                String.format("Cannot operate on different currencies: %s and %s", 
                    this.currencyCode, other.currencyCode)
            );
        }
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Money money = (Money) o;
        return Objects.equals(amount, money.amount) && 
               Objects.equals(currencyCode, money.currencyCode);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(amount, currencyCode);
    }
    
    @Override
    public String toString() {
        return String.format("%s %s", currencyCode, amount);
    }
}
