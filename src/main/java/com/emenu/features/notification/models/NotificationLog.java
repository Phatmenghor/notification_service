package com.emenu.features.notification.models;

import com.emenu.enums.notification.NotificationChannel;
import com.emenu.enums.notification.NotificationStatus;
import com.emenu.enums.notification.NotificationType;
import com.emenu.shared.domain.BaseUUIDEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification_logs", indexes = {
    @Index(name = "idx_notif_log_api_key", columnList = "api_key_value"),
    @Index(name = "idx_notif_log_channel", columnList = "channel"),
    @Index(name = "idx_notif_log_status", columnList = "status"),
    @Index(name = "idx_notif_log_created", columnList = "created_at"),
    @Index(name = "idx_notif_log_batch", columnList = "batch_id")
})
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class NotificationLog extends BaseUUIDEntity {

    @Column(name = "batch_id")
    private String batchId;

    @Column(name = "api_key_value", nullable = false)
    private String apiKeyValue;

    @Column(name = "system_name")
    private String systemName;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false)
    private NotificationChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private NotificationType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private NotificationStatus status;

    @Column(name = "recipient", nullable = false)
    private String recipient;

    @Column(name = "subject")
    private String subject;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @Column(name = "response", columnDefinition = "TEXT")
    private String response;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "retry_count")
    private Integer retryCount = 0;
}