package com.pos.inventsight.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class SalesChartDTO {
    
    private String period;
    private List<DataPoint> dataPoints;
    private Summary summary;
    
    // Constructors
    public SalesChartDTO() {}
    
    public SalesChartDTO(String period, List<DataPoint> dataPoints, Summary summary) {
        this.period = period;
        this.dataPoints = dataPoints;
        this.summary = summary;
    }
    
    // Getters and Setters
    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }
    
    public List<DataPoint> getDataPoints() { return dataPoints; }
    public void setDataPoints(List<DataPoint> dataPoints) { this.dataPoints = dataPoints; }
    
    public Summary getSummary() { return summary; }
    public void setSummary(Summary summary) { this.summary = summary; }
    
    // Inner class for data point
    public static class DataPoint {
        private LocalDate date;
        private BigDecimal revenue;
        private Long orders;
        private String label;
        
        public DataPoint() {}
        
        public DataPoint(LocalDate date, BigDecimal revenue, Long orders, String label) {
            this.date = date;
            this.revenue = revenue;
            this.orders = orders;
            this.label = label;
        }
        
        public LocalDate getDate() { return date; }
        public void setDate(LocalDate date) { this.date = date; }
        
        public BigDecimal getRevenue() { return revenue; }
        public void setRevenue(BigDecimal revenue) { this.revenue = revenue; }
        
        public Long getOrders() { return orders; }
        public void setOrders(Long orders) { this.orders = orders; }
        
        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
    }
    
    // Inner class for summary
    public static class Summary {
        private BigDecimal totalRevenue;
        private Long totalOrders;
        private BigDecimal avgOrderValue;
        private Double growthPercentage;
        
        public Summary() {}
        
        public Summary(BigDecimal totalRevenue, Long totalOrders, BigDecimal avgOrderValue, Double growthPercentage) {
            this.totalRevenue = totalRevenue;
            this.totalOrders = totalOrders;
            this.avgOrderValue = avgOrderValue;
            this.growthPercentage = growthPercentage;
        }
        
        public BigDecimal getTotalRevenue() { return totalRevenue; }
        public void setTotalRevenue(BigDecimal totalRevenue) { this.totalRevenue = totalRevenue; }
        
        public Long getTotalOrders() { return totalOrders; }
        public void setTotalOrders(Long totalOrders) { this.totalOrders = totalOrders; }
        
        public BigDecimal getAvgOrderValue() { return avgOrderValue; }
        public void setAvgOrderValue(BigDecimal avgOrderValue) { this.avgOrderValue = avgOrderValue; }
        
        public Double getGrowthPercentage() { return growthPercentage; }
        public void setGrowthPercentage(Double growthPercentage) { this.growthPercentage = growthPercentage; }
    }
}
