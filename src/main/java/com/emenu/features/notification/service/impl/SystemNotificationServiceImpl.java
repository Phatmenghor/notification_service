package com.emenu.features.notification.service.impl;

import com.emenu.enums.notification.NotificationChannel;
import com.emenu.enums.notification.NotificationStatus;
import com.emenu.exception.custom.ValidationException;
import com.emenu.features.notification.dto.NotificationMessage;
import com.emenu.features.notification.dto.request.SystemSendNotificationRequest;
import com.emenu.features.notification.dto.request.UpdateSystemSettingsRequest;
import com.emenu.features.notification.dto.response.SystemSendNotificationResponse;
import com.emenu.features.notification.dto.response.SystemSettingsResponse;
import com.emenu.features.notification.kafka.NotificationProducer;
import com.emenu.features.notification.mapper.SystemSettingsMapper;
import com.emenu.features.notification.models.ApiKey;
import com.emenu.features.notification.models.NotificationLog;
import com.emenu.features.notification.models.SystemNotificationSettings;
import com.emenu.features.notification.repository.ApiKeyRepository;
import com.emenu.features.notification.repository.NotificationLogRepository;
import com.emenu.features.notification.repository.SystemNotificationSettingsRepository;
import com.emenu.features.notification.service.ApiKeyService;
import com.emenu.features.notification.service.SystemNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SystemNotificationServiceImpl implements SystemNotificationService {

    private final SystemNotificationSettingsRepository settingsRepository;
    private final NotificationLogRepository logRepository;
    private final ApiKeyRepository apiKeyRepository;
    private final ApiKeyService apiKeyService;
    private final NotificationProducer notificationProducer;
    private final SystemSettingsMapper settingsMapper;

    private static final String DEFAULT_SETTING_KEY = "DEFAULT";

    @Override
    @Transactional(readOnly = true)
    public SystemSettingsResponse getSystemSettings() {
        SystemNotificationSettings settings = getOrCreateSettings();
        return settingsMapper.toResponse(settings);
    }

    @Override
    @Transactional
    public SystemSettingsResponse updateSystemSettings(UpdateSystemSettingsRequest request) {
        log.info("Updating system notification settings");

        SystemNotificationSettings settings = getOrCreateSettings();

        // Update Telegram settings
        if (request.getTelegramEnabled() != null) {
            settings.setTelegramEnabled(request.getTelegramEnabled());
        }
        if (request.getTelegramBotToken() != null) {
            settings.setTelegramBotToken(request.getTelegramBotToken());
        }

        // Update Email settings
        if (request.getEmailEnabled() != null) {
            settings.setEmailEnabled(request.getEmailEnabled());
        }
        if (request.getEmailFrom() != null) {
            settings.setEmailFrom(request.getEmailFrom());
        }
        if (request.getEmailSmtpHost() != null) {
            settings.setEmailSmtpHost(request.getEmailSmtpHost());
        }
        if (request.getEmailSmtpPort() != null) {
            settings.setEmailSmtpPort(request.getEmailSmtpPort());
        }
        if (request.getEmailSmtpUsername() != null) {
            settings.setEmailSmtpUsername(request.getEmailSmtpUsername());
        }
        if (request.getEmailSmtpPassword() != null) {
            settings.setEmailSmtpPassword(request.getEmailSmtpPassword());
        }
        if (request.getEmailUseSSL() != null) {
            settings.setEmailUseSSL(request.getEmailUseSSL());
        }
        if (request.getEmailUseTLS() != null) {
            settings.setEmailUseTLS(request.getEmailUseTLS());
        }

        SystemNotificationSettings saved = settingsRepository.save(settings);
        log.info("System notification settings updated successfully");

        return settingsMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public SystemSendNotificationResponse sendSystemNotification(String apiKeyValue, SystemSendNotificationRequest request) {
        log.info("Sending system notification - Channel: {}, Type: {}", request.getChannel(), request.getType());

        // Validate API Key
        ApiKey apiKey = apiKeyService.validateApiKey(apiKeyValue);

        // Get system settings
        SystemNotificationSettings settings = getOrCreateSettings();

        // Validate channel is enabled and configured
        validateChannelSettings(settings, request.getChannel());

        // Validate recipients
        List<String> recipients = validateAndGetRecipients(request);

        if (recipients.isEmpty()) {
            throw new ValidationException("At least one recipient is required");
        }

        // Generate batch ID
        String batchId = UUID.randomUUID().toString();
        List<UUID> logIds = new ArrayList<>();

        // Create logs and send to Kafka for each recipient
        for (String recipient : recipients) {
            NotificationLog notificationLog = createSystemNotificationLog(apiKey, request, recipient, batchId);
            NotificationLog savedLog = logRepository.save(notificationLog);
            logIds.add(savedLog.getId());

            // Build message for Kafka
            NotificationMessage message = buildSystemNotificationMessage(savedLog, settings, request, recipient);

            // Send to Kafka
            if (request.getChannel() == NotificationChannel.TELEGRAM) {
                notificationProducer.sendTelegramNotification(message);
            } else if (request.getChannel() == NotificationChannel.EMAIL) {
                notificationProducer.sendEmailNotification(message);
            }

            // Increment API Key usage
            apiKey.incrementUsage();
        }

        apiKeyRepository.save(apiKey);

        log.info("System notification batch queued - Batch: {}, Recipients: {}", batchId, recipients.size());

        return SystemSendNotificationResponse.builder()
            .batchId(batchId)
            .logIds(logIds)
            .channel(request.getChannel())
            .status(NotificationStatus.PENDING)
            .totalRecipients(recipients.size())
            .message("Notifications queued successfully")
            .build();
    }

    private SystemNotificationSettings getOrCreateSettings() {
        return settingsRepository.findBySettingKeyAndIsDeletedFalse(DEFAULT_SETTING_KEY)
            .orElseGet(() -> {
                SystemNotificationSettings newSettings = new SystemNotificationSettings();
                newSettings.setSettingKey(DEFAULT_SETTING_KEY);
                return settingsRepository.save(newSettings);
            });
    }

    private void validateChannelSettings(SystemNotificationSettings settings, NotificationChannel channel) {
        if (channel == NotificationChannel.TELEGRAM) {
            if (!Boolean.TRUE.equals(settings.getTelegramEnabled())) {
                throw new ValidationException("Telegram notifications are disabled in system settings");
            }
            if (settings.getTelegramBotToken() == null || settings.getTelegramBotToken().isBlank()) {
                throw new ValidationException("Telegram bot token is not configured");
            }
        } else if (channel == NotificationChannel.EMAIL) {
            if (!Boolean.TRUE.equals(settings.getEmailEnabled())) {
                throw new ValidationException("Email notifications are disabled in system settings");
            }
            if (settings.getEmailFrom() == null || settings.getEmailSmtpHost() == null ||
                settings.getEmailSmtpPort() == null) {
                throw new ValidationException("Email is not fully configured");
            }
        }
    }

    private List<String> validateAndGetRecipients(SystemSendNotificationRequest request) {
        if (request.getChannel() == NotificationChannel.TELEGRAM) {
            if (request.getTelegramChatIds() == null || request.getTelegramChatIds().isEmpty()) {
                throw new ValidationException("At least one Telegram chat ID is required");
            }
            return request.getTelegramChatIds();
        } else if (request.getChannel() == NotificationChannel.EMAIL) {
            if (request.getEmailRecipients() == null || request.getEmailRecipients().isEmpty()) {
                throw new ValidationException("At least one email recipient is required");
            }
            return request.getEmailRecipients();
        }
        
        throw new ValidationException("Unsupported notification channel");
    }

    private NotificationLog createSystemNotificationLog(
            ApiKey apiKey, SystemSendNotificationRequest request, String recipient, String batchId) {

        NotificationLog notificationLog = new NotificationLog();
        notificationLog.setBatchId(batchId);
        notificationLog.setApiKeyValue(apiKey.getApiKeyValue());
        notificationLog.setSystemName(apiKey.getSystemName());
        notificationLog.setChannel(request.getChannel());
        notificationLog.setType(request.getType());
        notificationLog.setStatus(NotificationStatus.PENDING);
        notificationLog.setRecipient(recipient);
        notificationLog.setSubject(request.getSubject());
        notificationLog.setMessage(request.getMessage());
        notificationLog.setRetryCount(0);

        return notificationLog;
    }

    private NotificationMessage buildSystemNotificationMessage(
            NotificationLog notificationLog, SystemNotificationSettings settings, 
            SystemSendNotificationRequest request, String recipient) {

        NotificationMessage.NotificationMessageBuilder builder = NotificationMessage.builder()
            .logId(notificationLog.getId())
            .batchId(notificationLog.getBatchId())
            .apiKeyValue(notificationLog.getApiKeyValue())
            .systemName(notificationLog.getSystemName())
            .channel(request.getChannel())
            .type(request.getType())
            .recipient(recipient)
            .subject(request.getSubject())
            .message(request.getMessage())
            .retryCount(0);

        if (request.getChannel() == NotificationChannel.TELEGRAM) {
            builder.telegramBotToken(settings.getTelegramBotToken());
        } else if (request.getChannel() == NotificationChannel.EMAIL) {
            builder.emailFrom(settings.getEmailFrom())
                   .emailSmtpHost(settings.getEmailSmtpHost())
                   .emailSmtpPort(settings.getEmailSmtpPort())
                   .emailSmtpUsername(settings.getEmailSmtpUsername())
                   .emailSmtpPassword(settings.getEmailSmtpPassword())
                   .emailUseSSL(settings.getEmailUseSSL())
                   .emailUseTLS(settings.getEmailUseTLS());
        }

        return builder.build();
    }
}