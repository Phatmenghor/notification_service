package com.emenu.features.notification.dto.response;

import com.emenu.enums.notification.NotificationChannel;
import com.emenu.enums.notification.NotificationStatus;
import com.emenu.enums.notification.NotificationType;
import com.emenu.shared.dto.BaseAuditResponse;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = true)
@Data
public class NotificationLogResponse extends BaseAuditResponse {
    
    private String batchId;
    
    private String apiKeyValue;
    
    private String systemName;
    
    private NotificationChannel channel;
    
    private NotificationType type;
    
    private NotificationStatus status;
    
    private String recipient;
    
    private String subject;
    
    private String message;
    
    private String response;
    
    private String errorMessage;
    
    private LocalDateTime sentAt;
    
    private Integer retryCount;
}