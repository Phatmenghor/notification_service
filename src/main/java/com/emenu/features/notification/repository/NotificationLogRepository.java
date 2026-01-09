
package com.emenu.features.notification.repository;

import com.emenu.enums.notification.NotificationChannel;
import com.emenu.enums.notification.NotificationStatus;
import com.emenu.features.notification.models.NotificationLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.UUID;

@Repository
public interface NotificationLogRepository extends JpaRepository<NotificationLog, UUID> {
    
    Page<NotificationLog> findByApiKeyValueAndIsDeletedFalse(String apiKeyValue, Pageable pageable);
    
    Page<NotificationLog> findByBatchIdAndIsDeletedFalse(String batchId, Pageable pageable);
    
    @Query("SELECT nl FROM NotificationLog nl WHERE nl.isDeleted = false " +
           "AND (:apiKeyValue IS NULL OR nl.apiKeyValue = :apiKeyValue) " +
           "AND (:channel IS NULL OR nl.channel = :channel) " +
           "AND (:status IS NULL OR nl.status = :status) " +
           "AND (:startDate IS NULL OR nl.createdAt >= :startDate) " +
           "AND (:endDate IS NULL OR nl.createdAt <= :endDate)")
    Page<NotificationLog> searchLogs(
        @Param("apiKeyValue") String apiKeyValue,
        @Param("channel") NotificationChannel channel,
        @Param("status") NotificationStatus status,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        Pageable pageable
    );

    @Query("SELECT COUNT(nl) FROM NotificationLog nl WHERE nl.apiKeyValue = :apiKeyValue " +
           "AND nl.createdAt >= :startDate AND nl.isDeleted = false")
    Long countUsageForPeriod(@Param("apiKeyValue") String apiKeyValue, 
                             @Param("startDate") LocalDateTime startDate);
}