package com.advertisementdesign.back.project.service;

import com.advertisementdesign.back.common.audit.service.AuditLogWriter;
import com.advertisementdesign.back.common.exception.ApiException;
import com.advertisementdesign.back.common.idempotency.service.IdempotencyService;
import com.advertisementdesign.back.common.outbox.service.ReliableEventWriter;
import com.advertisementdesign.back.common.storage.enums.FileBusinessScope;
import com.advertisementdesign.back.common.storage.service.FileService;
import com.advertisementdesign.back.communication.service.FirstRequirementConversationService;
import com.advertisementdesign.back.identity.enums.UserRole;
import com.advertisementdesign.back.identity.enums.UserStatus;
import com.advertisementdesign.back.identity.model.ActorRef;
import com.advertisementdesign.back.identity.service.CurrentActorProvider;
import com.advertisementdesign.back.identity.service.CurrentUserProfileProvider;
import com.advertisementdesign.back.identity.service.IdentityService;
import com.advertisementdesign.back.identity.service.OrganizationMembershipService;
import com.advertisementdesign.back.project.converter.ProjectConverter;
import com.advertisementdesign.back.project.dto.FirstRequirementRequest;
import com.advertisementdesign.back.project.entity.ProjectEntity;
import com.advertisementdesign.back.project.repository.CustomerProjectMemberRepository;
import com.advertisementdesign.back.project.repository.ProjectRepository;
import com.advertisementdesign.back.project.vo.FirstRequirementResponse;
import com.advertisementdesign.back.workflow.enums.StageCode;
import com.advertisementdesign.back.workflow.service.ProjectWorkflowInitializationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FirstRequirementHighRiskTest {
    private static final ActorRef ACTOR = new ActorRef(ActorRef.ActorType.CUSTOMER_USER, 101L);

    private final CurrentActorProvider actorProvider = Mockito.mock(CurrentActorProvider.class);
    private final CurrentUserProfileProvider profileProvider = Mockito.mock(CurrentUserProfileProvider.class);
    private final OrganizationMembershipService membershipService = Mockito.mock(OrganizationMembershipService.class);
    private final IdempotencyService idempotencyService = Mockito.mock(IdempotencyService.class);
    private final ProjectRepository projectRepository = Mockito.mock(ProjectRepository.class);
    private final CustomerProjectMemberRepository memberRepository = Mockito.mock(CustomerProjectMemberRepository.class);
    private final FirstRequirementAttachmentService attachmentService = Mockito.mock(FirstRequirementAttachmentService.class);
    private final FirstRequirementConversationService conversationService = Mockito.mock(FirstRequirementConversationService.class);
    private final ProjectWorkflowInitializationService workflowService = Mockito.mock(ProjectWorkflowInitializationService.class);
    private final AuditLogWriter auditWriter = Mockito.mock(AuditLogWriter.class);
    private final ReliableEventWriter eventWriter = Mockito.mock(ReliableEventWriter.class);
    private final DeterministicValidRequirementPolicy policy = new DeterministicValidRequirementPolicy();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ProjectConverter converter = new ProjectConverter(objectMapper);
    private final FirstRequirementProjectCreationService service = new FirstRequirementProjectCreationService(
            policy, actorProvider, profileProvider, membershipService, idempotencyService,
            projectRepository, memberRepository, attachmentService, conversationService,
            workflowService, auditWriter, eventWriter, converter, objectMapper);

    private JdbcTemplate jdbc;
    private TransactionTemplate transaction;

    @BeforeEach
    void setUp() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:first_requirement;MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("CREATE TABLE IF NOT EXISTS first_requirement_write_probe "
                + "(id BIGINT AUTO_INCREMENT PRIMARY KEY, category VARCHAR(64))");
        jdbc.execute("DELETE FROM first_requirement_write_probe");
        transaction = new TransactionTemplate(new DataSourceTransactionManager(dataSource));

        when(actorProvider.requireCurrentActor())
                .thenReturn(new CurrentActorProvider.CurrentActor(ACTOR, "客户甲"));
        when(profileProvider.currentUserProfile())
                .thenReturn(new IdentityService.UserProfile(101L, "客户甲", UserRole.CUSTOMER, null, UserStatus.ENABLED));
        when(membershipService.findActiveOrganization(11L))
                .thenReturn(Optional.of(new OrganizationMembershipService.ActiveOrganization(11L, 0L)));
        when(membershipService.findActiveMembership(11L, 101L))
                .thenReturn(Optional.of(new OrganizationMembershipService.ActiveOrganizationMember(
                        21L, 11L, 101L, "PRIMARY", 0L)));
    }

    @Test
    void invalidContentCreatesNoBusinessOrReliabilityRecords() {
        FirstRequirementResponse response = service.create(request("你好"), "invalid-key");

        assertEquals(FirstRequirementResponse.Status.INVALID_REQUIREMENT, response.status());
        assertEquals(0, probeCount());
        verify(actorProvider, never()).requireCurrentActor();
        verify(idempotencyService, never()).claim(any(), any(), any());
    }

    @Test
    void middleFailureRollsBackEveryEarlierWriteIncludingIdempotencyOwnership() throws Exception {
        Method useCase = FirstRequirementProjectCreationService.class
                .getMethod("create", FirstRequirementRequest.class, String.class);
        assertTrue(useCase.isAnnotationPresent(Transactional.class));
        when(idempotencyService.claim(any(), any(), any())).thenAnswer(invocation -> {
            mark("idempotency");
            return ownerClaim();
        });
        stubSuccessfulWritesUntilWorkflow();
        when(workflowService.initialize(any())).thenAnswer(invocation -> {
            mark("workflow");
            throw new IllegalStateException("injected workflow failure");
        });

        assertThrows(IllegalStateException.class, () -> transaction.executeWithoutResult(
                status -> service.create(request("请设计一张新品宣传海报"), "rollback-key")));

        assertEquals(0, probeCount());
        verify(auditWriter, never()).append(any());
        verify(eventWriter, never()).write(any());
        verify(idempotencyService, never()).succeed(any(), any(), any(), any());
    }

    @Test
    void successfulReplayReturnsSameResourceWithoutCreatingTwice() {
        Map<String, Object> snapshot = Map.of(
                "projectId", 31L, "conversationId", 41L,
                "projectName", ProjectEntity.INITIAL_NAME,
                "currentStage", StageCode.REQUIREMENT_GUIDE.name());
        AtomicInteger claims = new AtomicInteger();
        when(idempotencyService.claim(any(), any(), any())).thenAnswer(invocation -> {
            if (claims.getAndIncrement() == 0) {
                mark("idempotency");
                return ownerClaim();
            }
            return new IdempotencyService.Claim(51L, 1L,
                    IdempotencyService.ClaimState.REPLAY_SUCCEEDED, 31L, snapshot, null);
        });
        stubSuccessfulWritesUntilWorkflow();
        when(workflowService.initialize(any())).thenAnswer(invocation -> {
            mark("workflow");
            return new ProjectWorkflowInitializationService.InitializedWorkflow(
                    61L, StageCode.REQUIREMENT_GUIDE, "需求引导");
        });
        when(auditWriter.append(any())).thenAnswer(invocation -> { mark("audit"); return 71L; });
        when(eventWriter.write(any())).thenAnswer(invocation -> { mark("outbox"); return 81L; });
        Mockito.doAnswer(invocation -> { mark("idempotency-success"); return null; })
                .when(idempotencyService).succeed(any(), any(), any(), any());

        FirstRequirementRequest request = request("请设计一张新品宣传海报");
        FirstRequirementResponse created = transaction.execute(status -> service.create(request, "same-key"));
        FirstRequirementResponse replay = transaction.execute(status -> service.create(request, "same-key"));

        assertEquals(FirstRequirementResponse.Status.PROJECT_CREATED, created.status());
        assertEquals(FirstRequirementResponse.Status.IDEMPOTENT_REPLAY, replay.status());
        assertEquals(created.projectId(), replay.projectId());
        assertEquals(created.conversationId(), replay.conversationId());
        assertEquals(11, probeCount());
        verify(projectRepository).save(any());
        verify(memberRepository).save(any());
        verify(conversationService).initialize(any());
        verify(workflowService).initialize(any());
        verify(eventWriter, Mockito.times(3)).write(any());
    }

    @Test
    void crossCustomerAttachmentIsRejectedWithoutClaim() {
        FileService fileService = Mockito.mock(FileService.class);
        FirstRequirementAttachmentService realService = new FirstRequirementAttachmentService(fileService);
        when(fileService.requireActiveMetadata(91L)).thenReturn(new FileService.AssetMetadata(
                91L, ActorRef.ActorType.CUSTOMER_USER.name(), 202L, null, null,
                FileBusinessScope.PRIVATE_DRAFT,
                com.advertisementdesign.back.common.storage.enums.StorageVisibility.INTERNAL,
                com.advertisementdesign.back.common.storage.enums.StorageZone.PRIVATE,
                "brief.pdf", "application/pdf", "pdf", 1024L));

        ApiException exception = assertThrows(ApiException.class, () -> realService.validateAndClaim(
                new FirstRequirementAttachmentService.Command(ACTOR, 11L, 31L, List.of(91L))));

        assertEquals(403, exception.getCode());
        verify(fileService, never()).claimFirstRequirementDraft(
                any(), any(), any(), any());
    }

    @Test
    void vagueIntentWordsAloneAreNotValidRequirements() {
        assertFalse(policy.evaluate("我需要", false).valid());
        assertFalse(policy.evaluate("产品活动", false).valid());
    }

    private void stubSuccessfulWritesUntilWorkflow() {
        when(projectRepository.save(any())).thenAnswer(invocation -> {
            mark("project");
            ProjectEntity project = invocation.getArgument(0);
            project.setId(31L);
            return project;
        });
        when(memberRepository.save(any())).thenAnswer(invocation -> { mark("member"); return invocation.getArgument(0); });
        when(attachmentService.validateAndClaim(any())).thenAnswer(invocation -> { mark("attachment"); return List.of(); });
        when(conversationService.initialize(any())).thenAnswer(invocation -> {
            mark("conversation");
            return new FirstRequirementConversationService.CreatedConversation(41L, 42L);
        });
    }

    private IdempotencyService.Claim ownerClaim() {
        return new IdempotencyService.Claim(51L, 0L, IdempotencyService.ClaimState.OWNER, null, null, null);
    }

    private FirstRequirementRequest request(String content) {
        return new FirstRequirementRequest(11L, content, "client-message-1", List.of());
    }

    private void mark(String category) {
        jdbc.update("INSERT INTO first_requirement_write_probe(category) VALUES (?)", category);
    }

    private int probeCount() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM first_requirement_write_probe", Integer.class);
    }
}
