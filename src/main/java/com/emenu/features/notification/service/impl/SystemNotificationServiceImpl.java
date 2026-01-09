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

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
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
        if (request.getTelegramChatId() != null) {
            settings.setTelegramChatId(request.getTelegramChatId());
        }

        // Update Email settings
        if (request.getEmailEnabled() != null) {
            settings.setEmailEnabled(request.getEmailEnabled());
        }
        if (request.getEmailFrom() != null) {
            settings.setEmailFrom(request.getEmailFrom());
        }
        if (request.getEmailTo() != null) {
            settings.setEmailTo(request.getEmailTo());
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
    public SystemSendNotificationResponse sendSystemNotification(String apiKeyValue, SystemSendNotificationRequest request) {
        log.info("Sending system notification - Channel: {}, Type: {}", request.getChannel(), request.getType());

        // Validate API Key
        ApiKey apiKey = apiKeyService.validateApiKey(apiKeyValue);

        // Get system settings
        SystemNotificationSettings settings = getOrCreateSettings();

        // Validate channel is enabled and configured
        validateChannelSettings(settings, request.getChannel());

        // Create notification log
        NotificationLog notificationLog = createSystemNotificationLog(apiKey, settings, request);
        NotificationLog savedLog = logRepository.save(notificationLog);

        // Build message for Kafka
        NotificationMessage message = buildSystemNotificationMessage(savedLog, settings, request);

        // Send to Kafka
        if (request.getChannel() == NotificationChannel.TELEGRAM) {
            notificationProducer.sendTelegramNotification(message);
        } else if (request.getChannel() == NotificationChannel.EMAIL) {
            notificationProducer.sendEmailNotification(message);
        }

        // Increment API Key usage
        apiKey.incrementUsage();
        apiKeyRepository.save(apiKey);

        log.info("System notification queued - Log ID: {}", savedLog.getId());

        return SystemSendNotificationResponse.builder()
            .logId(savedLog.getId())
            .channel(request.getChannel())
            .status(NotificationStatus.PENDING)
            .message("Notification queued successfully")
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
            if (settings.getTelegramChatId() == null || settings.getTelegramChatId().isBlank()) {
                throw new ValidationException("Telegram chat ID is not configured");
            }
        } else if (channel == NotificationChannel.EMAIL) {
            if (!Boolean.TRUE.equals(settings.getEmailEnabled())) {
                throw new ValidationException("Email notifications are disabled in system settings");
            }
            if (settings.getEmailFrom() == null || settings.getEmailTo() == null ||
                settings.getEmailSmtpHost() == null || settings.getEmailSmtpPort() == null) {
                throw new ValidationException("Email is not fully configured");
            }
        }
    }

    private NotificationLog createSystemNotificationLog(
            ApiKey apiKey, SystemNotificationSettings settings, SystemSendNotificationRequest request) {

        NotificationLog notificationLog = new NotificationLog();
        notificationLog.setBatchId(UUID.randomUUID().toString());
        notificationLog.setApiKeyValue(apiKey.getApiKeyValue());
        notificationLog.setSystemName(apiKey.getSystemName());
        notificationLog.setChannel(request.getChannel());
        notificationLog.setType(request.getType());
        notificationLog.setStatus(NotificationStatus.PENDING);
        notificationLog.setSubject(request.getSubject());
        notificationLog.setMessage(request.getMessage());
        notificationLog.setRetryCount(0);

        if (request.getChannel() == NotificationChannel.TELEGRAM) {
            notificationLog.setRecipient(settings.getTelegramChatId());
        } else if (request.getChannel() == NotificationChannel.EMAIL) {
            notificationLog.setRecipient(settings.getEmailTo());
        }

        return notificationLog;
    }

    private NotificationMessage buildSystemNotificationMessage(
            NotificationLog notificationLog, SystemNotificationSettings settings, SystemSendNotificationRequest request) {

        NotificationMessage.NotificationMessageBuilder builder = NotificationMessage.builder()
            .logId(notificationLog.getId())
            .batchId(notificationLog.getBatchId())
            .apiKeyValue(notificationLog.getApiKeyValue())
            .systemName(notificationLog.getSystemName())
            .channel(request.getChannel())
            .type(request.getType())
            .recipient(notificationLog.getRecipient())
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