package com.pos.inventsight.controller;

import com.pos.inventsight.dto.ApiResponse;
import com.pos.inventsight.dto.DashboardSummaryResponse;
import com.pos.inventsight.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "*", maxAge = 3600)
public class DashboardController {
    
    @Autowired
    private DashboardService dashboardService;
    
    // GET /api/dashboard/summary - Comprehensive dashboard data
    @GetMapping("/summary")
    public ResponseEntity<?> getDashboardSummary(Authentication authentication) {
        try {
            String username = authentication.getName();
            System.out.println("📊 InventSight - Fetching dashboard summary for user: " + username);
            System.out.println("📅 Current DateTime (UTC): " + LocalDateTime.now());
            System.out.println("👤 Current User: WinKyaw");
            
            DashboardSummaryResponse summary = dashboardService.getDashboardSummary();
            
            Map<String, Object> response = new HashMap<>();
            response.put("summary", summary);
            response.put("message", "Dashboard summary retrieved successfully");
            response.put("timestamp", LocalDateTime.now());
            response.put("system", "InventSight");
            
            System.out.println("✅ InventSight - Dashboard summary retrieved successfully");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.out.println("❌ InventSight - Error fetching dashboard summary: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "Failed to fetch dashboard summary: " + e.getMessage()));
        }
    }
    
    // GET /api/dashboard/kpis - Key Performance Indicators
    @GetMapping("/kpis")
    public ResponseEntity<?> getDashboardKPIs(Authentication authentication) {
        try {
            String username = authentication.getName();
            System.out.println("📈 InventSight - Fetching KPIs for user: " + username);
            System.out.println("📅 Current DateTime (UTC): " + LocalDateTime.now());
            System.out.println("👤 Current User: WinKyaw");
            
            Map<String, Object> kpis = dashboardService.getKPIs();
            
            Map<String, Object> response = new HashMap<>();
            response.put("kpis", kpis);
            response.put("message", "KPIs retrieved successfully");
            response.put("timestamp", LocalDateTime.now());
            response.put("system", "InventSight");
            
            System.out.println("✅ InventSight - KPIs retrieved successfully");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.out.println("❌ InventSight - Error fetching KPIs: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "Failed to fetch KPIs: " + e.getMessage()));
        }
    }
}