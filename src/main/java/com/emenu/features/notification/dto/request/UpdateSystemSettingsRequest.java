package com.emenu.features.notification.dto.request;

import jakarta.validation.constraints.Email;
import lombok.Data;

@Data
public class UpdateSystemSettingsRequest {
    
    // Telegram Settings
    private Boolean telegramEnabled;
    private String telegramBotToken;
    private String telegramChatId;
    
    // Email Settings
    private Boolean emailEnabled;
    
    @Email(message = "Invalid email format")
    private String emailFrom;
    
    @Email(message = "Invalid email format")
    private String emailTo;
    
    private String emailSmtpHost;
    private Integer emailSmtpPort;
    private String emailSmtpUsername;
    private String emailSmtpPassword;
    private Boolean emailUseSSL;
    private Boolean emailUseTLS;
}