package com.advertisementdesign.back.project.service;

import com.advertisementdesign.back.common.audit.service.AuditLogWriter;
import com.advertisementdesign.back.common.exception.ApiException;
import com.advertisementdesign.back.identity.model.ActorRef;
import com.advertisementdesign.back.identity.service.CurrentActorProvider;
import com.advertisementdesign.back.identity.service.CurrentUserProfileProvider;
import com.advertisementdesign.back.project.converter.ProjectConverter;
import com.advertisementdesign.back.project.entity.ProjectEntity;
import com.advertisementdesign.back.project.enums.ProjectNameSource;
import com.advertisementdesign.back.project.repository.ProjectRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectNamingServiceTest {
    @Mock private ProjectRepository repository;
    @Mock private ProjectAuthorizationService authorizationService;
    @Mock private CurrentActorProvider currentActorProvider;
    @Mock private CurrentUserProfileProvider currentUserProfileProvider;
    @Mock private AuditLogWriter auditLogWriter;

    @Test
    void manualNameCannotBeOverwrittenAutomatically() {
        ProjectEntity project = ProjectEntity.builder().id(101L).organizationId(20L).name("客户命名")
                .nameSource(ProjectNameSource.MANUAL).confirmedRequirementVersionId(55L).version(3L).build();
        when(repository.findById(101L)).thenReturn(Optional.of(project));
        ProjectNamingService service = service();

        assertFalse(service.applyAutomaticName(101L, 3L, "自动名称"));

        verify(repository, never()).updateName(any(), any(), any(), any(), any());
    }

    @Test
    void concurrentManualRenameReturnsExplicitConflict() {
        ActorRef actor = new ActorRef(ActorRef.ActorType.CUSTOMER_USER, 7L);
        when(currentActorProvider.requireCurrentActor())
                .thenReturn(new CurrentActorProvider.CurrentActor(actor, "customer"));
        when(authorizationService.authorize(101L, ProjectAuthorizationService.ProjectAction.UPDATE_PROJECT))
                .thenReturn(new ProjectAuthorizationService.AuthorizationDecision(true, null, null));
        when(repository.findById(101L)).thenReturn(Optional.of(ProjectEntity.builder().id(101L)
                .organizationId(20L).name("旧名称").nameSource(ProjectNameSource.AUTO).version(4L).build()));
        when(repository.updateName(101L, 3L, "新名称", ProjectNameSource.MANUAL, null)).thenReturn(false);
        ProjectNamingService service = service();

        ApiException exception = assertThrows(ApiException.class,
                () -> service.renameManually(101L, "新名称", 3L));

        assertEquals(409, exception.getCode());
        assertEquals("项目名称已被其他操作更新，请刷新后重试", exception.getMessage());
        verify(repository).updateName(101L, 3L, "新名称",
                ProjectNameSource.MANUAL, null);
    }

    private ProjectNamingService service() {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        return new ProjectNamingService(repository, authorizationService, currentActorProvider,
                currentUserProfileProvider, auditLogWriter, new ProjectConverter(objectMapper), objectMapper);
    }
}
