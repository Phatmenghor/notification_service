package com.emenu.features.notification.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class TelegramConfig {
    
    @NotBlank(message = "Bot token is required")
    private String botToken;
    
    @NotEmpty(message = "At least one chat ID is required")
    private List<String> chatIds;

    // Custom formatted message body - if provided, this replaces the default telegram template entirely
    // Supports full Telegram HTML: <b>, <i>, <u>, <s>, <code>, <pre>, <a href="">, <tg-spoiler>, etc.
    private String htmlBody;
}