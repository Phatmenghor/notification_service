package com.emenu.features.notification.repository;

import com.emenu.features.notification.models.ApiKey;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, UUID> {
    
    Optional<ApiKey> findByApiKeyValueAndIsActiveTrueAndIsDeletedFalse(String apiKeyValue);
    
    Optional<ApiKey> findByApiKeyValueAndIsDeletedFalse(String apiKeyValue);
    
    Page<ApiKey> findAllByIsDeletedFalse(Pageable pageable);
    
    List<ApiKey> findAllByIsDeletedFalse();
    
    boolean existsByApiKeyValueAndIsDeletedFalse(String apiKeyValue);
    
    boolean existsBySystemNameAndIsDeletedFalse(String systemName);

    @Query("SELECT ak FROM ApiKey ak WHERE ak.isDeleted = false " +
           "AND ak.usageResetDate IS NOT NULL " +
           "AND ak.usageResetDate < :now")
    List<ApiKey> findKeysNeedingUsageReset(LocalDateTime now);
}