package com.emenu.features.notification.controller;

import com.emenu.enums.notification.NotificationChannel;
import com.emenu.features.notification.dto.request.SendNotificationRequest;
import com.emenu.features.notification.dto.response.NotificationLogResponse;
import com.emenu.features.notification.dto.response.SendNotificationResponse;
import com.emenu.features.notification.service.NotificationService;
import com.emenu.shared.dto.ApiResponse;
import com.emenu.shared.dto.PaginationResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/public/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @PostMapping("/send")
    public ResponseEntity<ApiResponse<SendNotificationResponse>> sendNotification(
            @RequestHeader("X-API-Key") String apiKey,
            @Valid @RequestBody SendNotificationRequest request) {

        log.info("Notification request received - API Key: {}...", apiKey.substring(0, 8));
        SendNotificationResponse response = notificationService.sendNotification(apiKey, request);
        return ResponseEntity.ok(ApiResponse.success("Notification sent successfully", response));
    }

    @PostMapping(value = "/send-with-file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<SendNotificationResponse>> sendNotificationWithFile(
            @RequestHeader("X-API-Key") String apiKey,
            @RequestPart("request") String requestJson,
            @RequestPart(value = "file", required = false) MultipartFile htmlFile) {

        try {
            log.info("Notification with file request received - API Key: {}...", apiKey.substring(0, 8));

            SendNotificationRequest request = objectMapper.readValue(requestJson, SendNotificationRequest.class);

            // Read HTML file content and set it to the appropriate config
            if (htmlFile != null && !htmlFile.isEmpty()) {
                String htmlContent = new String(htmlFile.getBytes(), StandardCharsets.UTF_8);

                if (request.getChannel() == NotificationChannel.EMAIL && request.getEmail() != null) {
                    request.getEmail().setHtmlBody(htmlContent);
                } else if (request.getChannel() == NotificationChannel.TELEGRAM && request.getTelegram() != null) {
                    request.getTelegram().setHtmlBody(htmlContent);
                }
            }

            SendNotificationResponse response = notificationService.sendNotification(apiKey, request);
            return ResponseEntity.ok(ApiResponse.success("Notification sent successfully", response));

        } catch (Exception e) {
            log.error("Error processing notification with file: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to process request: " + e.getMessage()));
        }
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