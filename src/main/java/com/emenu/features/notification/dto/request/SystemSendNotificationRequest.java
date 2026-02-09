package com.emenu.features.notification.dto.request;

import com.emenu.enums.notification.NotificationChannel;
import com.emenu.enums.notification.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class SystemSendNotificationRequest {
    
    @NotNull(message = "Channel is required (TELEGRAM or EMAIL)")
    private NotificationChannel channel;
    
    @NotNull(message = "Type is required (ALERT, INFO, WARNING, ERROR, SUCCESS)")
    private NotificationType type;
    
    private String subject;
    
    @NotBlank(message = "Message is required")
    private String message;
    
    // ========== Recipients (Required) ==========
    
    // For Telegram: List of chat IDs
    private List<String> telegramChatIds;
    
    // For Email: List of email addresses
    private List<String> emailRecipients;

    // ========== Custom Template Bodies (Optional) ==========

    // Custom HTML body for email - replaces default template entirely
    // Allows full HTML design like any email builder
    private String emailHtmlBody;

    // Custom formatted body for Telegram - replaces default template entirely
    // Supports full Telegram HTML: <b>, <i>, <u>, <s>, <code>, <pre>, <a href="">, <tg-spoiler>, etc.
    private String telegramHtmlBody;
}