package com.emenu.features.notification.controller;

import com.emenu.features.notification.dto.request.SendNotificationRequest;
import com.emenu.features.notification.dto.response.NotificationLogResponse;
import com.emenu.features.notification.dto.response.SendNotificationResponse;
import com.emenu.features.notification.service.NotificationService;
import com.emenu.shared.dto.ApiResponse;
import com.emenu.shared.dto.PaginationResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/public/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping("/send")
    public ResponseEntity<ApiResponse<SendNotificationResponse>> sendNotification(
            @RequestHeader("X-API-Key") String apiKey,
            @Valid @RequestBody SendNotificationRequest request) {
        
        log.info("Notification request received - API Key: {}...", apiKey.substring(0, 8));
        SendNotificationResponse response = notificationService.sendNotification(apiKey, request);
        return ResponseEntity.ok(ApiResponse.success("Notification sent successfully", response));
    }

    @GetMapping("/logs")
    public ResponseEntity<ApiResponse<PaginationResponse<NotificationLogResponse>>> getMyLogs(
            @RequestHeader("X-API-Key") String apiKey,
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "15") Integer pageSize) {
        
        PaginationResponse<NotificationLogResponse> response = 
            notificationService.getMyLogs(apiKey, pageNo, pageSize);
        return ResponseEntity.ok(ApiResponse.success("Logs retrieved", response));
    }

    @GetMapping("/logs/batch/{batchId}")
    public ResponseEntity<ApiResponse<PaginationResponse<NotificationLogResponse>>> getBatchLogs(
            @PathVariable String batchId,
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "15") Integer pageSize) {
        
        PaginationResponse<NotificationLogResponse> response = 
            notificationService.getBatchLogs(batchId, pageNo, pageSize);
        return ResponseEntity.ok(ApiResponse.success("Batch logs retrieved", response));
    }

    @GetMapping("/logs/{logId}")
    public ResponseEntity<ApiResponse<NotificationLogResponse>> getLogById(
            @PathVariable UUID logId) {
        
        NotificationLogResponse response = notificationService.getLogById(logId);
        return ResponseEntity.ok(ApiResponse.success("Log retrieved", response));
    }
}