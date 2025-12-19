package com.pos.inventsight.service;

import com.pos.inventsight.model.sql.Sale;
import com.pos.inventsight.model.sql.SaleItem;
import com.pos.inventsight.model.sql.SaleStatus;
import com.pos.inventsight.model.sql.User;
import com.pos.inventsight.model.sql.Product;
import com.pos.inventsight.model.sql.Store;
import com.pos.inventsight.repository.sql.SaleRepository;
import com.pos.inventsight.repository.sql.SaleItemRepository;
import com.pos.inventsight.dto.SaleRequest;
import com.pos.inventsight.exception.ResourceNotFoundException;
import com.pos.inventsight.exception.InsufficientStockException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.ArrayList;
import java.util.UUID;

@Service
@Transactional
public class SaleService {
    
    @Autowired
    private SaleRepository saleRepository;
    
    @Autowired
    private SaleItemRepository saleItemRepository;
    
    @Autowired
    private ProductService productService;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private ActivityLogService activityLogService;
    
    @Autowired
    private InventoryAnalyticsService inventoryAnalyticsService;
    
    @Autowired
    private UserActiveStoreService userActiveStoreService;
    
    private static final BigDecimal TAX_RATE = new BigDecimal("0.08"); // 8% tax rate
    
    // Sale Processing
    public Sale processSale(SaleRequest request, UUID userId) {
        System.out.println("🧾 Processing new sale for user: " + userId);
        System.out.println("📅 Sale time: 2025-08-26 08:47:36");
        System.out.println("👤 Processed by: WinKyaw");
        
        User user = userService.getUserById(userId);
        
        // ✅ GET USER'S ACTIVE STORE
        Store activeStore = userActiveStoreService.getUserActiveStoreOrThrow(userId);
        System.out.println("🏪 Active store: " + activeStore.getStoreName() + " (ID: " + activeStore.getId() + ")");
        
        // Create sale
        Sale sale = new Sale();
        sale.setProcessedBy(user);
        sale.setStore(activeStore);  // ✅ SET STORE FROM USER'S ACTIVE STORE
        sale.setCustomerName(request.getCustomerName());
        sale.setCustomerEmail(request.getCustomerEmail());
        sale.setCustomerPhone(request.getCustomerPhone());
        sale.setPaymentMethod(request.getPaymentMethod());
        sale.setNotes(request.getNotes());
        sale.setStatus(SaleStatus.PENDING);
        
        // Calculate totals
        BigDecimal subtotal = BigDecimal.ZERO;
        List<SaleItem> saleItems = new ArrayList<>();
        
        // Process each item
        for (SaleRequest.ItemRequest itemRequest : request.getItems()) {
            Product product = productService.getProductById(itemRequest.getProductId());
            
            // Check stock availability
            if (product.getQuantity() < itemRequest.getQuantity()) {
                throw new InsufficientStockException(
                    "Insufficient stock for " + product.getName() + 
                    ". Available: " + product.getQuantity() + ", Requested: " + itemRequest.getQuantity()
                );
            }
            
            // Create sale item
            SaleItem saleItem = new SaleItem(
                sale, 
                product, 
                itemRequest.getQuantity(), 
                product.getPrice()
            );
            
            saleItems.add(saleItem);
            subtotal = subtotal.add(saleItem.getTotalPrice());
        }
        
        // Apply discount if provided
        BigDecimal discountAmount = request.getDiscountAmount() != null ? 
            request.getDiscountAmount() : BigDecimal.ZERO;
        
        // Calculate tax and total
        BigDecimal taxableAmount = subtotal.subtract(discountAmount);
        BigDecimal taxAmount = taxableAmount.multiply(TAX_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalAmount = taxableAmount.add(taxAmount);
        
        sale.setSubtotal(subtotal);
        sale.setDiscountAmount(discountAmount);
        sale.setTaxAmount(taxAmount);
        sale.setTotalAmount(totalAmount);
        
        // Save sale
        Sale savedSale = saleRepository.save(sale);
        
        // Save sale items and update inventory
        for (SaleItem saleItem : saleItems) {
            saleItem.setSale(savedSale);
            saleItemRepository.save(saleItem);
            
            // Reduce inventory
            productService.reduceStock(
                saleItem.getProduct().getId(), 
                saleItem.getQuantity(),
                "SALE - Receipt: " + savedSale.getReceiptNumber()
            );
        }
        
        // Complete sale
        savedSale.setStatus(SaleStatus.COMPLETED);
        savedSale = saleRepository.save(savedSale);
        
        // Log activity
        activityLogService.logActivity(
            userId.toString(), 
            "WinKyaw", 
            "SALE_COMPLETED", 
            "SALE", 
            String.format("Sale completed: %s - Total: $%.2f", 
                savedSale.getReceiptNumber(), savedSale.getTotalAmount())
        );
        
        // Update analytics
        inventoryAnalyticsService.updateDailyAnalytics(savedSale);
        
        System.out.println("✅ Sale completed: " + savedSale.getReceiptNumber() + 
                         " - Total: $" + savedSale.getTotalAmount());
        
        return savedSale;
    }
    
    // CRUD Operations
    public Sale getSaleById(Long saleId) {
        return saleRepository.findById(saleId)
                .orElseThrow(() -> new ResourceNotFoundException("Sale not found with ID: " + saleId));
    }
    
    public Sale getSaleByReceiptNumber(String receiptNumber) {
        return saleRepository.findByReceiptNumber(receiptNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Sale not found with receipt number: " + receiptNumber));
    }
    
    public List<Sale> getAllSales() {
        return saleRepository.findAll();
    }
    
    public Page<Sale> getSalesByUserId(UUID userId, Pageable pageable) {
        return saleRepository.findByUserId(userId, pageable);
    }
    
    public List<Sale> getSalesByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return saleRepository.findByDateRange(startDate, endDate);
    }
    
    public List<Sale> getTodaySales() {
        return saleRepository.findTodaySales();
    }
    
    // Refund Processing
    public Sale processRefund(Long saleId, String reason, UUID processedBy) {
        Sale sale = getSaleById(saleId);
        
        if (!sale.isRefundable()) {
            throw new IllegalStateException("Sale is not refundable");
        }
        
        // Restore inventory
        for (SaleItem item : sale.getItems()) {
            productService.increaseStock(
                item.getProduct().getId(), 
                item.getQuantity(),
                "REFUND - Receipt: " + sale.getReceiptNumber()
            );
        }
        
        // Update sale status
        sale.setStatus(SaleStatus.REFUNDED);
        sale.setUpdatedAt(LocalDateTime.now());
        sale.setNotes(sale.getNotes() + "\nREFUNDED: " + reason);
        
        Sale refundedSale = saleRepository.save(sale);
        
        // Log activity
        User user = userService.getUserById(processedBy);
        activityLogService.logActivity(
            processedBy.toString(), 
            "WinKyaw", 
            "SALE_REFUNDED", 
            "SALE", 
            String.format("Sale refunded: %s - Reason: %s", 
                sale.getReceiptNumber(), reason)
        );
        
        return refundedSale;
    }
    
    // Analytics Methods
    public BigDecimal getTotalRevenueByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        BigDecimal revenue = saleRepository.getTotalRevenueByDateRange(startDate, endDate);
        return revenue != null ? revenue : BigDecimal.ZERO;
    }
    
    public long getSalesCountByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return saleRepository.getSalesCountByDateRange(startDate, endDate);
    }
    
    public BigDecimal getAverageOrderValue(LocalDateTime startDate, LocalDateTime endDate) {
        BigDecimal avg = saleRepository.getAverageOrderValueByDateRange(startDate, endDate);
        return avg != null ? avg : BigDecimal.ZERO;
    }
    
    public BigDecimal getTodayRevenue() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1).minusSeconds(1);
        return getTotalRevenueByDateRange(startOfDay, endOfDay);
    }
    
    public BigDecimal getMonthlyRevenue(int year, int month) {
        BigDecimal revenue = saleRepository.getMonthlyRevenue(year, month);
        return revenue != null ? revenue : BigDecimal.ZERO;
    }
    
    // Dashboard Data
    public SaleSummary getDashboardSummary() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1).minusSeconds(1);
        
        BigDecimal todayRevenue = getTotalRevenueByDateRange(startOfDay, endOfDay);
        long todaySales = getSalesCountByDateRange(startOfDay, endOfDay);
        BigDecimal averageOrderValue = getAverageOrderValue(startOfDay, endOfDay);
        
        return new SaleSummary(todayRevenue, todaySales, averageOrderValue);
    }
    
    // Additional methods for Receipt API
    public Sale createSale(SaleRequest request, UUID userId) {
        return processSale(request, userId);
    }
    
    public Sale updateSale(Long saleId, SaleRequest request) {
        Sale existingSale = getSaleById(saleId);
        
        // Update basic fields
        existingSale.setCustomerName(request.getCustomerName());
        existingSale.setCustomerEmail(request.getCustomerEmail());
        existingSale.setCustomerPhone(request.getCustomerPhone());
        existingSale.setPaymentMethod(request.getPaymentMethod());
        existingSale.setDiscountAmount(request.getDiscountAmount() != null ? 
            request.getDiscountAmount() : BigDecimal.ZERO);
        existingSale.setNotes(request.getNotes());
        existingSale.setUpdatedAt(LocalDateTime.now());
        
        // Clear existing items and add new ones
        if (existingSale.getItems() != null) {
            existingSale.getItems().clear();
        }
        
        // Recalculate with new items
        BigDecimal subtotal = BigDecimal.ZERO;
        List<SaleItem> saleItems = new ArrayList<>();
        
        for (SaleRequest.ItemRequest itemRequest : request.getItems()) {
            Product product = productService.getProductById(itemRequest.getProductId());
            BigDecimal itemTotal = product.getPrice().multiply(new BigDecimal(itemRequest.getQuantity()));
            subtotal = subtotal.add(itemTotal);
            
            SaleItem saleItem = new SaleItem(existingSale, product, itemRequest.getQuantity(), product.getPrice());
            saleItems.add(saleItem);
        }
        
        existingSale.setSubtotal(subtotal);
        existingSale.setTaxAmount(subtotal.multiply(TAX_RATE).setScale(2, RoundingMode.HALF_UP));
        existingSale.calculateTotal();
        
        Sale savedSale = saleRepository.save(existingSale);
        
        // Save sale items
        for (SaleItem item : saleItems) {
            saleItemRepository.save(item);
        }
        
        activityLogService.logActivity(
            savedSale.getProcessedBy().getId().toString(),
            savedSale.getProcessedBy().getUsername(),
            "SALE_UPDATED",
            "Sale",
            "Updated sale: " + savedSale.getReceiptNumber()
        );
        
        return savedSale;
    }
    
    public void deleteSale(Long saleId) {
        Sale sale = getSaleById(saleId);
        
        // For soft delete, change status to CANCELLED
        sale.setStatus(SaleStatus.CANCELLED);
        sale.setUpdatedAt(LocalDateTime.now());
        saleRepository.save(sale);
        
        activityLogService.logActivity(
            sale.getProcessedBy().getId().toString(),
            sale.getProcessedBy().getUsername(),
            "SALE_DELETED",
            "Sale",
            "Deleted sale: " + sale.getReceiptNumber()
        );
    }
    
    public Sale addItemsToSale(Long saleId, List<SaleRequest.ItemRequest> itemRequests) {
        Sale sale = getSaleById(saleId);
        
        BigDecimal additionalSubtotal = BigDecimal.ZERO;
        List<SaleItem> newItems = new ArrayList<>();
        
        for (SaleRequest.ItemRequest itemRequest : itemRequests) {
            Product product = productService.getProductById(itemRequest.getProductId());
            
            // Check stock availability
            if (product.getQuantity() < itemRequest.getQuantity()) {
                throw new InsufficientStockException("Insufficient stock for product: " + product.getName());
            }
            
            BigDecimal itemTotal = product.getPrice().multiply(new BigDecimal(itemRequest.getQuantity()));
            additionalSubtotal = additionalSubtotal.add(itemTotal);
            
            SaleItem saleItem = new SaleItem(sale, product, itemRequest.getQuantity(), product.getPrice());
            newItems.add(saleItem);
            
            // Update product stock
            productService.reduceStock(product.getId(), itemRequest.getQuantity(), 
                "Added to sale: " + sale.getReceiptNumber());
        }
        
        // Update sale totals
        sale.setSubtotal(sale.getSubtotal().add(additionalSubtotal));
        sale.setTaxAmount(sale.getSubtotal().multiply(TAX_RATE).setScale(2, RoundingMode.HALF_UP));
        sale.calculateTotal();
        sale.setUpdatedAt(LocalDateTime.now());
        
        Sale savedSale = saleRepository.save(sale);
        
        // Save new sale items
        for (SaleItem item : newItems) {
            saleItemRepository.save(item);
        }
        
        activityLogService.logActivity(
            sale.getProcessedBy().getId().toString(),
            sale.getProcessedBy().getUsername(),
            "SALE_ITEMS_ADDED",
            "Sale",
            "Added " + itemRequests.size() + " items to sale: " + sale.getReceiptNumber()
        );
        
        return savedSale;
    }
    
    public List<Sale> searchReceipts(UUID userId, LocalDateTime startDate, LocalDateTime endDate,
                                   String customerName, String customerEmail, String receiptNumber, String status) {
        List<Sale> allSales = getAllSales();
        List<Sale> filteredSales = new ArrayList<>();
        
        for (Sale sale : allSales) {
            // Filter by user (unless admin)
            if (userId != null && !sale.getProcessedBy().getId().equals(userId)) {
                continue;
            }
            
            // Filter by date range
            if (startDate != null && sale.getCreatedAt().isBefore(startDate)) {
                continue;
            }
            if (endDate != null && sale.getCreatedAt().isAfter(endDate)) {
                continue;
            }
            
            // Filter by customer name
            if (customerName != null && !customerName.isEmpty() && 
                (sale.getCustomerName() == null || !sale.getCustomerName().toLowerCase().contains(customerName.toLowerCase()))) {
                continue;
            }
            
            // Filter by customer email
            if (customerEmail != null && !customerEmail.isEmpty() && 
                (sale.getCustomerEmail() == null || !sale.getCustomerEmail().toLowerCase().contains(customerEmail.toLowerCase()))) {
                continue;
            }
            
            // Filter by receipt number
            if (receiptNumber != null && !receiptNumber.isEmpty() && 
                !sale.getReceiptNumber().toLowerCase().contains(receiptNumber.toLowerCase())) {
                continue;
            }
            
            // Filter by status
            if (status != null && !status.isEmpty() && 
                !sale.getStatus().name().equalsIgnoreCase(status)) {
                continue;
            }
            
            filteredSales.add(sale);
        }
        
        return filteredSales;
    }
    
    // Inner class for dashboard summary
    public static class SaleSummary {
        private final BigDecimal todayRevenue;
        private final long todaySales;
        private final BigDecimal averageOrderValue;
        
        public SaleSummary(BigDecimal todayRevenue, long todaySales, BigDecimal averageOrderValue) {
            this.todayRevenue = todayRevenue != null ? todayRevenue : BigDecimal.ZERO;
            this.todaySales = todaySales;
            this.averageOrderValue = averageOrderValue != null ? averageOrderValue : BigDecimal.ZERO;
        }
        
        public BigDecimal getTodayRevenue() { return todayRevenue; }
        public long getTodaySales() { return todaySales; }
        public BigDecimal getAverageOrderValue() { return averageOrderValue; }
    }
}