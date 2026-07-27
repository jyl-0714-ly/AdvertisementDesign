package com.advertisementdesign.back.common.storage.converter;

import com.advertisementdesign.back.common.storage.entity.FileAssetEntity;
import com.advertisementdesign.back.common.storage.model.FileModels;
import org.springframework.stereotype.Component;

@Component
public class FileConverter {
    public FileModels.FileAssetVO toVO(FileAssetEntity entity) {
        return new FileModels.FileAssetVO(
                entity.getId(),
                entity.getOriginalName(),
                entity.getStorageName(),
                entity.getStorageProvider(),
                entity.getBucketName(),
                entity.getObjectKey(),
                entity.getUrl(),
                entity.getMimeType(),
                entity.getFileSize(),
                entity.getFileHash(),
                entity.getStatus(),
                entity.getCreatedAt() == null ? null : entity.getCreatedAt().toString(),
                entity.getUpdatedAt() == null ? null : entity.getUpdatedAt().toString()
        );
    }
}
