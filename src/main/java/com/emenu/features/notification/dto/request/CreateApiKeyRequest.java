package com.emenu.features.notification.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateApiKeyRequest {
    
    @NotBlank(message = "System name is required")
    private String systemName;
    
    private String companyName;
    
    @Email(message = "Invalid email format")
    private String contactEmail;
    
    private String contactPhone;
    
    private String description;
    
    private LocalDate startDate;
    
    private LocalDate endDate;
    
    private Boolean neverExpires = true;
    
    private Integer monthlyLimit; // null = unlimited
}