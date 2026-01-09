package com.emenu.features.notification.service;

import com.emenu.features.notification.dto.request.SendNotificationRequest;
import com.emenu.features.notification.dto.response.NotificationLogResponse;
import com.emenu.features.notification.dto.response.SendNotificationResponse;
import com.emenu.shared.dto.PaginationResponse;

import java.util.UUID;

public interface NotificationService {
    
    SendNotificationResponse sendNotification(String apiKey, SendNotificationRequest request);
    
    PaginationResponse<NotificationLogResponse> getMyLogs(String apiKey, Integer pageNo, Integer pageSize);
    
    PaginationResponse<NotificationLogResponse> getBatchLogs(String batchId, Integer pageNo, Integer pageSize);
    
    NotificationLogResponse getLogById(UUID logId);
}