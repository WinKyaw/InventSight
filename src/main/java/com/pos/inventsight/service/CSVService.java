package com.pos.inventsight.service;

import com.pos.inventsight.model.sql.PredefinedItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class CSVService {
    
    private static final Logger logger = LoggerFactory.getLogger(CSVService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    /**
     * Parse CSV file for import
     */
    public List<Map<String, String>> parseImportCSV(MultipartFile file) throws IOException {
        List<Map<String, String>> items = new ArrayList<>();
        
        logger.info("📄 Starting CSV import from file: {}", file.getOriginalFilename());
        logger.info("📏 File size: {} bytes", file.getSize());
        
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            
            String headerLine = reader.readLine();
            if (headerLine == null) {
                throw new IllegalArgumentException("CSV file is empty");
            }
            
            logger.debug("CSV header line: {}", headerLine);
            
            String[] headers = parseCSVLine(headerLine);
            logger.info("CSV headers parsed: {}", Arrays.toString(headers));
            
            validateImportHeaders(headers);
            
            String line;
            int lineNumber = 1;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.trim().isEmpty()) {
                    logger.debug("Skipping empty line {}", lineNumber);
                    continue; // Skip empty lines
                }
                
                try {
                    String[] values = parseCSVLine(line);
                    Map<String, String> item = new HashMap<>();
                    
                    for (int i = 0; i < headers.length && i < values.length; i++) {
                        item.put(headers[i].trim().toLowerCase(), values[i].trim());
                    }
                    
                    logger.debug("Line {} parsed: {}", lineNumber, item);
                    items.add(item);
                } catch (Exception e) {
                    logger.warn("❌ Error parsing line {}: {}", lineNumber, e.getMessage());
                    // Continue processing other lines
                }
            }
        }
        
        logger.info("✅ Parsed {} items from CSV", items.size());
        return items;
    }
    
    /**
     * Generate CSV content for export
     */
    public String generateExportCSV(List<PredefinedItem> items) {
        StringBuilder csv = new StringBuilder();
        
        // Header
        csv.append("name,sku,category,unitType,description,defaultPrice,createdAt,createdBy\n");
        
        // Data rows
        for (PredefinedItem item : items) {
            csv.append(escapeCSVField(item.getName())).append(",");
            csv.append(escapeCSVField(item.getSku())).append(",");
            csv.append(escapeCSVField(item.getCategory())).append(",");
            csv.append(escapeCSVField(item.getUnitType())).append(",");
            csv.append(escapeCSVField(item.getDescription())).append(",");
            csv.append(item.getDefaultPrice() != null ? item.getDefaultPrice().toString() : "").append(",");
            csv.append(item.getCreatedAt() != null ? DATE_FORMATTER.format(item.getCreatedAt()) : "").append(",");
            csv.append(escapeCSVField(item.getCreatedByUser() != null ? item.getCreatedByUser().getEmail() : ""));
            csv.append("\n");
        }
        
        return csv.toString();
    }
    
    /**
     * Validate required fields in a parsed item
     * Case-insensitive validation - accepts both "unitType" and "unittype"
     */
    public boolean validateItem(Map<String, String> item, List<String> errors) {
        boolean valid = true;
        
        // Normalize keys to lowercase for case-insensitive comparison
        Map<String, String> normalizedItem = new HashMap<>();
        item.forEach((key, value) -> normalizedItem.put(key.toLowerCase(), value));
        
        if (!normalizedItem.containsKey("name") || normalizedItem.get("name").isEmpty()) {
            errors.add("Name is required");
            valid = false;
        }
        
        if (!normalizedItem.containsKey("unittype") || normalizedItem.get("unittype").isEmpty()) {
            errors.add("Unit type is required");
            valid = false;
        }
        
        // Validate price if present
        if (normalizedItem.containsKey("defaultprice") && !normalizedItem.get("defaultprice").isEmpty()) {
            try {
                new BigDecimal(normalizedItem.get("defaultprice"));
            } catch (NumberFormatException e) {
                errors.add("Invalid price format: " + normalizedItem.get("defaultprice"));
                valid = false;
            }
        }
        
        return valid;
    }
    
    /**
     * Parse a CSV line handling quoted fields
     */
    private String[] parseCSVLine(String line) {
        List<String> result = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean inQuotes = false;
        
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    // Escaped quote
                    field.append('"');
                    i++; // Skip next quote
                } else {
                    // Toggle quote state
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                // End of field
                result.add(field.toString());
                field = new StringBuilder();
            } else {
                field.append(c);
            }
        }
        
        // Add last field
        result.add(field.toString());
        
        return result.toArray(new String[0]);
    }
    
    /**
     * Escape CSV field (add quotes if needed)
     */
    private String escapeCSVField(String field) {
        if (field == null) {
            return "";
        }
        
        // If field contains comma, quote, or newline, wrap in quotes and escape quotes
        if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
            return "\"" + field.replace("\"", "\"\"") + "\"";
        }
        
        return field;
    }
    
    /**
     * Validate import CSV headers (case-insensitive)
     */
    private void validateImportHeaders(String[] headers) {
        Set<String> requiredHeaders = new HashSet<>(Arrays.asList("name", "unittype"));
        Set<String> headerSet = new HashSet<>();
        
        // Normalize headers to lowercase for case-insensitive comparison
        for (String header : headers) {
            String normalized = header.trim().toLowerCase();
            headerSet.add(normalized);
            logger.debug("CSV header found: '{}' (normalized: '{}')", header, normalized);
        }
        
        logger.info("CSV headers: {}", headerSet);
        
        for (String required : requiredHeaders) {
            if (!headerSet.contains(required)) {
                throw new IllegalArgumentException(
                    "Missing required header: '" + required + "' (case-insensitive). " +
                    "Found headers: " + Arrays.toString(headers) + ". " +
                    "Required headers: name, unitType (accepts both unitType and unittype). " +
                    "Optional headers: sku, category, description, defaultPrice");
            }
        }
        
        logger.info("✅ CSV headers validated successfully");
    }
    
    /**
     * Create import report
     */
    public Map<String, Object> createImportReport(int total, int successful, int failed, List<String> errors) {
        Map<String, Object> report = new HashMap<>();
        report.put("totalRecords", total);
        report.put("successfulImports", successful);
        report.put("failedImports", failed);
        report.put("errors", errors);
        report.put("timestamp", LocalDateTime.now());
        
        return report;
    }
}
