package com.advertisementdesign.back.artifact.repository;

import com.advertisementdesign.back.artifact.entity.*;
import com.advertisementdesign.back.artifact.enums.ArtifactEnums.ApprovalDecision;
import com.advertisementdesign.back.artifact.enums.ArtifactEnums.ArtifactType;
import com.advertisementdesign.back.artifact.enums.ArtifactEnums.ConfirmationResult;
import com.advertisementdesign.back.artifact.mapper.*;
import com.advertisementdesign.back.common.exception.ApiErrorCode;
import com.advertisementdesign.back.common.exception.ApiException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;


import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ArtifactRepository {
    private final ArtifactMapper artifactMapper;
    private final ArtifactVersionMapper versionMapper;
    private final ArtifactApprovalMapper approvalMapper;
    private final ArtifactConfirmationMapper confirmationMapper;
    private final ArtifactVersionFileMapper versionFileMapper;
    private final ArtifactAnnotationMapper annotationMapper;

    public ArtifactEntity insert(ArtifactEntity entity) { requireOne(artifactMapper.insert(entity)); return entity; }
    public Optional<ArtifactEntity> findArtifact(Long projectId, Long artifactId) {
        return Optional.ofNullable(artifactMapper.selectOne(new LambdaQueryWrapper<ArtifactEntity>()
                .eq(ArtifactEntity::getId, artifactId).eq(ArtifactEntity::getProjectId, projectId).last("LIMIT 1")));
    }
    public Optional<ArtifactVersionEntity> findVersion(Long projectId, Long artifactId, Long versionId) {
        return Optional.ofNullable(versionMapper.selectOne(new LambdaQueryWrapper<ArtifactVersionEntity>()
                .eq(ArtifactVersionEntity::getId, versionId).eq(ArtifactVersionEntity::getArtifactId, artifactId)
                .eq(ArtifactVersionEntity::getProjectId, projectId).last("LIMIT 1")));
    }
    public ArtifactVersionEntity insertVersion(ArtifactVersionEntity entity) { requireOne(versionMapper.insert(entity)); return entity; }
    public void updateDraftVersion(ArtifactVersionEntity entity) {
        if (versionMapper.updateById(entity) != 1) throw conflict();
    }
    public void updateArtifact(ArtifactEntity entity) { if (artifactMapper.updateById(entity) != 1) throw conflict(); }
    public boolean hasApproval(Long projectId, Long versionId) {
        return approvalMapper.selectCount(new LambdaQueryWrapper<ArtifactApprovalEntity>()
                .eq(ArtifactApprovalEntity::getProjectId, projectId)
                .eq(ArtifactApprovalEntity::getArtifactVersionId, versionId)
                .eq(ArtifactApprovalEntity::getDecision, ApprovalDecision.APPROVED)) > 0;
    }
    public ArtifactApprovalEntity insertApproval(ArtifactApprovalEntity entity) { requireOne(approvalMapper.insert(entity)); return entity; }
    public Optional<ArtifactConfirmationEntity> findConfirmation(Long projectId, String key) {
        return Optional.ofNullable(confirmationMapper.selectOne(new LambdaQueryWrapper<ArtifactConfirmationEntity>()
                .eq(ArtifactConfirmationEntity::getProjectId, projectId)
                .eq(ArtifactConfirmationEntity::getIdempotencyKey, key).last("LIMIT 1")));
    }
    public boolean confirmOnce(ArtifactEntity artifact, Long expectedVersion, ArtifactConfirmationEntity confirmation) {
        int changed = artifactMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<ArtifactEntity>()
                .eq(ArtifactEntity::getId, artifact.getId()).eq(ArtifactEntity::getProjectId, artifact.getProjectId())
                .eq(ArtifactEntity::getVersion, expectedVersion).set(ArtifactEntity::getVersion, expectedVersion + 1)
                .set(ArtifactEntity::getUpdatedAt, java.time.LocalDateTime.now()));
        if (changed != 1) return false;
        insertConfirmation(confirmation);
        artifact.setVersion(expectedVersion + 1);
        return true;
    }

    public ArtifactConfirmationEntity insertConfirmation(ArtifactConfirmationEntity entity) {
        try { requireOne(confirmationMapper.insert(entity)); return entity; }
        catch (DuplicateKeyException exception) { throw conflict(); }
    }
    public List<ArtifactConfirmationEntity> confirmed(Long projectId, Long versionId) {
        return confirmationMapper.selectList(new LambdaQueryWrapper<ArtifactConfirmationEntity>()
                .eq(ArtifactConfirmationEntity::getProjectId, projectId)
                .eq(ArtifactConfirmationEntity::getArtifactVersionId, versionId)
                .eq(ArtifactConfirmationEntity::getResult, com.advertisementdesign.back.artifact.enums.ArtifactEnums.ConfirmationResult.CONFIRMED));
    }
    public Optional<ConfirmedVersion> findConfirmedVersion(Long projectId, ArtifactType type, Long versionId) {
        ArtifactVersionEntity version = versionMapper.selectOne(new LambdaQueryWrapper<ArtifactVersionEntity>()
                .eq(ArtifactVersionEntity::getId, versionId).eq(ArtifactVersionEntity::getProjectId, projectId).last("LIMIT 1"));
        if (version == null) return Optional.empty();
        ArtifactEntity artifact = artifactMapper.selectOne(new LambdaQueryWrapper<ArtifactEntity>()
                .eq(ArtifactEntity::getId, version.getArtifactId()).eq(ArtifactEntity::getProjectId, projectId)
                .eq(ArtifactEntity::getArtifactType, type).last("LIMIT 1"));
        if (artifact == null || confirmed(projectId, versionId).isEmpty()) return Optional.empty();
        return Optional.of(new ConfirmedVersion(artifact.getId(), version.getId(), version.getVersionNumber()));
    }

    public Optional<ConfirmedVersion> findLatestConfirmedVersion(Long projectId, ArtifactType type) {
        List<ArtifactConfirmationEntity> confirmations = confirmationMapper.selectList(
                new LambdaQueryWrapper<ArtifactConfirmationEntity>()
                        .eq(ArtifactConfirmationEntity::getProjectId, projectId)
                        .eq(ArtifactConfirmationEntity::getResult, ConfirmationResult.CONFIRMED)
                        .orderByDesc(ArtifactConfirmationEntity::getConfirmedAt));
        for (ArtifactConfirmationEntity confirmation : confirmations) {
            ArtifactEntity artifact = artifactMapper.selectOne(new LambdaQueryWrapper<ArtifactEntity>()
                    .eq(ArtifactEntity::getId, confirmation.getArtifactId())
                    .eq(ArtifactEntity::getProjectId, projectId)
                    .eq(ArtifactEntity::getArtifactType, type)
                    .last("LIMIT 1"));
            if (artifact != null) {
                return Optional.of(new ConfirmedVersion(
                        artifact.getId(), confirmation.getArtifactVersionId(), confirmation.getArtifactVersionNumber()));
            }
        }
        return Optional.empty();
    }

    public record ConfirmedVersion(Long artifactId, Long versionId, Integer versionNumber) {}

    public boolean fileBelongsToVersion(Long projectId, Long versionId, Long fileId) {
        ArtifactVersionEntity version = versionMapper.selectById(versionId);
        return version != null && projectId.equals(version.getProjectId())
                && versionFileMapper.selectCount(new LambdaQueryWrapper<ArtifactVersionFileEntity>()
                .eq(ArtifactVersionFileEntity::getArtifactVersionId, versionId)
                .eq(ArtifactVersionFileEntity::getFileAssetId, fileId)) > 0;
    }
    public ArtifactVersionFileEntity insertFile(ArtifactVersionFileEntity entity) { requireOne(versionFileMapper.insert(entity)); return entity; }
    public ArtifactAnnotationEntity insertAnnotation(ArtifactAnnotationEntity entity) { requireOne(annotationMapper.insert(entity)); return entity; }

    private void requireOne(int affected) { if (affected != 1) throw conflict(); }
    private ApiException conflict() { return new ApiException(ApiErrorCode.CONFLICT.getCode(), "产物状态已变化，请刷新后重试"); }
}
