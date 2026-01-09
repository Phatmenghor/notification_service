package com.emenu.features.notification.dto.request;

import jakarta.validation.constraints.Email;
import lombok.Data;

@Data
public class UpdateSystemSettingsRequest {
    
    // ========== Telegram Sender Settings ==========
    private Boolean telegramEnabled;
    private String telegramBotToken;
    
    // ========== Email Sender Settings ==========
    private Boolean emailEnabled;
    
    @Email(message = "Invalid email format")
    private String emailFrom;
    
    private String emailSmtpHost;
    private Integer emailSmtpPort;
    private String emailSmtpUsername;
    private String emailSmtpPassword;
    private Boolean emailUseSSL;
    private Boolean emailUseTLS;
}