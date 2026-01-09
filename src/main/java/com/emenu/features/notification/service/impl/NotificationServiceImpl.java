package com.emenu.features.notification.service.impl;

import com.emenu.enums.notification.NotificationChannel;
import com.emenu.enums.notification.NotificationStatus;
import com.emenu.exception.custom.NotFoundException;
import com.emenu.exception.custom.ValidationException;
import com.emenu.features.notification.dto.NotificationMessage;
import com.emenu.features.notification.dto.request.SendNotificationRequest;
import com.emenu.features.notification.dto.response.NotificationLogResponse;
import com.emenu.features.notification.dto.response.SendNotificationResponse;
import com.emenu.features.notification.kafka.NotificationProducer;
import com.emenu.features.notification.mapper.NotificationLogMapper;
import com.emenu.features.notification.models.ApiKey;
import com.emenu.features.notification.models.NotificationLog;
import com.emenu.features.notification.repository.ApiKeyRepository;
import com.emenu.features.notification.repository.NotificationLogRepository;
import com.emenu.features.notification.service.ApiKeyService;
import com.emenu.features.notification.service.NotificationService;
import com.emenu.shared.dto.PaginationResponse;
import com.emenu.shared.pagination.PaginationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private final ApiKeyService apiKeyService;
    private final ApiKeyRepository apiKeyRepository;
    private final NotificationLogRepository logRepository;
    private final NotificationProducer notificationProducer;
    private final NotificationLogMapper logMapper;

    @Value("${notification.gateway.max-recipients-per-request:100}")
    private int maxRecipients;

    @Override
    public SendNotificationResponse sendNotification(String apiKeyValue, SendNotificationRequest request) {
        log.info("Processing notification request - Channel: {}, Type: {}", 
                 request.getChannel(), request.getType());

        // Validate API key
        ApiKey apiKey = apiKeyService.validateApiKey(apiKeyValue);

        // Validate configuration
        List<String> recipients = validateAndGetRecipients(request);

        if (recipients.size() > maxRecipients) {
            throw new ValidationException("Maximum " + maxRecipients + " recipients per request");
        }

        // Generate batch ID
        String batchId = UUID.randomUUID().toString();
        List<UUID> logIds = new ArrayList<>();

        // Create logs and send to Kafka for each recipient
        for (String recipient : recipients) {
            NotificationLog notificationLog = createNotificationLog(apiKey, request, recipient, batchId);
            NotificationLog savedLog = logRepository.save(notificationLog);
            logIds.add(savedLog.getId());

            // Build message for Kafka
            NotificationMessage message = buildNotificationMessage(
                savedLog, apiKey, request, recipient, batchId
            );

            // Send to appropriate Kafka topic
            if (request.getChannel() == NotificationChannel.TELEGRAM) {
                notificationProducer.sendTelegramNotification(message);
            } else if (request.getChannel() == NotificationChannel.EMAIL) {
                notificationProducer.sendEmailNotification(message);
            }

            // Increment usage
            apiKey.incrementUsage();
        }

        apiKeyRepository.save(apiKey);

        log.info("Notification batch created - Batch: {}, Recipients: {}", batchId, recipients.size());

        return SendNotificationResponse.builder()
            .batchId(batchId)
            .logIds(logIds)
            .channel(request.getChannel())
            .status(NotificationStatus.PENDING)
            .totalRecipients(recipients.size())
            .message("Notifications queued successfully")
            .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PaginationResponse<NotificationLogResponse> getMyLogs(
            String apiKeyValue, Integer pageNo, Integer pageSize) {
        
        Pageable pageable = PaginationUtils.createPageable(pageNo, pageSize, "createdAt", "DESC");
        Page<NotificationLog> logs = logRepository.findByApiKeyValueAndIsDeletedFalse(apiKeyValue, pageable);
        return logMapper.toPaginationResponse(logs);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginationResponse<NotificationLogResponse> getBatchLogs(
            String batchId, Integer pageNo, Integer pageSize) {
        
        Pageable pageable = PaginationUtils.createPageable(pageNo, pageSize, "createdAt", "ASC");
        Page<NotificationLog> logs = logRepository.findByBatchIdAndIsDeletedFalse(batchId, pageable);
        return logMapper.toPaginationResponse(logs);
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationLogResponse getLogById(UUID logId) {
        NotificationLog notificationLog = logRepository.findById(logId)
            .orElseThrow(() -> new NotFoundException("Notification log not found"));
        return logMapper.toResponse(notificationLog);
    }

    private List<String> validateAndGetRecipients(SendNotificationRequest request) {
        if (request.getChannel() == NotificationChannel.TELEGRAM) {
            if (request.getTelegram() == null) {
                throw new ValidationException("Telegram configuration is required");
            }
            if (request.getTelegram().getBotToken() == null || request.getTelegram().getBotToken().isBlank()) {
                throw new ValidationException("Telegram bot token is required");
            }
            if (request.getTelegram().getChatIds() == null || request.getTelegram().getChatIds().isEmpty()) {
                throw new ValidationException("At least one Telegram chat ID is required");
            }
            return request.getTelegram().getChatIds();
            
        } else if (request.getChannel() == NotificationChannel.EMAIL) {
            if (request.getEmail() == null) {
                throw new ValidationException("Email configuration is required");
            }
            if (request.getEmail().getTo() == null || request.getEmail().getTo().isEmpty()) {
                throw new ValidationException("At least one email recipient is required");
            }
            return request.getEmail().getTo();
        }
        
        throw new ValidationException("Unsupported notification channel");
    }

    private NotificationLog createNotificationLog(
            ApiKey apiKey, SendNotificationRequest request, String recipient, String batchId) {
        
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

    private NotificationMessage buildNotificationMessage(
            NotificationLog notificationLog, ApiKey apiKey, SendNotificationRequest request, 
            String recipient, String batchId) {
        
        NotificationMessage.NotificationMessageBuilder builder = NotificationMessage.builder()
            .logId(notificationLog.getId())
            .batchId(batchId)
            .apiKeyValue(apiKey.getApiKeyValue())
            .systemName(apiKey.getSystemName())
            .channel(request.getChannel())
            .type(request.getType())
            .recipient(recipient)
            .subject(request.getSubject())
            .message(request.getMessage())
            .retryCount(0);

        if (request.getChannel() == NotificationChannel.TELEGRAM) {
            builder.telegramBotToken(request.getTelegram().getBotToken());
        } else if (request.getChannel() == NotificationChannel.EMAIL) {
            builder.emailFrom(request.getEmail().getFrom())
                   .emailSmtpHost(request.getEmail().getSmtpHost())
                   .emailSmtpPort(request.getEmail().getSmtpPort())
                   .emailSmtpUsername(request.getEmail().getSmtpUsername())
                   .emailSmtpPassword(request.getEmail().getSmtpPassword())
                   .emailUseSSL(request.getEmail().getUseSSL())
                   .emailUseTLS(request.getEmail().getUseTLS());
        }

        return builder.build();
    }
}