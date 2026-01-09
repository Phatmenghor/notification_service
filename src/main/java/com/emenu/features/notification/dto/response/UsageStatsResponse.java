package com.emenu.features.notification.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsageStatsResponse {
    
    private Integer currentUsage;
    
    private Integer monthlyLimit;
    
    private Integer remainingQuota;
    
    private Double usagePercentage;
    
    private Boolean isUnlimited;
    
    private Boolean isExpired;
}