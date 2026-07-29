package com.advertisementdesign.back.project.entity;

import com.advertisementdesign.back.project.enums.ProjectNameSource;
import com.advertisementdesign.back.project.enums.ProjectStatus;
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
@TableName("project")
public class ProjectEntity {
    public static final String INITIAL_NAME = "新项目需求沟通";

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long organizationId;
    @Builder.Default
    private String name = INITIAL_NAME;
    @Builder.Default
    private ProjectNameSource nameSource = ProjectNameSource.AUTO;
    private String description;
    @Builder.Default
    private ProjectStatus status = ProjectStatus.ACTIVE;
    private Long confirmedRequirementVersionId;
    private LocalDateTime startedAt;
    private LocalDateTime pausedAt;
    private LocalDateTime completedAt;
    private LocalDateTime terminatedAt;
    @Version
    @Builder.Default
    private Long version = 0L;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
