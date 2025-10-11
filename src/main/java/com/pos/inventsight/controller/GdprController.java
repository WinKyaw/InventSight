package com.pos.inventsight.controller;

import com.pos.inventsight.dto.ApiResponse;
import com.pos.inventsight.model.sql.User;
import com.pos.inventsight.service.GdprService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for GDPR data subject rights
 * Implements Article 15 (right to access) and Article 17 (right to erasure)
 */
@RestController
@RequestMapping("/gdpr")
@CrossOrigin(origins = "*", maxAge = 3600)
public class GdprController {
    
    private static final Logger logger = LoggerFactory.getLogger(GdprController.class);
    
    @Autowired
    private GdprService gdprService;
    
    /**
     * Export user data (GDPR Article 15 - Right to Access)
     * GET /gdpr/export
     * Returns ZIP archive with user data in JSON format
     */
    @GetMapping("/export")
    public ResponseEntity<?> exportUserData(Authentication authentication) {
        try {
            // Verify authentication
            if (authentication == null || !(authentication.getPrincipal() instanceof User)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse(false, "Authentication required"));
            }
            
            User user = (User) authentication.getPrincipal();
            logger.info("GDPR data export requested by user: {}", user.getUsername());
            
            // Generate export archive
            byte[] exportData = gdprService.exportUserData(user);
            
            // Return as downloadable file
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", 
                String.format("gdpr_export_%s.zip", user.getUsername()));
            headers.setContentLength(exportData.length);
            
            return new ResponseEntity<>(exportData, headers, HttpStatus.OK);
            
        } catch (Exception e) {
            logger.error("Failed to export user data: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "Failed to export user data: " + e.getMessage()));
        }
    }
    
    /**
     * Delete user data (GDPR Article 17 - Right to Erasure)
     * DELETE /gdpr/delete
     * Anonymizes PII while maintaining audit trail integrity
     */
    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteUserData(
            @RequestParam(defaultValue = "false") boolean hardDelete,
            Authentication authentication) {
        try {
            // Verify authentication
            if (authentication == null || !(authentication.getPrincipal() instanceof User)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse(false, "Authentication required"));
            }
            
            User user = (User) authentication.getPrincipal();
            logger.info("GDPR data deletion requested by user: {} (hard delete: {})", 
                       user.getUsername(), hardDelete);
            
            // Perform deletion/anonymization
            gdprService.deleteUserData(user, hardDelete);
            
            return ResponseEntity.ok(new ApiResponse(true, 
                hardDelete ? "User data deleted successfully" : 
                            "User data anonymized successfully. Audit trail maintained."));
            
        } catch (Exception e) {
            logger.error("Failed to delete user data: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "Failed to delete user data: " + e.getMessage()));
        }
    }
}
