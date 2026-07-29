package com.advertisementdesign.back.common.storage.repository;

import com.advertisementdesign.back.common.exception.ApiErrorCode;
import com.advertisementdesign.back.common.exception.ApiException;
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
        int affected = fileAsset.getId() == null
                ? fileAssetMapper.insert(fileAsset)
                : fileAssetMapper.updateById(fileAsset);
        if (affected != 1) {
            throw new ApiException(ApiErrorCode.CONFLICT.getCode(), "文件状态已变化，请刷新后重试");
        }
        return fileAsset;
    }

    public boolean claimFirstRequirementDraft(FileAssetEntity fileAsset,
                                               Long actorId,
                                               Long organizationId,
                                               Long projectId) {
        return fileAssetMapper.claimFirstRequirementDraft(
                fileAsset.getId(), actorId, organizationId, projectId, fileAsset.getVersion()) == 1;
    }

    public boolean claimProjectMessageDraft(FileAssetEntity fileAsset,
                                            String actorType,
                                            Long actorId,
                                            Long organizationId,
                                            Long projectId) {
        return fileAssetMapper.claimProjectMessageDraft(
                fileAsset.getId(), actorType, actorId, organizationId, projectId, fileAsset.getVersion()) == 1;
    }

    public boolean claimProjectArtifactDraft(FileAssetEntity fileAsset,
                                             String actorType,
                                             Long actorId,
                                             Long organizationId,
                                             Long projectId) {
        return fileAssetMapper.claimProjectArtifactDraft(
                fileAsset.getId(), actorType, actorId, organizationId, projectId, fileAsset.getVersion()) == 1;
    }

    public Optional<FileAssetEntity> findById(Long id) {
        return Optional.ofNullable(fileAssetMapper.selectById(id));
    }
}
