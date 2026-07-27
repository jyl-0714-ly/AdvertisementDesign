package com.advertisementdesign.back.consultation.service;

import com.advertisementdesign.back.auth.service.AuthService;
import com.advertisementdesign.back.common.exception.ApiErrorCode;
import com.advertisementdesign.back.common.exception.ApiException;
import com.advertisementdesign.back.communication.enums.MessageSenderRole;
import com.advertisementdesign.back.consultation.entity.ConsultantHumanMessageEntity;
import com.advertisementdesign.back.consultation.entity.ConsultantIntakeEntity;
import com.advertisementdesign.back.consultation.model.ConsultantHumanChatModels;
import com.advertisementdesign.back.consultation.repository.ConsultationRepository;
import com.advertisementdesign.back.identity.enums.UserRole;
import com.advertisementdesign.back.identity.enums.UserStatus;
import com.advertisementdesign.back.identity.service.IdentityService;
import com.advertisementdesign.back.identity.service.IdentityService.UserProfile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ConsultantHumanChatService {
    private final ConsultationRepository consultationRepository;
    private final IdentityService identityService;
    private final AuthService authService;

    public List<ConsultantHumanChatModels.HumanMessageVO> listMessages(String humanChatId) {
        UserProfile currentUser = authService.currentUserProfile();
        ConsultantIntakeEntity intake = accessibleIntake(humanChatId, currentUser);
        return consultationRepository.listHumanMessages(intake.getHumanChatId()).stream()
                .map(this::toVO)
                .toList();
    }

    @Transactional
    public ConsultantHumanChatModels.HumanMessageVO sendMessage(
            String humanChatId,
            ConsultantHumanChatModels.SendHumanMessageRequest request) {
        UserProfile currentUser = authService.currentUserProfile();
        ConsultantIntakeEntity intake = accessibleIntake(humanChatId, currentUser);
        ConsultantHumanMessageEntity message = consultationRepository.saveHumanMessage(
                ConsultantHumanMessageEntity.builder()
                        .humanChatId(intake.getHumanChatId())
                        .senderId(currentUser.id())
                        .senderRole(toSenderRole(currentUser.role()))
                        .content(request.content().trim())
                        .build());
        return toVO(message);
    }

    private ConsultantIntakeEntity accessibleIntake(String humanChatId, UserProfile user) {
        if (user.status() != UserStatus.ENABLED) {
            throw new ApiException(ApiErrorCode.FORBIDDEN);
        }
        ConsultantIntakeEntity intake = consultationRepository.findIntakeByHumanChatId(humanChatId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.NOT_FOUND));
        boolean customerAccess = user.role() == UserRole.CUSTOMER
                && user.id().equals(intake.getCustomerId());
        boolean designerAccess = user.role() == UserRole.DESIGNER
                && user.id().equals(intake.getMatchedDesignerId());
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
        UserProfile sender = identityService.findById(message.getSenderId()).orElse(null);
        String senderName = sender == null ? "未知用户" : sender.nickname();
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
