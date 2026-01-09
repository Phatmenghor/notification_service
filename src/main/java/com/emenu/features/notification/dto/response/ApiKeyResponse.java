package com.emenu.features.notification.dto.response;

import com.emenu.shared.dto.BaseAuditResponse;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@EqualsAndHashCode(callSuper = true)
@Data
public class ApiKeyResponse extends BaseAuditResponse {
    
    private String apiKeyValue;
    
    private String systemName;
    
    private String companyName;
    
    private String contactEmail;
    
    private String contactPhone;
    
    private String description;
    
    private Boolean isActive;
    
    private LocalDate startDate;
    
    private LocalDate endDate;
    
    private Boolean neverExpires;
    
    private Integer monthlyLimit;
    
    private Integer currentUsage;
    
    private LocalDate usageResetDate;
    
    private Boolean isExpired;
    
    private Boolean isUnlimited;
}