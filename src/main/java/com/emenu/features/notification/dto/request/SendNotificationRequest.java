package com.emenu.features.notification.dto.request;

import com.emenu.enums.notification.NotificationChannel;
import com.emenu.enums.notification.NotificationType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SendNotificationRequest {

    @NotNull(message = "Channel is required (TELEGRAM or EMAIL)")
    private NotificationChannel channel;

    // Optional - not needed when using custom HTML file
    private NotificationType type;

    private String subject;

    // Optional - not needed when using custom HTML file
    private String message;

    @Valid
    private TelegramConfig telegram;

    @Valid
    private EmailConfig email;
}
