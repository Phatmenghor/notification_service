package com.emenu.features.notification.dto.response;

import com.emenu.shared.dto.BaseAuditResponse;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class SystemSettingsResponse extends BaseAuditResponse {
    
    // ========== Telegram Sender Config ==========
    private Boolean telegramEnabled;
    private Boolean telegramConfigured;
    
    // ========== Email Sender Config ==========
    private Boolean emailEnabled;
    private String emailFrom;
    private String emailSmtpHost;
    private Integer emailSmtpPort;
    private Boolean emailConfigured;
}