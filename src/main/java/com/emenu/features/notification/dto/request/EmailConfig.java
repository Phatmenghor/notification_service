package com.emenu.features.notification.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class EmailConfig {
    
    @NotBlank(message = "From email is required")
    @Email(message = "Invalid from email format")
    private String from;
    
    @NotEmpty(message = "At least one recipient is required")
    private List<String> to;
    
    @NotBlank(message = "SMTP host is required")
    private String smtpHost;
    
    private Integer smtpPort = 587;
    
    @NotBlank(message = "SMTP username is required")
    private String smtpUsername;
    
    @NotBlank(message = "SMTP password is required")
    private String smtpPassword;
    
    private Boolean useSSL = false;
    
    private Boolean useTLS = true;
}