package com.emenu.features.notification.service.impl;

import com.emenu.exception.custom.AlreadyExistsException;
import com.emenu.exception.custom.NotFoundException;
import com.emenu.exception.custom.UnauthorizedException;
import com.emenu.exception.custom.ValidationException;
import com.emenu.features.notification.dto.request.CreateApiKeyRequest;
import com.emenu.features.notification.dto.request.UpdateApiKeyRequest;
import com.emenu.features.notification.dto.response.ApiKeyResponse;
import com.emenu.features.notification.dto.response.UsageStatsResponse;
import com.emenu.features.notification.mapper.ApiKeyMapper;
import com.emenu.features.notification.models.ApiKey;
import com.emenu.features.notification.repository.ApiKeyRepository;
import com.emenu.features.notification.service.ApiKeyService;
import com.emenu.shared.dto.PaginationResponse;
import com.emenu.shared.pagination.PaginationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ApiKeyServiceImpl implements ApiKeyService {

    private final ApiKeyRepository apiKeyRepository;
    private final ApiKeyMapper apiKeyMapper;

    private static final int API_KEY_LENGTH = 32;

    @Override
    public ApiKeyResponse createApiKey(CreateApiKeyRequest request) {
        log.info("Creating API key for system: {}", request.getSystemName());

        // Check if system name already exists
        if (apiKeyRepository.existsBySystemNameAndIsDeletedFalse(request.getSystemName())) {
            throw new AlreadyExistsException("API key for system '" + request.getSystemName() + "' already exists");
        }

        // Validate dates
        if (Boolean.FALSE.equals(request.getNeverExpires())) {
            if (request.getEndDate() == null) {
                throw new ValidationException("End date is required when neverExpires is false");
            }
            if (request.getStartDate() != null && request.getEndDate().isBefore(request.getStartDate())) {
                throw new ValidationException("End date must be after start date");
            }
        }

        // Generate secure API key
        String apiKeyValue = generateSecureApiKey();

        ApiKey apiKey = apiKeyMapper.toEntity(request);
        apiKey.setApiKeyValue(apiKeyValue);
        apiKey.setIsActive(true);
        apiKey.setCurrentUsage(0);

        // Set usage reset date to first day of next month
        apiKey.setUsageResetDate(LocalDateTime.now().withDayOfMonth(1).plusMonths(1).withHour(0).withMinute(0).withSecond(0));

        ApiKey savedApiKey = apiKeyRepository.save(apiKey);
        log.info("API key created successfully for system: {}", request.getSystemName());

        return apiKeyMapper.toResponse(savedApiKey);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginationResponse<ApiKeyResponse> getAllApiKeys(Integer pageNo, Integer pageSize) {
        Pageable pageable = PaginationUtils.createPageable(pageNo, pageSize, "createdAt", "DESC");
        Page<ApiKey> apiKeys = apiKeyRepository.findAllByIsDeletedFalse(pageable);
        return apiKeyMapper.toPaginationResponse(apiKeys);
    }

    @Override
    @Transactional(readOnly = true)
    public ApiKeyResponse getApiKeyById(UUID id) {
        ApiKey apiKey = apiKeyRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("API key not found"));
        return apiKeyMapper.toResponse(apiKey);
    }

    @Override
    public ApiKeyResponse updateApiKey(UUID id, UpdateApiKeyRequest request) {
        log.info("Updating API key: {}", id);

        ApiKey apiKey = apiKeyRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("API key not found"));

        if (request.getSystemName() != null && !request.getSystemName().equals(apiKey.getSystemName())) {
            if (apiKeyRepository.existsBySystemNameAndIsDeletedFalse(request.getSystemName())) {
                throw new AlreadyExistsException("System name already exists");
            }
            apiKey.setSystemName(request.getSystemName());
        }

        if (request.getCompanyName() != null) {
            apiKey.setCompanyName(request.getCompanyName());
        }
        if (request.getContactEmail() != null) {
            apiKey.setContactEmail(request.getContactEmail());
        }
        if (request.getContactPhone() != null) {
            apiKey.setContactPhone(request.getContactPhone());
        }
        if (request.getDescription() != null) {
            apiKey.setDescription(request.getDescription());
        }
        if (request.getIsActive() != null) {
            apiKey.setIsActive(request.getIsActive());
        }
        if (request.getStartDate() != null) {
            apiKey.setStartDate(request.getStartDate());
        }
        if (request.getEndDate() != null) {
            apiKey.setEndDate(request.getEndDate());
        }
        if (request.getNeverExpires() != null) {
            apiKey.setNeverExpires(request.getNeverExpires());
        }
        if (request.getMonthlyLimit() != null) {
            apiKey.setMonthlyLimit(request.getMonthlyLimit());
        }

        ApiKey updatedApiKey = apiKeyRepository.save(apiKey);
        log.info("API key updated successfully: {}", id);

        return apiKeyMapper.toResponse(updatedApiKey);
    }

    @Override
    public void deleteApiKey(UUID id) {
        log.info("Deleting API key: {}", id);

        ApiKey apiKey = apiKeyRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("API key not found"));

        apiKey.setIsDeleted(true);
        apiKeyRepository.save(apiKey);

        log.info("API key deleted successfully: {}", id);
    }

    @Override
    public ApiKey validateApiKey(String apiKeyValue) {
        ApiKey apiKey = apiKeyRepository.findByApiKeyValueAndIsDeletedFalse(apiKeyValue)
                .orElseThrow(() -> new UnauthorizedException("Invalid API key"));

        if (!Boolean.TRUE.equals(apiKey.getIsActive())) {
            throw new UnauthorizedException("API key is inactive");
        }

        if (apiKey.isExpired()) {
            throw new UnauthorizedException("API key has expired");
        }

        if (apiKey.hasReachedLimit()) {
            throw new UnauthorizedException("API key has reached monthly usage limit");
        }

        return apiKey;
    }

    @Override
    @Transactional(readOnly = true)
    public UsageStatsResponse getUsageStats(String apiKeyValue) {
        ApiKey apiKey = apiKeyRepository.findByApiKeyValueAndIsDeletedFalse(apiKeyValue)
                .orElseThrow(() -> new NotFoundException("API key not found"));

        boolean isUnlimited = apiKey.getMonthlyLimit() == null;
        int currentUsage = apiKey.getCurrentUsage() != null ? apiKey.getCurrentUsage() : 0;
        int remainingQuota = isUnlimited ? 0 : apiKey.getMonthlyLimit() - currentUsage;
        double usagePercentage = isUnlimited ? 0.0 :
                (currentUsage * 100.0) / apiKey.getMonthlyLimit();

        return UsageStatsResponse.builder()
                .currentUsage(currentUsage)
                .monthlyLimit(apiKey.getMonthlyLimit())
                .remainingQuota(remainingQuota)
                .usagePercentage(usagePercentage)
                .isUnlimited(isUnlimited)
                .isExpired(apiKey.isExpired())
                .build();
    }

    @Override
    public void resetUsageForAllKeys() {
        log.info("Starting monthly usage reset for all API keys");

        LocalDateTime now = LocalDateTime.now();
        apiKeyRepository.findAllByIsDeletedFalse().forEach(apiKey -> {
            if (apiKey.getUsageResetDate() != null &&
                    !now.isBefore(apiKey.getUsageResetDate())) {
                apiKey.resetUsage();
                apiKeyRepository.save(apiKey);
                log.info("Usage reset for API key: {}", apiKey.getSystemName());
            }
        });

        log.info("Monthly usage reset completed");
    }

    private String generateSecureApiKey() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] randomBytes = new byte[API_KEY_LENGTH];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
}