package com.advertisementdesign.back.common.storage.mapper;

import com.advertisementdesign.back.common.storage.entity.FileAssetEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface FileAssetMapper extends BaseMapper<FileAssetEntity> {
    @Update("""
            UPDATE file_asset
               SET project_id = #{projectId},
                   business_scope = 'PROJECT_COMMUNICATION',
                   visibility = 'PROJECT',
                   updated_at = CURRENT_TIMESTAMP(3),
                   version = version + 1
             WHERE id = #{fileId}
               AND uploader_actor_type = #{actorType}
               AND uploader_actor_id = #{actorId}
               AND organization_id = #{organizationId}
               AND business_scope = 'PRIVATE_DRAFT'
               AND project_id IS NULL
               AND status = 'ACTIVE'
               AND version = #{expectedVersion}
            """)
    int claimProjectMessageDraft(@Param("fileId") Long fileId,
                                 @Param("actorType") String actorType,
                                 @Param("actorId") Long actorId,
                                 @Param("organizationId") Long organizationId,
                                 @Param("projectId") Long projectId,
                                 @Param("expectedVersion") Long expectedVersion);

    @Update("""
            UPDATE file_asset
               SET project_id = #{projectId},
                   business_scope = 'PROJECT_ARTIFACT',
                   visibility = 'PROJECT',
                   updated_at = CURRENT_TIMESTAMP(3),
                   version = version + 1
             WHERE id = #{fileId}
               AND uploader_actor_type = #{actorType}
               AND uploader_actor_id = #{actorId}
               AND organization_id = #{organizationId}
               AND business_scope = 'PRIVATE_DRAFT'
               AND storage_zone = 'PRIVATE'
               AND project_id IS NULL
               AND status = 'ACTIVE'
               AND version = #{expectedVersion}
            """)
    int claimProjectArtifactDraft(@Param("fileId") Long fileId,
                                  @Param("actorType") String actorType,
                                  @Param("actorId") Long actorId,
                                  @Param("organizationId") Long organizationId,
                                  @Param("projectId") Long projectId,
                                  @Param("expectedVersion") Long expectedVersion);

    @Update("""
            UPDATE file_asset
               SET organization_id = #{organizationId},
                   project_id = #{projectId},
                   business_scope = 'PROJECT_COMMUNICATION',
                   visibility = 'PROJECT',
                   updated_at = CURRENT_TIMESTAMP(3),
                   version = version + 1
             WHERE id = #{fileId}
               AND uploader_actor_type = 'CUSTOMER_USER'
               AND uploader_actor_id = #{actorId}
               AND business_scope = 'PRIVATE_DRAFT'
               AND project_id IS NULL
               AND status = 'ACTIVE'
               AND version = #{expectedVersion}
            """)
    int claimFirstRequirementDraft(@Param("fileId") Long fileId,
                                   @Param("actorId") Long actorId,
                                   @Param("organizationId") Long organizationId,
                                   @Param("projectId") Long projectId,
                                   @Param("expectedVersion") Long expectedVersion);
}
