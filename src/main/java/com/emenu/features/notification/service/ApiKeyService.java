package com.emenu.features.notification.service;

import com.emenu.features.notification.dto.request.CreateApiKeyRequest;
import com.emenu.features.notification.dto.request.UpdateApiKeyRequest;
import com.emenu.features.notification.dto.response.ApiKeyResponse;
import com.emenu.features.notification.dto.response.UsageStatsResponse;
import com.emenu.features.notification.models.ApiKey;
import com.emenu.shared.dto.PaginationResponse;

import java.util.UUID;

public interface ApiKeyService {
    
    ApiKeyResponse createApiKey(CreateApiKeyRequest request);
    
    PaginationResponse<ApiKeyResponse> getAllApiKeys(Integer pageNo, Integer pageSize);
    
    ApiKeyResponse getApiKeyById(UUID id);
    
    ApiKeyResponse updateApiKey(UUID id, UpdateApiKeyRequest request);
    
    void deleteApiKey(UUID id);
    
    ApiKey validateApiKey(String apiKeyValue);
    
    UsageStatsResponse getUsageStats(String apiKeyValue);
    
    void resetUsageForAllKeys();
}