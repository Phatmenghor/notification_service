package com.emenu.features.notification.controller;

import com.emenu.features.notification.dto.request.SystemSendNotificationRequest;
import com.emenu.features.notification.dto.request.UpdateSystemSettingsRequest;
import com.emenu.features.notification.dto.response.SystemSendNotificationResponse;
import com.emenu.features.notification.dto.response.SystemSettingsResponse;
import com.emenu.features.notification.service.SystemNotificationService;
import com.emenu.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/public/system-notifications")
@RequiredArgsConstructor
@Slf4j
public class SystemNotificationController {

    private final SystemNotificationService systemNotificationService;

    // ========== ADMIN ENDPOINTS (Configure Settings) ==========

    @GetMapping("/settings")
    @PreAuthorize("hasRole('PLATFORM_OWNER') or hasRole('PLATFORM_ADMIN')")
    public ResponseEntity<ApiResponse<SystemSettingsResponse>> getSystemSettings() {
        SystemSettingsResponse response = systemNotificationService.getSystemSettings();
        return ResponseEntity.ok(ApiResponse.success("System settings retrieved", response));
    }

    @PutMapping("/settings")
    @PreAuthorize("hasRole('PLATFORM_OWNER') or hasRole('PLATFORM_ADMIN')")
    public ResponseEntity<ApiResponse<SystemSettingsResponse>> updateSystemSettings(
            @Valid @RequestBody UpdateSystemSettingsRequest request) {
        log.info("Updating system notification settings");
        SystemSettingsResponse response = systemNotificationService.updateSystemSettings(request);
        return ResponseEntity.ok(ApiResponse.success("System settings updated", response));
    }

    // ========== API KEY AUTHENTICATED ENDPOINT (Send Notifications) ==========

    @PostMapping("/send")
    public ResponseEntity<ApiResponse<SystemSendNotificationResponse>> sendSystemNotification(
            @RequestHeader("X-API-Key") String apiKey,
            @Valid @RequestBody SystemSendNotificationRequest request) {
        
        log.info("System notification request - API Key: {}..., Channel: {}, Type: {}", 
                 apiKey.substring(0, Math.min(8, apiKey.length())), 
                 request.getChannel(), request.getType());
        
        SystemSendNotificationResponse response = 
            systemNotificationService.sendSystemNotification(apiKey, request);
        
        return ResponseEntity.ok(ApiResponse.success("System notification sent", response));
    }
}