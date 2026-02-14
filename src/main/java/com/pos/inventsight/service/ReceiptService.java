package com.pos.inventsight.service;

import com.pos.inventsight.dto.ReceiptRequest;
import com.pos.inventsight.dto.ReceiptResponse;
import com.pos.inventsight.exception.ResourceNotFoundException;
import com.pos.inventsight.model.sql.*;
import com.pos.inventsight.repository.sql.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class ReceiptService {
    
    private static final Logger logger = LoggerFactory.getLogger(ReceiptService.class);
    
    @Autowired
    private ReceiptRepository receiptRepository;
    
    @Autowired
    private ReceiptItemRepository receiptItemRepository;
    
    @Autowired
    private SaleReceiptRepository saleReceiptRepository;
    
    @Autowired
    private CustomerRepository customerRepository;
    
    @Autowired
    private StoreRepository storeRepository;
    
    @Autowired
    private ProductService productService;
    
    /**
     * Create a new receipt
     */
    public Receipt createReceipt(ReceiptRequest request, Company company, User createdBy) {
        logger.info("Creating receipt for company: {}", company.getId());
        
        Receipt receipt = new Receipt();
        receipt.setReceiptType(request.getReceiptType() != null ? request.getReceiptType() : ReceiptType.IN_STORE);
        receipt.setStatus(ReceiptStatus.PENDING);
        receipt.setCompany(company);
        receipt.setCreatedBy(createdBy);
        
        // Set store if provided
        if (request.getStoreId() != null) {
            Store store = storeRepository.findById(request.getStoreId())
                .orElseThrow(() -> new ResourceNotFoundException("Store not found"));
            receipt.setStore(store);
        }
        
        // Set customer information
        if (request.getCustomerId() != null) {
            Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
            receipt.setCustomer(customer);
            receipt.setCustomerName(customer.getName());
            receipt.setCustomerEmail(customer.getEmail());
            receipt.setCustomerPhone(customer.getPhoneNumber());
        } else {
            receipt.setCustomerName(request.getCustomerName());
            receipt.setCustomerEmail(request.getCustomerEmail());
            receipt.setCustomerPhone(request.getCustomerPhone());
        }
        
        // Set financial information
        receipt.setSubtotal(request.getSubtotal());
        receipt.setTaxAmount(request.getTaxAmount() != null ? request.getTaxAmount() : BigDecimal.ZERO);
        receipt.setDiscountAmount(request.getDiscountAmount() != null ? request.getDiscountAmount() : BigDecimal.ZERO);
        receipt.setTotalAmount(request.getTotalAmount());
        
        // Set payment information
        receipt.setPaymentMethod(request.getPaymentMethod());
        if (request.getPaymentMethod() != null) {
            receipt.setPaymentStatus(PaymentStatus.PAID);
            receipt.setPaidAmount(request.getTotalAmount());
            receipt.setPaymentDate(LocalDateTime.now());
        } else {
            receipt.setPaymentStatus(PaymentStatus.UNPAID);
        }
        
        // Set delivery type
        receipt.setDeliveryType(request.getDeliveryType() != null ? request.getDeliveryType() : DeliveryType.IN_STORE);
        receipt.setNotes(request.getNotes());
        
        // Save receipt
        receipt = receiptRepository.save(receipt);
        logger.info("Receipt created with number: {}", receipt.getReceiptNumber());
        
        // Create receipt items
        if (request.getItems() != null && !request.getItems().isEmpty()) {
            List<ReceiptItem> items = new ArrayList<>();
            for (ReceiptRequest.ReceiptItemRequest itemRequest : request.getItems()) {
                Product product = productService.getProductById(itemRequest.getProductId());
                
                ReceiptItem item = new ReceiptItem();
                item.setReceipt(receipt);
                item.setProduct(product);
                item.setProductName(product.getName());
                item.setProductSku(product.getSku());
                item.setQuantity(itemRequest.getQuantity());
                item.setUnitPrice(itemRequest.getUnitPrice());
                item.setTotalPrice(itemRequest.getUnitPrice().multiply(new BigDecimal(itemRequest.getQuantity())));
                
                items.add(item);
            }
            receiptItemRepository.saveAll(items);
            logger.info("Created {} receipt items for receipt: {}", items.size(), receipt.getReceiptNumber());
        }
        
        return receipt;
    }
    
    /**
     * Get receipt by ID
     */
    public Receipt getReceiptById(UUID id) {
        return receiptRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Receipt not found with id: " + id));
    }
    
    /**
     * Get receipt by receipt number
     */
    public Receipt getReceiptByNumber(String receiptNumber) {
        return receiptRepository.findByReceiptNumber(receiptNumber)
            .orElseThrow(() -> new ResourceNotFoundException("Receipt not found with number: " + receiptNumber));
    }
    
    /**
     * Complete payment for a receipt
     */
    public Receipt completePayment(UUID receiptId, PaymentMethod paymentMethod) {
        logger.info("Completing payment for receipt: {}", receiptId);
        
        Receipt receipt = getReceiptById(receiptId);
        receipt.setPaymentMethod(paymentMethod);
        receipt.setPaymentStatus(PaymentStatus.PAID);
        receipt.setPaidAmount(receipt.getTotalAmount());
        receipt.setPaymentDate(LocalDateTime.now());
        
        return receiptRepository.save(receipt);
    }
    
    /**
     * Fulfill a receipt
     */
    public Receipt fulfillReceipt(UUID receiptId, User fulfilledBy) {
        logger.info("Fulfilling receipt: {} by user: {}", receiptId, fulfilledBy.getId());
        
        Receipt receipt = getReceiptById(receiptId);
        receipt.setStatus(ReceiptStatus.COMPLETED);
        receipt.setFulfilledBy(fulfilledBy);
        receipt.setFulfilledAt(LocalDateTime.now());
        
        return receiptRepository.save(receipt);
    }
    
    /**
     * Mark receipt as delivered
     */
    public Receipt markAsDelivered(UUID receiptId) {
        logger.info("Marking receipt as delivered: {}", receiptId);
        
        Receipt receipt = getReceiptById(receiptId);
        receipt.setDeliveredAt(LocalDateTime.now());
        
        return receiptRepository.save(receipt);
    }
    
    /**
     * Get all unpaid receipts for a company
     */
    public List<Receipt> getUnpaidReceipts(UUID companyId) {
        return receiptRepository.findUnpaidReceiptsByCompany(companyId);
    }
    
    /**
     * Get all pending receipts for a company
     */
    public List<Receipt> getPendingReceipts(UUID companyId) {
        return receiptRepository.findPendingReceiptsByCompany(companyId);
    }
    
    /**
     * Get receipts by company with pagination
     */
    public Page<Receipt> getReceiptsByCompany(UUID companyId, Pageable pageable) {
        return receiptRepository.findByCompanyId(companyId, pageable);
    }
    
    /**
     * Get receipts by store with pagination
     */
    public Page<Receipt> getReceiptsByStore(UUID storeId, Pageable pageable) {
        return receiptRepository.findByStoreId(storeId, pageable);
    }
    
    /**
     * Convert Receipt entity to ReceiptResponse DTO
     */
    public ReceiptResponse toResponse(Receipt receipt) {
        ReceiptResponse response = new ReceiptResponse();
        response.setId(receipt.getId());
        response.setReceiptNumber(receipt.getReceiptNumber());
        response.setReceiptType(receipt.getReceiptType());
        response.setStatus(receipt.getStatus());
        
        response.setPaymentMethod(receipt.getPaymentMethod());
        response.setPaymentStatus(receipt.getPaymentStatus());
        response.setPaidAmount(receipt.getPaidAmount());
        response.setPaymentDate(receipt.getPaymentDate());
        
        response.setSubtotal(receipt.getSubtotal());
        response.setTaxAmount(receipt.getTaxAmount());
        response.setDiscountAmount(receipt.getDiscountAmount());
        response.setTotalAmount(receipt.getTotalAmount());
        
        response.setCustomerName(receipt.getCustomerName());
        response.setCustomerEmail(receipt.getCustomerEmail());
        response.setCustomerPhone(receipt.getCustomerPhone());
        
        response.setDeliveryType(receipt.getDeliveryType());
        response.setDeliveryNotes(receipt.getDeliveryNotes());
        response.setDeliveredAt(receipt.getDeliveredAt());
        
        response.setFulfilledAt(receipt.getFulfilledAt());
        if (receipt.getFulfilledBy() != null) {
            response.setFulfilledByName(receipt.getFulfilledBy().getUsername());
        }
        
        response.setCreatedAt(receipt.getCreatedAt());
        if (receipt.getCreatedBy() != null) {
            response.setCreatedByName(receipt.getCreatedBy().getUsername());
        }
        
        response.setNotes(receipt.getNotes());
        
        // Load items
        List<ReceiptItem> items = receiptItemRepository.findByReceiptId(receipt.getId());
        response.setItems(items.stream().map(this::toItemResponse).collect(Collectors.toList()));
        
        return response;
    }
    
    /**
     * Convert ReceiptItem entity to ReceiptItemResponse DTO
     */
    private ReceiptResponse.ReceiptItemResponse toItemResponse(ReceiptItem item) {
        ReceiptResponse.ReceiptItemResponse response = new ReceiptResponse.ReceiptItemResponse();
        response.setId(item.getId());
        response.setProductId(item.getProduct().getId());
        response.setProductName(item.getProductName());
        response.setProductSku(item.getProductSku());
        response.setQuantity(item.getQuantity());
        response.setUnitPrice(item.getUnitPrice());
        response.setTotalPrice(item.getTotalPrice());
        return response;
    }
}
