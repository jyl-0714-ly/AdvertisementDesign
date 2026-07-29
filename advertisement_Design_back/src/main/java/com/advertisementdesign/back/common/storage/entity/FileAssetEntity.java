package com.advertisementdesign.back.common.storage.entity;

import com.advertisementdesign.back.common.storage.enums.FileBusinessScope;
import com.advertisementdesign.back.common.storage.enums.FileStatus;
import com.advertisementdesign.back.common.storage.enums.StorageProvider;
import com.advertisementdesign.back.common.storage.enums.StorageVisibility;
import com.advertisementdesign.back.common.storage.enums.StorageZone;
import com.advertisementdesign.back.identity.model.ActorRef;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("file_asset")
public class FileAssetEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private ActorRef.ActorType uploaderActorType;
    private Long uploaderActorId;
    private Long organizationId;
    private Long projectId;
    private FileBusinessScope businessScope;
    private StorageVisibility visibility;
    private String originalName;
    private StorageProvider storageProvider;
    private StorageZone storageZone;
    private String bucketName;
    private String objectKey;
    private String storageRegion;
    private String storageClass;
    private String mimeType;
    private String fileExtension;
    private Long fileSize;
    private String hashAlgorithm;
    private String fileHash;
    private String encryptionKeyRef;
    private LocalDateTime retentionUntil;
    private Boolean legalHold;
    private FileStatus status;
    @Version
    private Long version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
