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
            sendToTelegram(message);
            
            acknowledgment.acknowledge();
            
        } catch (Exception e) {
            log.error("Error processing Telegram notification: {}", e.getMessage(), e);
            acknowledgment.acknowledge();
        }
    }

    private void sendToTelegram(NotificationMessage message) {
        try {
            NotificationLog notificationLog = logRepository.findById(message.getLogId())
                .orElseThrow(() -> new RuntimeException("Notification log not found"));

            notificationLog.setStatus(NotificationStatus.PROCESSING);
            logRepository.save(notificationLog);

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

            notificationLog.setStatus(NotificationStatus.SENT);
            notificationLog.setResponse(response);
            notificationLog.setSentAt(LocalDateTime.now());
            logRepository.save(notificationLog);

            log.info("Telegram message sent - Chat: {}, Batch: {}", 
                     message.getRecipient(), message.getBatchId());

        } catch (Exception e) {
            log.error("Failed to send Telegram: {}", e.getMessage(), e);
            updateLogAsFailed(message.getLogId(), e.getMessage());
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

    private void updateLogAsFailed(java.util.UUID logId, String error) {
        try {
            NotificationLog notificationLog = logRepository.findById(logId).orElse(null);
            if (notificationLog != null) {
                notificationLog.setStatus(NotificationStatus.FAILED);
                notificationLog.setErrorMessage(error);
                notificationLog.setRetryCount(notificationLog.getRetryCount() + 1);
                logRepository.save(notificationLog);
            }
        } catch (Exception e) {
            log.error("Failed to update notification log: {}", e.getMessage());
        }
    }
}