package com.emenu.features.notification.dto.request;

import com.emenu.enums.notification.NotificationChannel;
import com.emenu.enums.notification.NotificationType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class SystemSendNotificationRequest {

    @NotNull(message = "Channel is required (TELEGRAM or EMAIL)")
    private NotificationChannel channel;

    // Optional - not needed when using custom HTML file
    private NotificationType type;

    private String subject;

    // Optional - not needed when using custom HTML file
    private String message;

    // ========== Recipients (Required) ==========

    // For Telegram: List of chat IDs
    private List<String> telegramChatIds;

    // For Email: List of email addresses
    private List<String> emailRecipients;

    // ========== Custom Template Bodies (Optional) ==========

    // Custom HTML body for email - replaces default template entirely
    private String emailHtmlBody;

    // Custom formatted body for Telegram - replaces default template entirely
    // Supports full Telegram HTML: <b>, <i>, <u>, <s>, <code>, <pre>, <a href="">, <tg-spoiler>, etc.
    private String telegramHtmlBody;
}
