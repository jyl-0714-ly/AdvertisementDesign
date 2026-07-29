package com.advertisementdesign.back.project.repository;

import com.advertisementdesign.back.project.entity.CustomerProjectMemberEntity;
import com.advertisementdesign.back.project.enums.CustomerProjectMemberStatus;

import java.util.List;
import java.util.Optional;

public interface CustomerProjectMemberRepository {
    CustomerProjectMemberEntity save(CustomerProjectMemberEntity member);

    Optional<CustomerProjectMemberEntity> findActiveByProjectAndOrganizationMember(
            Long projectId, Long organizationId, Long organizationMemberId);

    List<CustomerProjectMemberEntity> findByProject(Long projectId, CustomerProjectMemberStatus status);
}
