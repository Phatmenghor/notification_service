package com.emenu.features.setting.models;

import com.emenu.shared.domain.BaseUUIDEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "images", indexes = {
        @Index(name = "idx_image_entity_deleted", columnList = "is_deleted"),
        @Index(name = "idx_image_entity_deleted_created", columnList = "is_deleted, created_at"),

        @Index(name = "idx_image_entity_type_deleted", columnList = "type, is_deleted"),
        @Index(name = "idx_image_entity_type_created_deleted", columnList = "type, created_at, is_deleted")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImageEntity extends BaseUUIDEntity {

    @Column(name = "type")
    private String type;
    
    @Lob
    @Column(name = "data", columnDefinition = "TEXT")
    private String data; // Base64 encoded string
}