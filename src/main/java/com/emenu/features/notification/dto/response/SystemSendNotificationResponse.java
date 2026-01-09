package com.emenu.features.notification.dto.response;

import com.emenu.enums.notification.NotificationChannel;
import com.emenu.enums.notification.NotificationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemSendNotificationResponse {
    
    private String batchId;
    
    private List<UUID> logIds;
    
    private NotificationChannel channel;
    
    private NotificationStatus status;
    
    private Integer totalRecipients;
    
    private String message;
}