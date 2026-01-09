package com.emenu.features.notification.kafka;

import com.emenu.enums.notification.NotificationStatus;
import com.emenu.features.notification.dto.NotificationMessage;
import com.emenu.features.notification.models.NotificationLog;
import com.emenu.features.notification.repository.NotificationLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Properties;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationConsumer {

    private final NotificationLogRepository logRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(
        topics = "email-notifications",
        groupId = "${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeEmailNotification(
            @Payload String payload,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {
        
        try {
            log.info("Processing Email notification - Partition: {}, Offset: {}", partition, offset);
            
            NotificationMessage message = objectMapper.readValue(payload, NotificationMessage.class);
            
            // Add retry mechanism with exponential backoff for finding the log
            NotificationLog notificationLog = findNotificationLogWithRetry(message.getLogId(), 5, 200);
            
            if (notificationLog == null) {
                log.error("Failed to find notification log after retries: {}", message.getLogId());
                acknowledgment.acknowledge();
                return;
            }
            
            sendEmail(message, notificationLog);
            acknowledgment.acknowledge();
            
        } catch (Exception e) {
            log.error("Error processing Email notification: {}", e.getMessage(), e);
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

    private void sendEmail(NotificationMessage message, NotificationLog notificationLog) {
        try {
            notificationLog.setStatus(NotificationStatus.PROCESSING);
            logRepository.saveAndFlush(notificationLog);

            JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
            mailSender.setHost(message.getEmailSmtpHost());
            mailSender.setPort(message.getEmailSmtpPort());
            mailSender.setUsername(message.getEmailSmtpUsername());
            mailSender.setPassword(message.getEmailSmtpPassword());

            Properties props = mailSender.getJavaMailProperties();
            props.put("mail.transport.protocol", "smtp");
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", Boolean.TRUE.equals(message.getEmailUseTLS()) ? "true" : "false");
            props.put("mail.smtp.ssl.enable", Boolean.TRUE.equals(message.getEmailUseSSL()) ? "true" : "false");

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(message.getEmailFrom());
            helper.setTo(message.getRecipient());
            helper.setSubject(message.getSubject() != null ? message.getSubject() : "Notification");
            helper.setText(formatEmailMessage(message), true);

            mailSender.send(mimeMessage);

            notificationLog.setStatus(NotificationStatus.SENT);
            notificationLog.setResponse("Email sent successfully");
            notificationLog.setSentAt(LocalDateTime.now());
            logRepository.saveAndFlush(notificationLog);

            log.info("Email sent - To: {}, Batch: {}", message.getRecipient(), message.getBatchId());

        } catch (MessagingException e) {
            log.error("Failed to send email: {}", e.getMessage(), e);
            updateLogAsFailed(notificationLog, e.getMessage());
        }
    }

    private String formatEmailMessage(NotificationMessage message) {
        return String.format("""
            <html>
            <body style="font-family: Arial, sans-serif;">
                <div style="background-color: #f5f5f5; padding: 20px;">
                    <div style="background-color: white; padding: 20px; border-radius: 5px;">
                        <h2 style="color: #333;">%s - %s</h2>
                        <hr style="border: 1px solid #eee;"/>
                        <div style="margin: 20px 0;">
                            %s
                        </div>
                        <hr style="border: 1px solid #eee;"/>
                        <p style="color: #666; font-size: 12px; margin-top: 20px;">
                            <i>Sent from: %s</i>
                        </p>
                    </div>
                </div>
            </body>
            </html>
            """,
            message.getType(),
            message.getSubject() != null ? message.getSubject() : "Notification",
            message.getMessage().replace("\n", "<br/>"),
            message.getSystemName()
        );
    }

    private void updateLogAsFailed(NotificationLog notificationLog, String error) {
        try {
            notificationLog.setStatus(NotificationStatus.FAILED);
            notificationLog.setErrorMessage(error);
            notificationLog.setRetryCount(notificationLog.getRetryCount() + 1);
            logRepository.saveAndFlush(notificationLog);
        } catch (Exception e) {
            log.error("Failed to update notification log: {}", e.getMessage());
        }
    }
}