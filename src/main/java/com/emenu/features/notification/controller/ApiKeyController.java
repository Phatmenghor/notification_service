package com.emenu.features.notification.controller;

import com.emenu.features.notification.dto.request.CreateApiKeyRequest;
import com.emenu.features.notification.dto.request.UpdateApiKeyRequest;
import com.emenu.features.notification.dto.response.ApiKeyResponse;
import com.emenu.features.notification.dto.response.UsageStatsResponse;
import com.emenu.features.notification.service.ApiKeyService;
import com.emenu.shared.dto.ApiResponse;
import com.emenu.shared.dto.PaginationResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notification/api-keys")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('PLATFORM_OWNER') or hasRole('PLATFORM_ADMIN')")
public class ApiKeyController {

    private final ApiKeyService apiKeyService;

    @PostMapping
    public ResponseEntity<ApiResponse<ApiKeyResponse>> createApiKey(
            @Valid @RequestBody CreateApiKeyRequest request) {
        log.info("Creating API key for system: {}", request.getSystemName());
        ApiKeyResponse response = apiKeyService.createApiKey(request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("API key created successfully", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PaginationResponse<ApiKeyResponse>>> getAllApiKeys(
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "15") Integer pageSize) {
        PaginationResponse<ApiKeyResponse> response = apiKeyService.getAllApiKeys(pageNo, pageSize);
        return ResponseEntity.ok(ApiResponse.success("API keys retrieved", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ApiKeyResponse>> getApiKeyById(@PathVariable UUID id) {
        ApiKeyResponse response = apiKeyService.getApiKeyById(id);
        return ResponseEntity.ok(ApiResponse.success("API key retrieved", response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ApiKeyResponse>> updateApiKey(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateApiKeyRequest request) {
        ApiKeyResponse response = apiKeyService.updateApiKey(id, request);
        return ResponseEntity.ok(ApiResponse.success("API key updated", response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteApiKey(@PathVariable UUID id) {
        apiKeyService.deleteApiKey(id);
        return ResponseEntity.ok(ApiResponse.success("API key deleted", null));
    }

    @GetMapping("/usage-stats")
    public ResponseEntity<ApiResponse<UsageStatsResponse>> getUsageStats(
            @RequestHeader("X-API-Key") String apiKey) {
        UsageStatsResponse response = apiKeyService.getUsageStats(apiKey);
        return ResponseEntity.ok(ApiResponse.success("Usage stats retrieved", response));
    }
}