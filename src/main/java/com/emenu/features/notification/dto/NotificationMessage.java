package com.emenu.features.notification.dto;

import com.emenu.enums.notification.NotificationChannel;
import com.emenu.enums.notification.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationMessage implements Serializable {
    
    private UUID logId;
    private String batchId;
    private String apiKeyValue;
    private String systemName;
    private NotificationChannel channel;
    private NotificationType type;
    private String recipient;
    private String subject;
    private String message;
    private Integer retryCount;
    
    // Telegram Config
    private String telegramBotToken;
    
    // Email Config
    private String emailFrom;
    private String emailSmtpHost;
    private Integer emailSmtpPort;
    private String emailSmtpUsername;
    private String emailSmtpPassword;
    private Boolean emailUseSSL;
    private Boolean emailUseTLS;
}