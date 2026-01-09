package com.emenu.features.notification.kafka;

import com.emenu.enums.notification.NotificationStatus;
import com.emenu.features.notification.dto.NotificationMessage;
import com.emenu.features.notification.models.NotificationLog;
import com.emenu.features.notification.repository.NotificationLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class TelegramNotificationConsumer {

    private final NotificationLogRepository logRepository;
    private final ObjectMapper objectMapper;
    private final WebClient.Builder webClientBuilder;

    @Value("${notification.telegram.api-url}")
    private String telegramApiUrl;

    @Value("${notification.telegram.timeout:5000}")
    private int timeout;

    @KafkaListener(
        topics = "telegram-notifications",
        groupId = "${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeTelegramNotification(
            @Payload String payload,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {
        
        try {
            log.info("Processing Telegram notification - Partition: {}, Offset: {}", partition, offset);
            
            NotificationMessage message = objectMapper.readValue(payload, NotificationMessage.class);
            
            // Add retry mechanism with exponential backoff for finding the log
            NotificationLog notificationLog = findNotificationLogWithRetry(message.getLogId(), 5, 200);
            
            if (notificationLog == null) {
                log.error("Failed to find notification log after retries: {}", message.getLogId());
                acknowledgment.acknowledge();
                return;
            }
            
            sendToTelegram(message, notificationLog);
            acknowledgment.acknowledge();
            
        } catch (Exception e) {
            log.error("Error processing Telegram notification: {}", e.getMessage(), e);
            acknowledgment.acknowledge();
        }
    }

    private NotificationLog findNotificationLogWithRetry(java.util.UUID logId, int maxRetries, long delayMs) {
        for (int i = 0; i < maxRetries; i++) {
            try {
                var optionalLog = logRepository.findById(logId);
                if (optionalLog.isPresent()) {
                    return optionalLog.get();
                }
                
                if (i < maxRetries - 1) {
                    log.debug("Log not found yet, attempt {}/{}, waiting {}ms", i + 1, maxRetries, delayMs);
                    Thread.sleep(delayMs);
                    delayMs *= 2; // Exponential backoff
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Thread interrupted while waiting for log");
                return null;
            } catch (Exception e) {
                log.error("Error finding notification log: {}", e.getMessage());
            }
        }
        return null;
    }

    private void sendToTelegram(NotificationMessage message, NotificationLog notificationLog) {
        int maxRetries = 3;
        
        for (int attempt = 0; attempt < maxRetries; attempt++) {
            try {
                // ✅ FIX: Fetch fresh entity from database to avoid stale state
                NotificationLog freshLog = logRepository.findById(notificationLog.getId())
                    .orElseThrow(() -> new RuntimeException("Log not found"));
                
                freshLog.setStatus(NotificationStatus.PROCESSING);
                logRepository.saveAndFlush(freshLog);

                String apiUrl = telegramApiUrl + message.getTelegramBotToken() + "/sendMessage";

                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("chat_id", message.getRecipient());
                requestBody.put("text", formatTelegramMessage(message));
                requestBody.put("parse_mode", "HTML");

                WebClient webClient = webClientBuilder.baseUrl(apiUrl).build();
                
                String response = webClient.post()
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofMillis(timeout))
                    .onErrorResume(e -> {
                        log.error("Telegram API error: {}", e.getMessage());
                        return Mono.just("ERROR: " + e.getMessage());
                    })
                    .block();

                // ✅ FIX: Fetch fresh entity again before final update
                freshLog = logRepository.findById(notificationLog.getId())
                    .orElseThrow(() -> new RuntimeException("Log not found"));
                
                freshLog.setStatus(NotificationStatus.SENT);
                freshLog.setResponse(response);
                freshLog.setSentAt(LocalDateTime.now());
                logRepository.saveAndFlush(freshLog);

                log.info("Telegram message sent - Chat: {}, Batch: {}", 
                         message.getRecipient(), message.getBatchId());
                
                return; // Success, exit method
                
            } catch (org.springframework.orm.ObjectOptimisticLockingFailureException e) {
                log.warn("Optimistic locking conflict, attempt {}/{}: {}", 
                         attempt + 1, maxRetries, e.getMessage());
                
                if (attempt == maxRetries - 1) {
                    log.error("Failed to update notification log after {} retries", maxRetries);
                    updateLogAsFailed(notificationLog, "Optimistic locking failure after retries");
                }
                
                // Wait before retrying
                try {
                    Thread.sleep(100 * (attempt + 1)); // Exponential backoff
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
                
            } catch (Exception e) {
                log.error("Failed to send Telegram: {}", e.getMessage(), e);
                updateLogAsFailed(notificationLog, e.getMessage());
                return; // Exit on other errors
            }
        }
    }

    private String formatTelegramMessage(NotificationMessage message) {
        StringBuilder formatted = new StringBuilder();
        formatted.append("<b>🔔 ").append(message.getType()).append("</b>\n\n");
        
        if (message.getSubject() != null) {
            formatted.append("<b>").append(message.getSubject()).append("</b>\n\n");
        }
        
        formatted.append(message.getMessage());
        formatted.append("\n\n<i>From: ").append(message.getSystemName()).append("</i>");
        
        return formatted.toString();
    }

    private void updateLogAsFailed(NotificationLog notificationLog, String error) {
        int maxRetries = 3;
        
        for (int attempt = 0; attempt < maxRetries; attempt++) {
            try {
                // ✅ FIX: Always fetch fresh entity
                NotificationLog freshLog = logRepository.findById(notificationLog.getId())
                    .orElseThrow(() -> new RuntimeException("Log not found"));
                
                freshLog.setStatus(NotificationStatus.FAILED);
                freshLog.setErrorMessage(error);
                freshLog.setRetryCount(freshLog.getRetryCount() + 1);
                logRepository.saveAndFlush(freshLog);
                
                return; // Success
                
            } catch (org.springframework.orm.ObjectOptimisticLockingFailureException e) {
                log.warn("Failed to update log as failed, attempt {}/{}", attempt + 1, maxRetries);
                
                if (attempt < maxRetries - 1) {
                    try {
                        Thread.sleep(100 * (attempt + 1));
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }
            } catch (Exception e) {
                log.error("Critical error updating notification log: {}", e.getMessage());
                return;
            }
        }
    }
}