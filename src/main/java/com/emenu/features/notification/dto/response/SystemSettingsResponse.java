package com.emenu.features.notification.dto.response;

import com.emenu.shared.dto.BaseAuditResponse;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class SystemSettingsResponse extends BaseAuditResponse {
    
    // Telegram
    private Boolean telegramEnabled;
    private String telegramChatId;
    private Boolean telegramConfigured;
    
    // Email
    private Boolean emailEnabled;
    private String emailFrom;
    private String emailTo;
    private String emailSmtpHost;
    private Integer emailSmtpPort;
    private Boolean emailConfigured;
}