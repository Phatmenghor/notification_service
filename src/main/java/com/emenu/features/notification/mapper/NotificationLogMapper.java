package com.emenu.features.notification.mapper;

import com.emenu.features.notification.dto.response.NotificationLogResponse;
import com.emenu.features.notification.models.NotificationLog;
import com.emenu.shared.dto.PaginationResponse;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.springframework.data.domain.Page;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface NotificationLogMapper {
    
    NotificationLogResponse toResponse(NotificationLog notificationLog);
    
    default PaginationResponse<NotificationLogResponse> toPaginationResponse(Page<NotificationLog> page) {
        return PaginationResponse.<NotificationLogResponse>builder()
            .content(page.getContent().stream().map(this::toResponse).toList())
            .pageNo(page.getNumber())
            .pageSize(page.getSize())
            .totalElements(page.getTotalElements())
            .totalPages(page.getTotalPages())
            .last(page.isLast())
            .build();
    }
}