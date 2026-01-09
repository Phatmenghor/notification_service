package com.emenu.features.notification.repository;

import com.emenu.features.notification.models.SystemNotificationSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SystemNotificationSettingsRepository extends JpaRepository<SystemNotificationSettings, UUID> {
    
    Optional<SystemNotificationSettings> findBySettingKeyAndIsDeletedFalse(String settingKey);
}