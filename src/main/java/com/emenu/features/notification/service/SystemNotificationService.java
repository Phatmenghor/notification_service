package com.emenu.features.notification.service;

import com.emenu.features.notification.dto.request.SystemSendNotificationRequest;
import com.emenu.features.notification.dto.request.UpdateSystemSettingsRequest;
import com.emenu.features.notification.dto.response.SystemSendNotificationResponse;
import com.emenu.features.notification.dto.response.SystemSettingsResponse;

public interface SystemNotificationService {
    
    SystemSettingsResponse getSystemSettings();
    
    SystemSettingsResponse updateSystemSettings(UpdateSystemSettingsRequest request);
    
    SystemSendNotificationResponse sendSystemNotification(String apiKey, SystemSendNotificationRequest request);
}