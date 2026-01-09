package com.emenu.features.notification.dto.request;

import com.emenu.enums.notification.NotificationChannel;
import com.emenu.enums.notification.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SystemSendNotificationRequest {
    
    @NotNull(message = "Channel is required (TELEGRAM or EMAIL)")
    private NotificationChannel channel;
    
    @NotNull(message = "Type is required (ALERT, INFO, WARNING, ERROR, SUCCESS)")
    private NotificationType type;
    
    private String subject;
    
    @NotBlank(message = "Message is required")
    private String message;
}