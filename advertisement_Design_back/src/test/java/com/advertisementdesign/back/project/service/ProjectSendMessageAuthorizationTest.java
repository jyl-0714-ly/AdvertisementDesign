package com.advertisementdesign.back.project.service;

import com.advertisementdesign.back.identity.entity.OrganizationEntity;
import com.advertisementdesign.back.identity.model.ActorRef;
import com.advertisementdesign.back.identity.service.CurrentActorProvider;
import com.advertisementdesign.back.identity.service.OrganizationMembershipService;
import com.advertisementdesign.back.project.converter.ProjectConverter;
import com.advertisementdesign.back.project.entity.CustomerProjectMemberEntity;
import com.advertisementdesign.back.project.entity.ProjectEntity;
import com.advertisementdesign.back.project.enums.CustomerProjectRole;
import com.advertisementdesign.back.project.repository.CustomerProjectMemberRepository;
import com.advertisementdesign.back.project.repository.ProjectAssignmentRepository;
import com.advertisementdesign.back.project.repository.ProjectRepository;
import com.advertisementdesign.back.identity.mapper.OrganizationMapper;
import com.advertisementdesign.back.identity.mapper.OrganizationMemberMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectSendMessageAuthorizationTest {
    @Mock private ProjectRepository projectRepository;
    @Mock private CustomerProjectMemberRepository customerMemberRepository;
    @Mock private ProjectAssignmentRepository assignmentRepository;
    @Mock private OrganizationMemberMapper organizationMemberMapper;
    @Mock private OrganizationMapper organizationMapper;

    @Test
    void viewOnlyCustomerCannotSendProjectMessage() {
        ActorRef actor = new ActorRef(ActorRef.ActorType.CUSTOMER_USER, 11L);
        CurrentActorProvider actorProvider = () -> new CurrentActorProvider.CurrentActor(actor, "test");
        DefaultProjectAuthorizationService service = new DefaultProjectAuthorizationService(
                actorProvider, new OrganizationMembershipService(organizationMemberMapper, organizationMapper),
                projectRepository, customerMemberRepository, assignmentRepository,
                new ProjectConverter(new ObjectMapper()));
        when(projectRepository.findById(101L)).thenReturn(Optional.of(
                ProjectEntity.builder().id(101L).organizationId(22L).build()));
        when(organizationMapper.selectById(22L)).thenReturn(
                OrganizationEntity.builder().id(22L).status("ACTIVE").version(1L).build());
        when(organizationMemberMapper.selectOne(any())).thenReturn(
                com.advertisementdesign.back.identity.entity.OrganizationMemberEntity.builder()
                        .id(33L).organizationId(22L).userId(11L).status("ACTIVE").version(1L).build());
        when(customerMemberRepository.findActiveByProjectAndOrganizationMember(101L, 22L, 33L))
                .thenReturn(Optional.of(CustomerProjectMemberEntity.builder()
                        .id(44L).projectId(101L).organizationId(22L).organizationMemberId(33L)
                        .projectRole(CustomerProjectRole.VIEW_ONLY).version(1L).build()));

        ProjectAuthorizationService.AuthorizationDecision decision = service.authorize(
                101L, ProjectAuthorizationService.ProjectAction.SEND_MESSAGE);

        assertFalse(decision.allowed());
    }
}
