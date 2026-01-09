package com.emenu.features.notification.mapper;

import com.emenu.features.notification.dto.request.CreateApiKeyRequest;
import com.emenu.features.notification.dto.response.ApiKeyResponse;
import com.emenu.features.notification.models.ApiKey;
import com.emenu.shared.dto.PaginationResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.springframework.data.domain.Page;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ApiKeyMapper {
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "apiKeyValue", ignore = true)
    @Mapping(target = "isActive", ignore = true)
    @Mapping(target = "currentUsage", ignore = true)
    @Mapping(target = "usageResetDate", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    ApiKey toEntity(CreateApiKeyRequest request);
    
    @Mapping(target = "isExpired", expression = "java(apiKey.isExpired())")
    @Mapping(target = "isUnlimited", expression = "java(apiKey.getMonthlyLimit() == null)")
    ApiKeyResponse toResponse(ApiKey apiKey);
    
    default PaginationResponse<ApiKeyResponse> toPaginationResponse(Page<ApiKey> page) {
        return PaginationResponse.<ApiKeyResponse>builder()
            .content(page.getContent().stream().map(this::toResponse).toList())
            .pageNo(page.getNumber())
            .pageSize(page.getSize())
            .totalElements(page.getTotalElements())
            .totalPages(page.getTotalPages())
            .last(page.isLast())
            .build();
    }
}