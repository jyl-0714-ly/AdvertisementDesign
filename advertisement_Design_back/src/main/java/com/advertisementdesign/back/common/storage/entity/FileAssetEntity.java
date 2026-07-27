package com.advertisementdesign.back.common.storage.entity;

import com.advertisementdesign.back.common.storage.enums.FileStatus;
import com.advertisementdesign.back.common.storage.enums.StorageProvider;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
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
    private Long uploaderId;
    private String originalName;
    private String storageName;
    private StorageProvider storageProvider;
    private String bucketName;
    private String objectKey;
    private String url;
    private String mimeType;
    private Long fileSize;
    private String fileHash;
    private FileStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
