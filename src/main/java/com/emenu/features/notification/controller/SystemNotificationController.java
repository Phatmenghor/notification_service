package com.emenu.features.notification.controller;

import com.emenu.enums.notification.NotificationChannel;
import com.emenu.features.notification.dto.request.SystemSendNotificationRequest;
import com.emenu.features.notification.dto.request.UpdateSystemSettingsRequest;
import com.emenu.features.notification.dto.response.SystemSendNotificationResponse;
import com.emenu.features.notification.dto.response.SystemSettingsResponse;
import com.emenu.features.notification.service.SystemNotificationService;
import com.emenu.shared.dto.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/v1/public/system-notifications")
@RequiredArgsConstructor
@Slf4j
public class SystemNotificationController {

    private final SystemNotificationService systemNotificationService;
    private final ObjectMapper objectMapper;

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

    @PostMapping(value = "/send-with-file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<SystemSendNotificationResponse>> sendSystemNotificationWithFile(
            @RequestHeader("X-API-Key") String apiKey,
            @RequestPart("request") String requestJson,
            @RequestPart(value = "file", required = false) MultipartFile htmlFile) {

        try {
            log.info("System notification with file request - API Key: {}...",
                     apiKey.substring(0, Math.min(8, apiKey.length())));

            SystemSendNotificationRequest request = objectMapper.readValue(requestJson, SystemSendNotificationRequest.class);

            // Read HTML file content and set it to the appropriate field
            if (htmlFile != null && !htmlFile.isEmpty()) {
                String htmlContent = new String(htmlFile.getBytes(), StandardCharsets.UTF_8);

                if (request.getChannel() == NotificationChannel.EMAIL) {
                    request.setEmailHtmlBody(htmlContent);
                } else if (request.getChannel() == NotificationChannel.TELEGRAM) {
                    request.setTelegramHtmlBody(htmlContent);
                }
            }

            SystemSendNotificationResponse response =
                systemNotificationService.sendSystemNotification(apiKey, request);

            return ResponseEntity.ok(ApiResponse.success("System notification sent", response));

        } catch (Exception e) {
            log.error("Error processing system notification with file: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to process request: " + e.getMessage()));
        }
    }
}