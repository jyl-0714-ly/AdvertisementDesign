package com.advertisementdesign.back.service;

import com.advertisementdesign.back.api.consultant.ConsultantHumanChatModels;
import com.advertisementdesign.back.common.exception.ApiErrorCode;
import com.advertisementdesign.back.common.exception.ApiException;
import com.advertisementdesign.back.domain.entity.ConsultantHumanMessageEntity;
import com.advertisementdesign.back.domain.entity.ConsultantIntakeEntity;
import com.advertisementdesign.back.domain.entity.UserEntity;
import com.advertisementdesign.back.domain.enums.MessageSenderRole;
import com.advertisementdesign.back.domain.enums.UserRole;
import com.advertisementdesign.back.domain.enums.UserStatus;
import com.advertisementdesign.back.store.DemoDataStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ConsultantHumanChatService {
    private final DemoDataStore store;
    private final AuthService authService;

    public List<ConsultantHumanChatModels.HumanMessageVO> listMessages(String humanChatId) {
        UserEntity currentUser = authService.currentUserEntity();
        ConsultantIntakeEntity intake = accessibleIntake(humanChatId, currentUser);
        return store.listConsultantHumanMessages(intake.getHumanChatId()).stream()
                .map(this::toVO)
                .toList();
    }

    public ConsultantHumanChatModels.HumanMessageVO sendMessage(
            String humanChatId,
            ConsultantHumanChatModels.SendHumanMessageRequest request) {
        UserEntity currentUser = authService.currentUserEntity();
        ConsultantIntakeEntity intake = accessibleIntake(humanChatId, currentUser);
        ConsultantHumanMessageEntity message = store.saveConsultantHumanMessage(
                ConsultantHumanMessageEntity.builder()
                        .humanChatId(intake.getHumanChatId())
                        .senderId(currentUser.getId())
                        .senderRole(toSenderRole(currentUser.getRole()))
                        .content(request.content().trim())
                        .build());
        return toVO(message);
    }

    private ConsultantIntakeEntity accessibleIntake(String humanChatId, UserEntity user) {
        if (user.getStatus() != UserStatus.ENABLED) {
            throw new ApiException(ApiErrorCode.FORBIDDEN);
        }
        ConsultantIntakeEntity intake = store.findConsultantIntakeByHumanChatId(humanChatId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.NOT_FOUND));
        boolean customerAccess = user.getRole() == UserRole.CUSTOMER
                && user.getId().equals(intake.getCustomerId());
        boolean designerAccess = user.getRole() == UserRole.DESIGNER
                && user.getId().equals(intake.getMatchedDesignerId());
        if (!customerAccess && !designerAccess) {
            throw new ApiException(ApiErrorCode.FORBIDDEN);
        }
        return intake;
    }

    private MessageSenderRole toSenderRole(UserRole role) {
        if (role == UserRole.CUSTOMER) {
            return MessageSenderRole.CUSTOMER;
        }
        if (role == UserRole.DESIGNER) {
            return MessageSenderRole.DESIGNER;
        }
        throw new ApiException(ApiErrorCode.FORBIDDEN);
    }

    private ConsultantHumanChatModels.HumanMessageVO toVO(ConsultantHumanMessageEntity message) {
        String senderName = store.findUserById(message.getSenderId())
                .map(UserEntity::getNickname)
                .orElse("未知用户");
        return new ConsultantHumanChatModels.HumanMessageVO(
                message.getId(),
                message.getHumanChatId(),
                message.getSenderId(),
                message.getSenderRole(),
                senderName,
                message.getContent(),
                message.getCreatedAt().toString()
        );
    }
}
