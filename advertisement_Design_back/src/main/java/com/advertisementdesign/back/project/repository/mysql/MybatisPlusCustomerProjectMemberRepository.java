package com.advertisementdesign.back.project.repository.mysql;

import com.advertisementdesign.back.common.exception.ApiErrorCode;
import com.advertisementdesign.back.common.exception.ApiException;
import com.advertisementdesign.back.project.entity.CustomerProjectMemberEntity;
import com.advertisementdesign.back.project.enums.CustomerProjectMemberStatus;
import com.advertisementdesign.back.project.mapper.CustomerProjectMemberMapper;
import com.advertisementdesign.back.project.repository.CustomerProjectMemberRepository;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class MybatisPlusCustomerProjectMemberRepository implements CustomerProjectMemberRepository {
    private final CustomerProjectMemberMapper mapper;

    @Override
    public CustomerProjectMemberEntity save(CustomerProjectMemberEntity member) {
        int affected = member.getId() == null ? mapper.insert(member) : mapper.updateById(member);
        if (affected != 1) {
            throw new ApiException(ApiErrorCode.CONFLICT.getCode(), "项目成员状态已变化，请重试");
        }
        return member;
    }

    @Override
    public Optional<CustomerProjectMemberEntity> findActiveByProjectAndOrganizationMember(
            Long projectId, Long organizationId, Long organizationMemberId) {
        return Optional.ofNullable(mapper.selectOne(Wrappers.<CustomerProjectMemberEntity>lambdaQuery()
                .eq(CustomerProjectMemberEntity::getProjectId, projectId)
                .eq(CustomerProjectMemberEntity::getOrganizationId, organizationId)
                .eq(CustomerProjectMemberEntity::getOrganizationMemberId, organizationMemberId)
                .eq(CustomerProjectMemberEntity::getStatus, CustomerProjectMemberStatus.ACTIVE)
                .last("LIMIT 1")));
    }

    @Override
    public List<CustomerProjectMemberEntity> findByProject(Long projectId, CustomerProjectMemberStatus status) {
        var query = Wrappers.<CustomerProjectMemberEntity>lambdaQuery()
                .eq(CustomerProjectMemberEntity::getProjectId, projectId)
                .orderByAsc(CustomerProjectMemberEntity::getId);
        if (status != null) query.eq(CustomerProjectMemberEntity::getStatus, status);
        return mapper.selectList(query);
    }
}
