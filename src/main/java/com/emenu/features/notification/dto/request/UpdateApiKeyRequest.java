package com.emenu.features.notification.dto.request;

import jakarta.validation.constraints.Email;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdateApiKeyRequest {
    
    private String systemName;
    
    private String companyName;
    
    @Email(message = "Invalid email format")
    private String contactEmail;
    
    private String contactPhone;
    
    private String description;
    
    private Boolean isActive;
    
    private LocalDate startDate;
    
    private LocalDate endDate;
    
    private Boolean neverExpires;
    
    private Integer monthlyLimit;
}