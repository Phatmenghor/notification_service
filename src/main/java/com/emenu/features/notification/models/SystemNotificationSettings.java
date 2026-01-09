package com.emenu.features.notification.models;

import com.emenu.shared.domain.BaseUUIDEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "system_notification_settings", indexes = {
    @Index(name = "idx_sys_notif_setting_key", columnList = "setting_key", unique = true)
})
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class SystemNotificationSettings extends BaseUUIDEntity {

    @Column(name = "setting_key", unique = true, nullable = false)
    private String settingKey = "DEFAULT";

    // Telegram Settings
    @Column(name = "telegram_enabled")
    private Boolean telegramEnabled = false;

    @Column(name = "telegram_bot_token", length = 500)
    private String telegramBotToken;

    @Column(name = "telegram_chat_id", length = 200)
    private String telegramChatId;

    // Email Settings
    @Column(name = "email_enabled")
    private Boolean emailEnabled = false;

    @Column(name = "email_from")
    private String emailFrom;

    @Column(name = "email_to")
    private String emailTo;

    @Column(name = "email_smtp_host")
    private String emailSmtpHost;

    @Column(name = "email_smtp_port")
    private Integer emailSmtpPort;

    @Column(name = "email_smtp_username")
    private String emailSmtpUsername;

    @Column(name = "email_smtp_password")
    private String emailSmtpPassword;

    @Column(name = "email_use_ssl")
    private Boolean emailUseSSL = false;

    @Column(name = "email_use_tls")
    private Boolean emailUseTLS = true;
}