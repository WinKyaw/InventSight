package com.pos.inventsight.controller;

import com.pos.inventsight.service.OcrService;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

@RestController
@RequestMapping("/api/ocr")
public class OcrController {

    private final OcrService ocrService;

    @Autowired
    public OcrController(OcrService ocrService) {
        this.ocrService = ocrService;
    }

    @PostMapping("/myanmar")
    public ResponseEntity<?> extractMyanmarText(@RequestParam("image") MultipartFile file) {
        File tempFile = null;
        try {
            tempFile = File.createTempFile("ocr_", file.getOriginalFilename());
            file.transferTo(tempFile);

            String text = ocrService.extractText(tempFile);

            return ResponseEntity.ok().body(text);

        } catch (IOException | TesseractException e) {
            return ResponseEntity.status(500).body("OCR failed: " + e.getMessage());
        } finally {
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
        }
    }
}