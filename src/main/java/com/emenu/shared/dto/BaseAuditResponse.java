package com.emenu.shared.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class BaseAuditResponse {
    
    private UUID id;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}