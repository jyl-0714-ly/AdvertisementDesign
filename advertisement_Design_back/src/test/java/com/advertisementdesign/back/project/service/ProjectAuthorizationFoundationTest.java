package com.advertisementdesign.back.project.service;

import com.advertisementdesign.back.common.exception.ApiException;
import com.advertisementdesign.back.common.web.CurrentUser;
import com.advertisementdesign.back.identity.entity.OrganizationEntity;
import com.advertisementdesign.back.identity.enums.UserRole;
import com.advertisementdesign.back.identity.enums.UserStatus;
import com.advertisementdesign.back.identity.mapper.OrganizationMapper;
import com.advertisementdesign.back.identity.mapper.OrganizationMemberMapper;
import com.advertisementdesign.back.identity.service.CurrentActorProvider;
import com.advertisementdesign.back.identity.service.IdentityService;
import com.advertisementdesign.back.identity.service.OrganizationMembershipService;
import com.advertisementdesign.back.identity.service.SpringSecurityCurrentActorProvider;
import com.advertisementdesign.back.project.converter.ProjectConverter;
import com.advertisementdesign.back.project.entity.ProjectEntity;
import com.advertisementdesign.back.project.repository.CustomerProjectMemberRepository;
import com.advertisementdesign.back.project.repository.ProjectAssignmentRepository;
import com.advertisementdesign.back.project.repository.ProjectRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectAuthorizationFoundationTest {
    @Mock private IdentityService identityService;
    @Mock private ProjectRepository projectRepository;
    @Mock private CustomerProjectMemberRepository customerMemberRepository;
    @Mock private ProjectAssignmentRepository assignmentRepository;
    @Mock private OrganizationMemberMapper organizationMemberMapper;
    @Mock private OrganizationMapper organizationMapper;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void disabledUserIsRejectedByLiveCurrentActorLookup() {
        CurrentUser principal = CurrentUser.builder().id(7L).nickname("disabled").role(UserRole.DESIGNER).build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of()));
        when(identityService.findById(7L)).thenReturn(Optional.of(
                new IdentityService.UserProfile(7L, "disabled", UserRole.DESIGNER, null, UserStatus.DISABLED)));

        ApiException exception = assertThrows(ApiException.class,
                () -> new SpringSecurityCurrentActorProvider(identityService).requireCurrentActor());
        assertEquals(401, exception.getCode());
    }

    @Test
    void customerWithoutActiveMembershipInProjectOrganizationIsRejected() {
        DefaultProjectAuthorizationService service = authorizationService(actor(11L, UserRole.CUSTOMER));
        when(projectRepository.findById(101L)).thenReturn(Optional.of(project()));
        when(organizationMapper.selectById(22L)).thenReturn(
                OrganizationEntity.builder().id(22L).status("ACTIVE").version(3L).build());
        when(organizationMemberMapper.selectOne(org.mockito.ArgumentMatchers.any())).thenReturn(null);

        ProjectAuthorizationService.AuthorizationDecision decision =
                service.authorize(101L, ProjectAuthorizationService.ProjectAction.VIEW_FULL);

        assertFalse(decision.allowed());
        assertEquals(ProjectAuthorizationService.AccessLevel.NONE, decision.accessLevel());
        assertEquals("NO_ACTIVE_ORGANIZATION_MEMBERSHIP", decision.basis().source());
    }

    @Test
    void ordinaryEnabledDesignerReceivesPrivacyReducedSummaryOnly() {
        DefaultProjectAuthorizationService service = authorizationService(actor(31L, UserRole.DESIGNER));
        when(projectRepository.findById(101L)).thenReturn(Optional.of(project()));
        when(assignmentRepository.findEffectiveAssignment(
                org.mockito.ArgumentMatchers.eq(101L), org.mockito.ArgumentMatchers.eq(31L),
                org.mockito.ArgumentMatchers.any(LocalDateTime.class))).thenReturn(Optional.empty());

        ProjectAuthorizationService.AuthorizationDecision decision =
                service.authorize(101L, ProjectAuthorizationService.ProjectAction.VIEW_SUMMARY);

        assertTrue(decision.allowed());
        assertEquals(ProjectAuthorizationService.AccessLevel.SUMMARY, decision.accessLevel());
        assertEquals("ENABLED_DESIGNER", decision.basis().source());
        assertTrue(decision.basis().scopes().contains("PRIVACY_REDUCED"));
    }

    @Test
    void unassignedDesignerIsDeniedFullProjectAccess() {
        DefaultProjectAuthorizationService service = authorizationService(actor(31L, UserRole.DESIGNER));
        when(projectRepository.findById(101L)).thenReturn(Optional.of(project()));
        when(assignmentRepository.findEffectiveAssignment(
                org.mockito.ArgumentMatchers.eq(101L), org.mockito.ArgumentMatchers.eq(31L),
                org.mockito.ArgumentMatchers.any(LocalDateTime.class))).thenReturn(Optional.empty());

        ProjectAuthorizationService.AuthorizationDecision decision =
                service.authorize(101L, ProjectAuthorizationService.ProjectAction.VIEW_FULL);

        assertFalse(decision.allowed());
        assertEquals(ProjectAuthorizationService.AccessLevel.NONE, decision.accessLevel());
        assertEquals("NO_EFFECTIVE_ASSIGNMENT", decision.basis().source());
    }

    private ProjectEntity project() {
        return ProjectEntity.builder().id(101L).organizationId(22L).build();
    }

    private DefaultProjectAuthorizationService authorizationService(CurrentActorProvider actorProvider) {
        return new DefaultProjectAuthorizationService(
                actorProvider, new OrganizationMembershipService(organizationMemberMapper, organizationMapper),
                projectRepository, customerMemberRepository, assignmentRepository,
                new ProjectConverter(new ObjectMapper()));
    }

    private CurrentActorProvider actor(Long userId, UserRole role) {
        return () -> new CurrentActorProvider.CurrentActor(
                new com.advertisementdesign.back.identity.model.ActorRef(
                        switch (role) {
                            case CUSTOMER -> com.advertisementdesign.back.identity.model.ActorRef.ActorType.CUSTOMER_USER;
                            case DESIGNER -> com.advertisementdesign.back.identity.model.ActorRef.ActorType.DESIGNER_USER;
                            case ADMIN -> com.advertisementdesign.back.identity.model.ActorRef.ActorType.ADMIN_USER;
                        }, userId), "test");
    }
}
