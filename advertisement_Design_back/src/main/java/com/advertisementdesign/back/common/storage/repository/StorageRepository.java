package com.advertisementdesign.back.common.storage.repository;

import com.advertisementdesign.back.common.storage.entity.FileAssetEntity;
import com.advertisementdesign.back.common.storage.mapper.FileAssetMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class StorageRepository {
    private final FileAssetMapper fileAssetMapper;

    public FileAssetEntity save(FileAssetEntity fileAsset) {
        if (fileAsset.getId() == null) {
            fileAssetMapper.insert(fileAsset);
        } else {
            fileAssetMapper.updateById(fileAsset);
        }
        return fileAsset;
    }

    public Optional<FileAssetEntity> findById(Long id) {
        return Optional.ofNullable(fileAssetMapper.selectById(id));
    }
}
