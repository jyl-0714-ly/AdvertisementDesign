package com.advertisementdesign.back.project.entity;

import com.advertisementdesign.back.project.enums.FileRole;
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
@TableName("project_file")
public class ProjectFileEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long projectId;
    private Long projectStageId;
    private String stageCode;
    private Long fileId;
    private Long uploaderId;
    private FileRole fileRole;
    private String description;
    private LocalDateTime createdAt;
}
