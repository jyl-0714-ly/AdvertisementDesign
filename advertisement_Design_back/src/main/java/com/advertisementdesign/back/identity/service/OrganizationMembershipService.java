package com.advertisementdesign.back.identity.service;

import com.advertisementdesign.back.identity.entity.OrganizationEntity;
import com.advertisementdesign.back.identity.entity.OrganizationMemberEntity;
import com.advertisementdesign.back.identity.mapper.OrganizationMapper;
import com.advertisementdesign.back.identity.mapper.OrganizationMemberMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class OrganizationMembershipService {
    private final OrganizationMemberMapper memberMapper;
    private final OrganizationMapper organizationMapper;

    public OrganizationMembershipService(OrganizationMemberMapper memberMapper,
                                         OrganizationMapper organizationMapper) {
        this.memberMapper = memberMapper;
        this.organizationMapper = organizationMapper;
    }

    public Optional<ActiveOrganization> findActiveOrganization(Long organizationId) {
        OrganizationEntity organization = organizationMapper.selectById(organizationId);
        if (organization == null || !"ACTIVE".equals(organization.getStatus())) {
            return Optional.empty();
        }
        return Optional.of(new ActiveOrganization(organization.getId(), organization.getVersion()));
    }

    public Optional<ActiveOrganizationMember> findActiveMembership(Long organizationId, Long userId) {
        OrganizationMemberEntity member = memberMapper.selectOne(Wrappers.<OrganizationMemberEntity>lambdaQuery()
                .eq(OrganizationMemberEntity::getOrganizationId, organizationId)
                .eq(OrganizationMemberEntity::getUserId, userId)
                .eq(OrganizationMemberEntity::getStatus, "ACTIVE")
                .last("LIMIT 1"));
        return Optional.ofNullable(member).map(value ->
                new ActiveOrganizationMember(value.getId(), value.getOrganizationId(), value.getUserId(),
                        value.getMemberRole(), value.getVersion()));
    }

    public record ActiveOrganization(Long id, Long version) {
    }

    public record ActiveOrganizationMember(Long id, Long organizationId, Long userId, String memberRole,
                                           Long version) {
    }
}
