package com.advertisementdesign.back.consultation.service;

import com.advertisementdesign.back.auth.service.AuthService;
import com.advertisementdesign.back.common.exception.ApiException;
import com.advertisementdesign.back.consultation.entity.ConsultantHumanMessageEntity;
import com.advertisementdesign.back.consultation.entity.ConsultantIntakeEntity;
import com.advertisementdesign.back.consultation.entity.DesignerProfileEntity;
import com.advertisementdesign.back.consultation.enums.ConsultantIntakeStatus;
import com.advertisementdesign.back.consultation.model.ConsultantIntakeModels;
import com.advertisementdesign.back.consultation.repository.ConsultationRepository;
import com.advertisementdesign.back.identity.entity.UserEntity;
import com.advertisementdesign.back.identity.enums.UserRole;
import com.advertisementdesign.back.identity.enums.UserStatus;
import com.advertisementdesign.back.identity.service.IdentityService;
import com.advertisementdesign.back.identity.service.IdentityService.UserProfile;
import com.advertisementdesign.back.project.repository.ProjectRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConsultantIntakeServiceTest {

    @Test
    void submitUsesAuthenticatedCustomerAndReturnsHandoffData() {
        Fixture fixture = new Fixture();
        ConsultantIntakeService service = fixture.serviceFor(fixture.customer);

        ConsultantIntakeModels.ConsultantIntakeVO result = service.submit(request("品牌设计", "餐饮"));

        assertEquals(2L, result.matchedDesigner().id());
        assertEquals(2, result.greetingMessages().size());
        assertNotNull(result.humanChatId());
        assertEquals(1L, fixture.intakes.get(result.intakeId()).getCustomerId());
        assertEquals(2, fixture.messages.size());
    }

    @Test
    void matchingUsesOnlineThenWorkloadThenSpecialtyThenIdPriority() {
        Fixture fixture = new Fixture();
        fixture.addDesigner(3L, "离线低负载", false, List.of("品牌设计"));
        fixture.addDesigner(4L, "在线低负载无专长", true, List.of("包装设计"));
        fixture.addDesigner(5L, "在线低负载有专长", true, List.of("品牌设计"));
        fixture.addDesigner(6L, "同优先级较大编号", true, List.of("品牌设计"));
        when(fixture.projectRepository.countInProgressProjectsByDesigner(2L)).thenReturn(1L);

        ConsultantIntakeModels.ConsultantIntakeVO result = fixture.serviceFor(fixture.customer)
                .submit(request("品牌设计", "餐饮"));

        assertEquals(5L, result.matchedDesigner().id());
    }

    @Test
    void submitRejectsDesignerRole() {
        Fixture fixture = new Fixture();
        ConsultantIntakeService service = fixture.serviceFor(fixture.designer);

        ApiException exception = assertThrows(ApiException.class,
                () -> service.submit(request("品牌设计", "餐饮")));

        assertEquals(403, exception.getCode());
    }

    @Test
    void submitRejectsDisabledCustomer() {
        Fixture fixture = new Fixture();
        fixture.customer.setStatus(UserStatus.DISABLED);
        ConsultantIntakeService service = fixture.serviceFor(fixture.customer);

        ApiException exception = assertThrows(ApiException.class,
                () -> service.submit(request("品牌设计", "餐饮")));

        assertEquals(403, exception.getCode());
    }

    @Test
    void matchedDesignerCanListAndIdempotentlyAcceptReception() {
        Fixture fixture = new Fixture();
        ConsultantIntakeModels.ConsultantIntakeVO submitted = fixture.serviceFor(fixture.customer)
                .submit(request("品牌设计", "餐饮"));
        ConsultantIntakeService designerService = fixture.serviceFor(fixture.designer);

        List<ConsultantIntakeModels.DesignerReceptionVO> receptions = designerService.listDesignerReceptions();
        ConsultantIntakeModels.DesignerReceptionVO accepted = designerService.accept(submitted.intakeId());
        ConsultantIntakeModels.DesignerReceptionVO acceptedAgain = designerService.accept(submitted.intakeId());

        assertEquals(1, receptions.size());
        assertEquals(submitted.intakeId(), receptions.get(0).intakeId());
        assertEquals(ConsultantIntakeStatus.MATCHED, receptions.get(0).status());
        assertEquals(ConsultantIntakeStatus.ACCEPTED, accepted.status());
        assertEquals(ConsultantIntakeStatus.ACCEPTED, acceptedAgain.status());
        assertEquals(submitted.humanChatId(), accepted.humanChatId());
    }

    @Test
    void unrelatedOrDisabledDesignerCannotAccessReception() {
        Fixture fixture = new Fixture();
        UserEntity unrelatedDesigner = fixture.addDesigner(
                3L, "其他设计师", true, List.of("包装设计"));
        ConsultantIntakeModels.ConsultantIntakeVO submitted = fixture.serviceFor(fixture.customer)
                .submit(request("品牌设计", "餐饮"));

        ConsultantIntakeService unrelatedService = fixture.serviceFor(unrelatedDesigner);
        assertEquals(403, assertThrows(ApiException.class,
                () -> unrelatedService.getDesignerReception(submitted.intakeId())).getCode());

        fixture.designer.setStatus(UserStatus.DISABLED);
        ConsultantIntakeService disabledService = fixture.serviceFor(fixture.designer);
        assertEquals(403, assertThrows(ApiException.class,
                disabledService::listDesignerReceptions).getCode());
    }

    private ConsultantIntakeModels.SubmitConsultantIntakeRequest request(
            String projectType,
            String industry) {
        return new ConsultantIntakeModels.SubmitConsultantIntakeRequest(
                projectType,
                industry,
                "需要完成品牌视觉升级",
                "1-2 万元",
                "4 周"
        );
    }

    private static final class Fixture {
        private final ConsultationRepository consultationRepository = mock(ConsultationRepository.class);
        private final IdentityService identityService = mock(IdentityService.class);
        private final ProjectRepository projectRepository = mock(ProjectRepository.class);
        private final Map<Long, UserEntity> users = new HashMap<>();
        private final Map<Long, ConsultantIntakeEntity> intakes = new HashMap<>();
        private final List<DesignerProfileEntity> profiles = new ArrayList<>();
        private final List<ConsultantHumanMessageEntity> messages = new ArrayList<>();
        private final AtomicLong intakeSequence = new AtomicLong();
        private final AtomicLong messageSequence = new AtomicLong();
        private final UserEntity customer;
        private final UserEntity designer;

        private Fixture() {
            customer = addUser(1L, "演示客户", UserRole.CUSTOMER);
            designer = addDesigner(2L, "演示设计师", true,
                    List.of("品牌设计", "海报设计", "餐饮", "教育"));

            when(identityService.findById(anyLong())).thenAnswer(invocation ->
                    Optional.ofNullable(users.get(invocation.getArgument(0, Long.class)))
                            .map(this::toProfile));
            when(consultationRepository.listEnabledDesignerProfiles()).thenAnswer(invocation ->
                    profiles.stream().filter(profile -> Boolean.TRUE.equals(profile.getEnabled())).toList());
            when(consultationRepository.saveIntake(any())).thenAnswer(invocation -> {
                ConsultantIntakeEntity intake = invocation.getArgument(0);
                LocalDateTime now = LocalDateTime.now();
                if (intake.getId() == null) {
                    intake.setId(intakeSequence.incrementAndGet());
                    intake.setCreatedAt(now);
                }
                intake.setUpdatedAt(now);
                intakes.put(intake.getId(), intake);
                return intake;
            });
            when(consultationRepository.saveHumanMessage(any())).thenAnswer(invocation -> {
                ConsultantHumanMessageEntity message = invocation.getArgument(0);
                message.setId(messageSequence.incrementAndGet());
                message.setCreatedAt(LocalDateTime.now());
                messages.add(message);
                return message;
            });
            when(consultationRepository.findIntakeById(any())).thenAnswer(invocation ->
                    Optional.ofNullable(intakes.get(invocation.getArgument(0, Long.class))));
            when(consultationRepository.listIntakesByDesigner(any())).thenAnswer(invocation -> {
                Long designerId = invocation.getArgument(0, Long.class);
                return intakes.values().stream()
                        .filter(intake -> designerId.equals(intake.getMatchedDesignerId()))
                        .toList();
            });
            when(consultationRepository.findDesignerProfile(any())).thenAnswer(invocation -> {
                Long designerId = invocation.getArgument(0, Long.class);
                return profiles.stream()
                        .filter(profile -> designerId.equals(profile.getDesignerId()))
                        .findFirst();
            });
        }

        private ConsultantIntakeService serviceFor(UserEntity currentUser) {
            AuthService authService = mock(AuthService.class);
            when(authService.currentUserProfile()).thenReturn(toProfile(currentUser));
            return new ConsultantIntakeService(
                    consultationRepository,
                    identityService,
                    projectRepository,
                    authService
            );
        }

        private UserProfile toProfile(UserEntity user) {
            return new UserProfile(
                    user.getId(), user.getNickname(), user.getRole(), user.getAvatar(), user.getStatus());
        }

        private UserEntity addDesigner(
                Long id,
                String nickname,
                boolean online,
                List<String> specialties) {
            UserEntity user = addUser(id, nickname, UserRole.DESIGNER);
            profiles.add(DesignerProfileEntity.builder()
                    .designerId(id)
                    .enabled(true)
                    .online(online)
                    .specialties(specialties)
                    .build());
            return user;
        }

        private UserEntity addUser(Long id, String nickname, UserRole role) {
            UserEntity user = UserEntity.builder()
                    .id(id)
                    .email("user" + id + "@example.com")
                    .passwordHash("not-used")
                    .nickname(nickname)
                    .role(role)
                    .status(UserStatus.ENABLED)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            users.put(id, user);
            return user;
        }
    }
}
