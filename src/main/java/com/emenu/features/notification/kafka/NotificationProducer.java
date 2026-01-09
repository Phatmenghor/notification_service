package com.emenu.features.notification.kafka;

import com.emenu.features.notification.dto.NotificationMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${notification.kafka.topics.telegram}")
    private String telegramTopic;

    @Value("${notification.kafka.topics.email}")
    private String emailTopic;

    public void sendTelegramNotification(NotificationMessage message) {
        try {
            log.info("Publishing Telegram notification - Batch: {}, Recipient: {}", 
                     message.getBatchId(), message.getRecipient());
            
            CompletableFuture<SendResult<String, Object>> future = 
                kafkaTemplate.send(telegramTopic, message.getBatchId(), message);
            
            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("Telegram message sent to Kafka - Partition: {}, Offset: {}", 
                             result.getRecordMetadata().partition(), 
                             result.getRecordMetadata().offset());
                } else {
                    log.error("Failed to send Telegram message to Kafka: {}", ex.getMessage());
                }
            });
            
        } catch (Exception e) {
            log.error("Error publishing Telegram notification: {}", e.getMessage(), e);
        }
    }

    public void sendEmailNotification(NotificationMessage message) {
        try {
            log.info("Publishing Email notification - Batch: {}, Recipient: {}", 
                     message.getBatchId(), message.getRecipient());
            
            CompletableFuture<SendResult<String, Object>> future = 
                kafkaTemplate.send(emailTopic, message.getBatchId(), message);
            
            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("Email message sent to Kafka - Partition: {}, Offset: {}", 
                             result.getRecordMetadata().partition(), 
                             result.getRecordMetadata().offset());
                } else {
                    log.error("Failed to send Email message to Kafka: {}", ex.getMessage());
                }
            });
            
        } catch (Exception e) {
            log.error("Error publishing Email notification: {}", e.getMessage(), e);
        }
    }
}