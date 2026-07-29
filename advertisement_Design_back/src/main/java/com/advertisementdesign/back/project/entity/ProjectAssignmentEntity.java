package com.advertisementdesign.back.project.entity;

import com.advertisementdesign.back.identity.model.ActorRef;
import com.advertisementdesign.back.project.enums.ProjectAssignmentRole;
import com.advertisementdesign.back.project.enums.ProjectAssignmentStatus;
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
@TableName("project_assignment")
public class ProjectAssignmentEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long projectId;
    private Long designerUserId;
    private ProjectAssignmentRole assignmentRole;
    private String authorizationScope;
    private ProjectAssignmentStatus status;
    private ActorRef.ActorType initiatedByActorType;
    private Long initiatedByActorId;
    private LocalDateTime acceptedAt;
    private LocalDateTime effectiveFrom;
    private LocalDateTime effectiveTo;
    @Version
    private Long version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
