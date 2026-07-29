package com.advertisementdesign.back.project.entity;

import com.advertisementdesign.back.project.enums.CustomerProjectMemberStatus;
import com.advertisementdesign.back.project.enums.CustomerProjectRole;

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
@TableName("customer_project_member")
public class CustomerProjectMemberEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long projectId;
    private Long organizationId;
    private Long organizationMemberId;
    private CustomerProjectRole projectRole;
    private Boolean canConfirmRequirement;
    private Boolean canConfirmReport;
    private Boolean canConfirmDesign;
    private Boolean canSignContract;
    private Boolean canManagePayment;
    private Boolean canReceiveDelivery;
    private CustomerProjectMemberStatus status;
    @Version
    private Long version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
