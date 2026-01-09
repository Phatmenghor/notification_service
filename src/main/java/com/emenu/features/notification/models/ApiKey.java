package com.emenu.features.notification.models;

import com.emenu.shared.domain.BaseUUIDEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "notification_api_keys", indexes = {
    @Index(name = "idx_notif_api_key_value", columnList = "api_key_value", unique = true),
    @Index(name = "idx_notif_api_key_system", columnList = "system_name"),
    @Index(name = "idx_notif_api_key_active", columnList = "is_active")
})
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class ApiKey extends BaseUUIDEntity {

    @Column(name = "api_key_value", nullable = false, unique = true, length = 64)
    private String apiKeyValue;

    @Column(name = "system_name", nullable = false)
    private String systemName;

    @Column(name = "company_name")
    private String companyName;

    @Column(name = "contact_email")
    private String contactEmail;

    @Column(name = "contact_phone")
    private String contactPhone;

    @Column(name = "description")
    private String description;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    // Subscription Period - Simple!
    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "never_expires")
    private Boolean neverExpires = true;

    // Usage Limits - Optional (null = unlimited)
    @Column(name = "monthly_limit")
    private Integer monthlyLimit;

    @Column(name = "current_usage")
    private Integer currentUsage = 0;

    @Column(name = "usage_reset_date")
    private LocalDateTime usageResetDate;

    // Methods
    public boolean isExpired() {
        if (Boolean.TRUE.equals(neverExpires)) {
            return false;  // Never expires
        }
        
        LocalDate today = LocalDate.now();
        
        if (endDate != null && today.isAfter(endDate)) {
            return true;  // Past end date
        }
        
        if (startDate != null && today.isBefore(startDate)) {
            return true;  // Not started yet
        }
        
        return false;
    }

    public boolean hasReachedLimit() {
        if (monthlyLimit == null || monthlyLimit <= 0) {
            return false;  // Unlimited
        }
        return currentUsage != null && currentUsage >= monthlyLimit;
    }

    public void incrementUsage() {
        if (currentUsage == null) {
            currentUsage = 0;
        }
        currentUsage++;
    }

    public void resetUsage() {
        currentUsage = 0;
        usageResetDate = LocalDateTime.now().plusMonths(1);
    }
}