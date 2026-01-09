package com.emenu.features.notification.tasks;

import com.emenu.features.notification.service.ApiKeyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class UsageResetScheduler {

    private final ApiKeyService apiKeyService;

    @Scheduled(cron = "0 0 0 * * ?") // Every day at midnight
    public void resetMonthlyUsage() {
        log.info("Running scheduled usage reset check");
        
        try {
            apiKeyService.resetUsageForAllKeys();
        } catch (Exception e) {
            log.error("Error during usage reset: {}", e.getMessage(), e);
        }
    }
}