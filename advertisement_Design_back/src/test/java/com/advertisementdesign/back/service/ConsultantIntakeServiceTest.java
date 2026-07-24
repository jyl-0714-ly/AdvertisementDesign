package com.advertisementdesign.back.service;

import com.advertisementdesign.back.api.consultant.ConsultantIntakeModels;
import com.advertisementdesign.back.common.exception.ApiException;
import com.advertisementdesign.back.domain.entity.DesignerProfileEntity;
import com.advertisementdesign.back.domain.entity.ProjectEntity;
import com.advertisementdesign.back.domain.entity.UserEntity;
import com.advertisementdesign.back.domain.enums.ProjectStatus;
import com.advertisementdesign.back.domain.enums.UserRole;
import com.advertisementdesign.back.domain.enums.UserStatus;
import com.advertisementdesign.back.store.DemoDataStore;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConsultantIntakeServiceTest {

    @Test
    void submitUsesAuthenticatedCustomerAndReturnsHandoffData() {
        DemoDataStore store = new DemoDataStore();
        AuthService authService = mock(AuthService.class);
        when(authService.currentUserEntity()).thenReturn(store.findUserById(1L).orElseThrow());
        ConsultantIntakeService service = new ConsultantIntakeService(store, authService);

        ConsultantIntakeModels.ConsultantIntakeVO result = service.submit(request("品牌设计", "餐饮"));

        assertEquals(2L, result.matchedDesigner().id());
        assertEquals(2, result.greetingMessages().size());
        assertNotNull(result.humanChatId());
        assertEquals(1L, store.findConsultantIntakeById(result.intakeId()).orElseThrow().getCustomerId());
    }

    @Test
    void matchingUsesOnlineThenWorkloadThenSpecialtyThenIdPriority() {
        DemoDataStore store = new DemoDataStore();
        addDesigner(store, 3L, "离线低负载", false, List.of("品牌设计"));
        addDesigner(store, 4L, "在线低负载无专长", true, List.of("包装设计"));
        addDesigner(store, 5L, "在线低负载有专长", true, List.of("品牌设计"));
        addDesigner(store, 6L, "同优先级较大编号", true, List.of("品牌设计"));
        store.saveProject(ProjectEntity.builder()
                .name("设计师 2 的额外项目")
                .customerId(1L)
                .designerId(2L)
                .status(ProjectStatus.IN_PROGRESS)
                .currentStage("REQUIREMENT_GUIDE")
                .progress(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());

        AuthService authService = mock(AuthService.class);
        when(authService.currentUserEntity()).thenReturn(store.findUserById(1L).orElseThrow());
        ConsultantIntakeService service = new ConsultantIntakeService(store, authService);

        ConsultantIntakeModels.ConsultantIntakeVO result = service.submit(request("品牌设计", "餐饮"));

        assertEquals(5L, result.matchedDesigner().id());
    }

    @Test
    void submitRejectsDesignerRole() {
        DemoDataStore store = new DemoDataStore();
        AuthService authService = mock(AuthService.class);
        when(authService.currentUserEntity()).thenReturn(store.findUserById(2L).orElseThrow());
        ConsultantIntakeService service = new ConsultantIntakeService(store, authService);

        ApiException exception = assertThrows(ApiException.class,
                () -> service.submit(request("品牌设计", "餐饮")));

        assertEquals(403, exception.getCode());
    }

    @Test
    void submitRejectsDisabledCustomer() {
        DemoDataStore store = new DemoDataStore();
        UserEntity customer = store.findUserById(1L).orElseThrow();
        customer.setStatus(UserStatus.DISABLED);
        AuthService authService = mock(AuthService.class);
        when(authService.currentUserEntity()).thenReturn(customer);
        ConsultantIntakeService service = new ConsultantIntakeService(store, authService);

        ApiException exception = assertThrows(ApiException.class,
                () -> service.submit(request("品牌设计", "餐饮")));

        assertEquals(403, exception.getCode());
    }

    private ConsultantIntakeModels.SubmitConsultantIntakeRequest request(String projectType, String industry) {
        return new ConsultantIntakeModels.SubmitConsultantIntakeRequest(
                projectType,
                industry,
                "需要完成品牌视觉升级",
                "1-2 万元",
                "4 周"
        );
    }

    private void addDesigner(DemoDataStore store, Long id, String nickname, boolean online, List<String> specialties) {
        store.saveUser(UserEntity.builder()
                .id(id)
                .email("designer" + id + "@example.com")
                .passwordHash("not-used")
                .nickname(nickname)
                .role(UserRole.DESIGNER)
                .status(UserStatus.ENABLED)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());
        store.saveDesignerProfile(DesignerProfileEntity.builder()
                .designerId(id)
                .enabled(true)
                .online(online)
                .specialties(specialties)
                .build());
    }
}
