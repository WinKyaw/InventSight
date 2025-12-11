package com.pos.inventsight.service;

import com.pos.inventsight.dto.*;
import com.pos.inventsight.model.sql.*;
import com.pos.inventsight.repository.sql.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests for DashboardService widget methods
 */
@ExtendWith(MockitoExtension.class)
public class DashboardServiceWidgetTest {
    
    @Mock
    private SalesOrderRepository salesOrderRepository;
    
    @Mock
    private SalesOrderItemRepository salesOrderItemRepository;
    
    @Mock
    private ProductRepository productRepository;
    
    @Mock
    private CategoryRepository categoryRepository;
    
    @Mock
    private ProductService productService;
    
    @InjectMocks
    private DashboardService dashboardService;
    
    @BeforeEach
    void setUp() {
        // Common setup can be done here
    }
    
    @Test
    void testGetRevenue_ShouldReturnRevenueWithGrowth() {
        // Given
        BigDecimal currentRevenue = new BigDecimal("15420.50");
        BigDecimal previousRevenue = new BigDecimal("13707.11");
        
        when(salesOrderItemRepository.calculateRevenueForPeriod(any(), any()))
            .thenReturn(currentRevenue)
            .thenReturn(previousRevenue);
        
        // When
        RevenueDTO result = dashboardService.getRevenue("THIS_MONTH");
        
        // Then
        assertNotNull(result);
        assertEquals(currentRevenue, result.getTotalRevenue());
        assertEquals(previousRevenue, result.getPreviousPeriodRevenue());
        assertTrue(result.getGrowthPercentage() > 0);
        assertEquals("THIS_MONTH", result.getPeriod());
        assertEquals("USD", result.getCurrency());
        
        verify(salesOrderItemRepository, times(2)).calculateRevenueForPeriod(any(), any());
    }
    
    @Test
    void testGetRevenue_WithNullRevenue_ShouldReturnZero() {
        // Given
        when(salesOrderItemRepository.calculateRevenueForPeriod(any(), any()))
            .thenReturn(null);
        
        // When
        RevenueDTO result = dashboardService.getRevenue("THIS_MONTH");
        
        // Then
        assertNotNull(result);
        assertEquals(BigDecimal.ZERO, result.getTotalRevenue());
        assertEquals(BigDecimal.ZERO, result.getPreviousPeriodRevenue());
    }
    
    @Test
    void testGetOrders_ShouldReturnOrdersWithGrowth() {
        // Given
        long currentOrders = 342L;
        long todayOrders = 15L;
        long previousOrders = 315L;
        
        when(salesOrderRepository.countByCreatedAtBetween(any(), any()))
            .thenReturn(currentOrders)
            .thenReturn(todayOrders)
            .thenReturn(previousOrders);
        
        // When
        OrdersDTO result = dashboardService.getOrders("THIS_MONTH");
        
        // Then
        assertNotNull(result);
        assertEquals(currentOrders, result.getTotalOrders());
        assertEquals(todayOrders, result.getTodayOrders());
        assertTrue(result.getGrowthPercentage() > 0);
        assertEquals("THIS_MONTH", result.getPeriod());
        
        verify(salesOrderRepository, times(3)).countByCreatedAtBetween(any(), any());
    }
    
    @Test
    void testGetLowStock_ShouldReturnLowStockProducts() {
        // Given
        Product product1 = createMockProduct("Coffee", "CAP126", 5, 10);
        Product product2 = createMockProduct("Sugar", "SUG001", 3, 15);
        
        List<Product> lowStockProducts = Arrays.asList(product1, product2);
        
        when(productRepository.findLowStockProducts()).thenReturn(lowStockProducts);
        
        // When
        LowStockDTO result = dashboardService.getLowStock();
        
        // Then
        assertNotNull(result);
        assertEquals(2L, result.getLowStockCount());
        assertEquals(2, result.getProducts().size());
        assertEquals("Coffee", result.getProducts().get(0).getName());
        assertEquals("CAP126", result.getProducts().get(0).getSku());
        assertEquals(5, result.getProducts().get(0).getCurrentStock());
        assertEquals(10, result.getProducts().get(0).getMinStock());
        
        verify(productRepository).findLowStockProducts();
    }
    
    @Test
    void testGetProductStats_ShouldReturnProductStatistics() {
        // Given
        when(productRepository.countActiveProducts()).thenReturn(145L);
        when(productRepository.countInactiveProducts()).thenReturn(11L);
        
        // When
        ProductStatsDTO result = dashboardService.getProductStats();
        
        // Then
        assertNotNull(result);
        assertEquals(156L, result.getTotalProducts());
        assertEquals(145L, result.getActiveProducts());
        assertEquals(11L, result.getInactiveProducts());
        
        verify(productRepository).countActiveProducts();
        verify(productRepository).countInactiveProducts();
    }
    
    @Test
    void testGetCategoryStats_ShouldReturnCategoryStatistics() {
        // Given
        when(categoryRepository.countActiveCategories()).thenReturn(12L);
        
        List<Object[]> topCategories = new ArrayList<>();
        topCategories.add(new Object[]{"Beverages", 45L});
        topCategories.add(new Object[]{"Snacks", 38L});
        
        when(productRepository.getTopCategoriesByProductCount()).thenReturn(topCategories);
        
        // When
        CategoryStatsDTO result = dashboardService.getCategoryStats();
        
        // Then
        assertNotNull(result);
        assertEquals(12L, result.getTotalCategories());
        assertEquals(2, result.getTopCategories().size());
        assertEquals("Beverages", result.getTopCategories().get(0).getName());
        assertEquals(45L, result.getTopCategories().get(0).getProductCount());
        
        verify(categoryRepository).countActiveCategories();
        verify(productRepository).getTopCategoriesByProductCount();
    }
    
    @Test
    void testGetInventoryValue_ShouldReturnInventoryValue() {
        // Given
        BigDecimal totalValue = new BigDecimal("45230.75");
        when(productRepository.getTotalInventoryValue()).thenReturn(totalValue);
        
        // When
        InventoryValueDTO result = dashboardService.getInventoryValue();
        
        // Then
        assertNotNull(result);
        assertEquals(totalValue, result.getTotalValue());
        assertEquals("USD", result.getCurrency());
        assertNotNull(result.getLastUpdated());
        
        verify(productRepository).getTotalInventoryValue();
    }
    
    @Test
    void testGetAvgOrderValue_ShouldCalculateAverage() {
        // Given
        BigDecimal totalRevenue = new BigDecimal("15430.00");
        long totalOrders = 342L;
        
        when(salesOrderItemRepository.calculateRevenueForPeriod(any(), any()))
            .thenReturn(totalRevenue);
        when(salesOrderRepository.countByCreatedAtBetween(any(), any()))
            .thenReturn(totalOrders);
        
        // When
        AvgOrderValueDTO result = dashboardService.getAvgOrderValue("THIS_MONTH");
        
        // Then
        assertNotNull(result);
        assertEquals(new BigDecimal("45.12"), result.getAvgOrderValue());
        assertEquals("USD", result.getCurrency());
        assertEquals("THIS_MONTH", result.getPeriod());
        assertEquals(totalOrders, result.getTotalOrders());
        
        verify(salesOrderItemRepository).calculateRevenueForPeriod(any(), any());
        verify(salesOrderRepository).countByCreatedAtBetween(any(), any());
    }
    
    @Test
    void testGetSalesChart_ShouldReturnChartData() {
        // Given
        List<Object[]> chartData = new ArrayList<>();
        chartData.add(new Object[]{LocalDate.of(2025, 11, 1), new BigDecimal("12345.67"), 123L});
        chartData.add(new Object[]{LocalDate.of(2025, 12, 1), new BigDecimal("15420.50"), 156L});
        
        when(salesOrderItemRepository.getSalesChartData(any(), any()))
            .thenReturn(chartData);
        
        // When
        SalesChartDTO result = dashboardService.getSalesChart("MONTHLY", null, null);
        
        // Then
        assertNotNull(result);
        assertEquals("MONTHLY", result.getPeriod());
        assertEquals(2, result.getDataPoints().size());
        
        SalesChartDTO.DataPoint firstPoint = result.getDataPoints().get(0);
        assertEquals(LocalDate.of(2025, 11, 1), firstPoint.getDate());
        assertEquals(new BigDecimal("12345.67"), firstPoint.getRevenue());
        assertEquals(123L, firstPoint.getOrders());
        
        assertNotNull(result.getSummary());
        assertTrue(result.getSummary().getTotalRevenue().compareTo(BigDecimal.ZERO) > 0);
        
        verify(salesOrderItemRepository).getSalesChartData(any(), any());
    }
    
    @Test
    void testGetBestPerformer_ShouldReturnTopProduct() {
        // Given
        UUID productId = UUID.randomUUID();
        List<Object[]> performers = new ArrayList<>();
        performers.add(new Object[]{
            productId, 
            "Coffee", 
            "CAP126", 
            "Beverages", 
            450L, 
            new BigDecimal("900.00")
        });
        
        when(salesOrderItemRepository.findBestPerformingProducts(any(), any()))
            .thenReturn(performers);
        
        // When
        BestPerformerDTO result = dashboardService.getBestPerformer("THIS_MONTH");
        
        // Then
        assertNotNull(result);
        assertNotNull(result.getProduct());
        assertEquals(productId, result.getProduct().getId());
        assertEquals("Coffee", result.getProduct().getName());
        assertEquals("CAP126", result.getProduct().getSku());
        assertEquals(450L, result.getProduct().getUnitsSold());
        assertEquals(new BigDecimal("900.00"), result.getProduct().getRevenue());
        assertEquals("Beverages", result.getProduct().getCategory());
        assertEquals("THIS_MONTH", result.getPeriod());
        
        verify(salesOrderItemRepository).findBestPerformingProducts(any(), any());
    }
    
    @Test
    void testGetBestPerformer_WithNoData_ShouldReturnNull() {
        // Given
        when(salesOrderItemRepository.findBestPerformingProducts(any(), any()))
            .thenReturn(Collections.emptyList());
        
        // When
        BestPerformerDTO result = dashboardService.getBestPerformer("THIS_MONTH");
        
        // Then
        assertNotNull(result);
        assertNull(result.getProduct());
        assertEquals("THIS_MONTH", result.getPeriod());
    }
    
    @Test
    void testGetRecentOrders_ShouldReturnOrders() {
        // Given
        SalesOrder order = createMockOrder();
        List<SalesOrder> orders = Collections.singletonList(order);
        
        when(salesOrderRepository.findRecentOrders(any(PageRequest.class)))
            .thenReturn(orders);
        
        // When
        RecentOrdersDTO result = dashboardService.getRecentOrders(5);
        
        // Then
        assertNotNull(result);
        assertEquals(1, result.getOrders().size());
        
        RecentOrdersDTO.OrderInfo orderInfo = result.getOrders().get(0);
        assertNotNull(orderInfo.getId());
        assertNotNull(orderInfo.getOrderNumber());
        assertTrue(orderInfo.getOrderNumber().startsWith("ORD-"));
        assertEquals("FULFILLED", orderInfo.getStatus());
        assertEquals("John Doe", orderInfo.getCustomerName());
        
        verify(salesOrderRepository).findRecentOrders(any(PageRequest.class));
    }
    
    // Helper methods
    
    private Product createMockProduct(String name, String sku, int quantity, int threshold) {
        Product product = new Product();
        product.setId(UUID.randomUUID());
        product.setName(name);
        product.setSku(sku);
        product.setQuantity(quantity);
        product.setLowStockThreshold(threshold);
        return product;
    }
    
    private SalesOrder createMockOrder() {
        SalesOrder order = new SalesOrder();
        order.setId(UUID.randomUUID());
        order.setStatus(OrderStatus.FULFILLED);
        order.setCustomerName("John Doe");
        order.setCreatedAt(LocalDateTime.now());
        
        // Create mock items
        SalesOrderItem item = new SalesOrderItem();
        item.setId(UUID.randomUUID());
        item.setQuantity(2);
        item.setUnitPrice(new BigDecimal("25.00"));
        item.setDiscountPercent(BigDecimal.ZERO);
        
        order.setItems(Collections.singletonList(item));
        
        return order;
    }
}
