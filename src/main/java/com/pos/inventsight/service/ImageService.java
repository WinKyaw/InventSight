package com.pos.inventsight.service;

import com.sksamuel.scrimage.ImmutableImage;
import com.sksamuel.scrimage.webp.WebpWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class ImageService {
    
    private static final Logger logger = LoggerFactory.getLogger(ImageService.class);
    
    @Value("${app.image.upload-dir:./uploads/images}")
    private String uploadDir;
    
    @Value("${app.image.max-width:800}")
    private int maxWidth;
    
    @Value("${app.image.max-height:800}")
    private int maxHeight;
    
    @Value("${app.image.quality:85}")
    private int quality;
    
    @Value("${spring.servlet.multipart.max-file-size:5MB}")
    private String maxFileSizeStr;
    
    /**
     * Upload and process an image
     * - Validates file size (max 5MB)
     * - Resizes to max dimensions
     * - Converts to WebP format
     * - Stores in upload directory
     * 
     * @param file The uploaded image file
     * @return The relative path to the saved image
     * @throws IOException if upload fails
     */
    public String uploadImage(MultipartFile file) throws IOException {
        // Validate file
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
        
        // Parse max file size from config (e.g., "5MB")
        long maxFileSize = parseSize(maxFileSizeStr);
        
        if (file.getSize() > maxFileSize) {
            throw new IllegalArgumentException("File size exceeds maximum limit of " + maxFileSizeStr);
        }
        
        // Validate file type
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("File must be an image");
        }
        
        // Create upload directory if it doesn't exist
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        
        // Generate unique filename
        String filename = UUID.randomUUID().toString() + ".webp";
        Path filePath = uploadPath.resolve(filename);
        
        try {
            // Load image using scrimage
            ImmutableImage image = ImmutableImage.loader().fromBytes(file.getBytes());
            
            // Resize if needed while maintaining aspect ratio
            if (image.width > maxWidth || image.height > maxHeight) {
                image = image.max(maxWidth, maxHeight);
            }
            
            // Save as WebP with compression
            WebpWriter writer = WebpWriter.DEFAULT.withQ(quality);
            image.output(writer, filePath);
            
            logger.info("Image uploaded successfully: {}", filename);
            
            // Return relative path
            return "/uploads/images/" + filename;
            
        } catch (Exception e) {
            logger.error("Failed to process image: {}", e.getMessage(), e);
            throw new IOException("Failed to process image: " + e.getMessage(), e);
        }
    }
    
    /**
     * Delete an image file
     * 
     * @param imagePath The relative path to the image
     * @return true if deleted successfully
     */
    public boolean deleteImage(String imagePath) {
        if (imagePath == null || imagePath.isEmpty()) {
            return false;
        }
        
        try {
            // Extract filename from path
            String filename = imagePath.substring(imagePath.lastIndexOf('/') + 1);
            Path filePath = Paths.get(uploadDir).resolve(filename);
            
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                logger.info("Image deleted successfully: {}", filename);
                return true;
            }
        } catch (Exception e) {
            logger.error("Failed to delete image: {}", e.getMessage(), e);
        }
        
        return false;
    }
    
    /**
     * Parse size string (e.g., "5MB", "10KB") to bytes
     */
    private long parseSize(String sizeStr) {
        sizeStr = sizeStr.trim().toUpperCase();
        long multiplier = 1;
        
        if (sizeStr.endsWith("KB")) {
            multiplier = 1024;
            sizeStr = sizeStr.substring(0, sizeStr.length() - 2);
        } else if (sizeStr.endsWith("MB")) {
            multiplier = 1024 * 1024;
            sizeStr = sizeStr.substring(0, sizeStr.length() - 2);
        } else if (sizeStr.endsWith("GB")) {
            multiplier = 1024 * 1024 * 1024;
            sizeStr = sizeStr.substring(0, sizeStr.length() - 2);
        }
        
        try {
            return Long.parseLong(sizeStr.trim()) * multiplier;
        } catch (NumberFormatException e) {
            // Default to 5MB if parsing fails
            return 5 * 1024 * 1024;
        }
    }
}
