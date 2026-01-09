package com.emenu.features.notification.mapper;

import com.emenu.features.notification.dto.response.SystemSettingsResponse;
import com.emenu.features.notification.models.SystemNotificationSettings;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface SystemSettingsMapper {
    
    @Mapping(target = "telegramConfigured", expression = "java(isTelegramConfigured(settings))")
    @Mapping(target = "emailConfigured", expression = "java(isEmailConfigured(settings))")
    SystemSettingsResponse toResponse(SystemNotificationSettings settings);
    
    default boolean isTelegramConfigured(SystemNotificationSettings settings) {
        return settings.getTelegramBotToken() != null && 
               !settings.getTelegramBotToken().isBlank() &&
               settings.getTelegramChatId() != null &&
               !settings.getTelegramChatId().isBlank();
    }
    
    default boolean isEmailConfigured(SystemNotificationSettings settings) {
        return settings.getEmailFrom() != null &&
               !settings.getEmailFrom().isBlank() &&
               settings.getEmailTo() != null &&
               !settings.getEmailTo().isBlank() &&
               settings.getEmailSmtpHost() != null &&
               !settings.getEmailSmtpHost().isBlank() &&
               settings.getEmailSmtpPort() != null;
    }
}