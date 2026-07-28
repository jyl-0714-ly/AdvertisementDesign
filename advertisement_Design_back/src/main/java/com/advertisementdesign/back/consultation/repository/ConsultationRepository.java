package com.advertisementdesign.back.consultation.repository;

import com.advertisementdesign.back.consultation.entity.ConsultantHumanMessageEntity;
import com.advertisementdesign.back.consultation.entity.ConsultantIntakeEntity;
import com.advertisementdesign.back.consultation.entity.ConsultationDesignerMatchEntity;
import com.advertisementdesign.back.consultation.entity.DesignerProfileEntity;
import com.advertisementdesign.back.consultation.mapper.ConsultantHumanMessageMapper;
import com.advertisementdesign.back.consultation.mapper.ConsultantIntakeMapper;
import com.advertisementdesign.back.consultation.mapper.ConsultationDesignerMatchMapper;
import com.advertisementdesign.back.consultation.mapper.DesignerProfileMapper;
import com.advertisementdesign.back.consultation.enums.ConsultationMatchStatus;
import com.advertisementdesign.back.consultation.enums.ConsultantIntakeStatus;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ConsultationRepository {
    private final ConsultantIntakeMapper intakeMapper;
    private final ConsultantHumanMessageMapper humanMessageMapper;
    private final DesignerProfileMapper designerProfileMapper;
    private final ConsultationDesignerMatchMapper designerMatchMapper;

    public ConsultantIntakeEntity saveIntake(ConsultantIntakeEntity intake) {
        LocalDateTime now = LocalDateTime.now();
        if (intake.getId() == null) {
            if (intake.getCreatedAt() == null) {
                intake.setCreatedAt(now);
            }
            intake.setUpdatedAt(now);
            intakeMapper.insert(intake);
        } else {
            intake.setUpdatedAt(now);
            if (intakeMapper.updateById(intake) != 1) {
                throw new OptimisticLockingFailureException(
                        "咨询需求已被并发修改: " + intake.getId());
            }
        }
        return intake;
    }

    public boolean confirmHandoff(
            Long intakeId,
            LocalDateTime confirmedAt,
            LocalDateTime nextMatchAt) {
        return intakeMapper.update(null,
                new LambdaUpdateWrapper<ConsultantIntakeEntity>()
                        .eq(ConsultantIntakeEntity::getId, intakeId)
                        .eq(ConsultantIntakeEntity::getStatus,
                                ConsultantIntakeStatus.AGENT_COLLECTING)
                        .set(ConsultantIntakeEntity::getStatus,
                                ConsultantIntakeStatus.READY_FOR_HANDOFF)
                        .set(ConsultantIntakeEntity::getHandoffConfirmedAt,
                                confirmedAt)
                        .set(ConsultantIntakeEntity::getNextMatchAt,
                                nextMatchAt)
                        .set(ConsultantIntakeEntity::getUpdatedAt,
                                confirmedAt)
                        .setSql("version = version + 1")) == 1;
    }

    public boolean acknowledgePendingMatch(
            Long matchId,
            Long designerId,
            LocalDateTime acknowledgedAt) {
        return designerMatchMapper.update(null,
                new LambdaUpdateWrapper<ConsultationDesignerMatchEntity>()
                        .eq(ConsultationDesignerMatchEntity::getId, matchId)
                        .eq(ConsultationDesignerMatchEntity::getDesignerId,
                                designerId)
                        .eq(ConsultationDesignerMatchEntity::getStatus,
                                ConsultationMatchStatus.PENDING_ACK)
                        .gt(ConsultationDesignerMatchEntity::getExpiresAt,
                                acknowledgedAt)
                        .set(ConsultationDesignerMatchEntity::getStatus,
                                ConsultationMatchStatus.ACKNOWLEDGED)
                        .set(ConsultationDesignerMatchEntity::getAcknowledgedAt,
                                acknowledgedAt)
                        .set(ConsultationDesignerMatchEntity::getUpdatedAt,
                                acknowledgedAt)
                        .setSql("version = version + 1")) == 1;
    }

    public boolean acceptMatchedIntake(
            Long intakeId,
            Long designerId,
            LocalDateTime acknowledgedAt) {
        return intakeMapper.update(null,
                new LambdaUpdateWrapper<ConsultantIntakeEntity>()
                        .eq(ConsultantIntakeEntity::getId, intakeId)
                        .eq(ConsultantIntakeEntity::getMatchedDesignerId,
                                designerId)
                        .eq(ConsultantIntakeEntity::getStatus,
                                ConsultantIntakeStatus.MATCHED)
                        .isNull(ConsultantIntakeEntity
                                ::getDesignerAcknowledgedAt)
                        .set(ConsultantIntakeEntity::getStatus,
                                ConsultantIntakeStatus.ACCEPTED)
                        .set(ConsultantIntakeEntity
                                        ::getDesignerAcknowledgedAt,
                                acknowledgedAt)
                        .set(ConsultantIntakeEntity::getUpdatedAt,
                                acknowledgedAt)
                        .setSql("version = version + 1")) == 1;
    }

    public boolean expirePendingMatch(
            Long matchId,
            Long designerId,
            LocalDateTime expiredAt) {
        return designerMatchMapper.update(null,
                new LambdaUpdateWrapper<ConsultationDesignerMatchEntity>()
                        .eq(ConsultationDesignerMatchEntity::getId, matchId)
                        .eq(ConsultationDesignerMatchEntity::getDesignerId,
                                designerId)
                        .eq(ConsultationDesignerMatchEntity::getStatus,
                                ConsultationMatchStatus.PENDING_ACK)
                        .le(ConsultationDesignerMatchEntity::getExpiresAt,
                                expiredAt)
                        .set(ConsultationDesignerMatchEntity::getStatus,
                                ConsultationMatchStatus.EXPIRED)
                        .set(ConsultationDesignerMatchEntity::getExpiredAt,
                                expiredAt)
                        .set(ConsultationDesignerMatchEntity::getUpdatedAt,
                                expiredAt)
                        .setSql("version = version + 1")) == 1;
    }

    public boolean releaseExpiredAssignment(
            Long intakeId,
            Long designerId,
            LocalDateTime retryAt,
            LocalDateTime handoffConfirmedAt) {
        return intakeMapper.update(null,
                new LambdaUpdateWrapper<ConsultantIntakeEntity>()
                        .eq(ConsultantIntakeEntity::getId, intakeId)
                        .eq(ConsultantIntakeEntity::getMatchedDesignerId,
                                designerId)
                        .eq(ConsultantIntakeEntity::getStatus,
                                ConsultantIntakeStatus.MATCHED)
                        .isNull(ConsultantIntakeEntity
                                ::getDesignerAcknowledgedAt)
                        .set(ConsultantIntakeEntity::getStatus,
                                ConsultantIntakeStatus.READY_FOR_HANDOFF)
                        .set(ConsultantIntakeEntity::getMatchedDesignerId,
                                null)
                        .set(ConsultantIntakeEntity::getDesignerAssignedAt,
                                null)
                        .set(ConsultantIntakeEntity
                                        ::getDesignerAcknowledgedAt,
                                null)
                        .set(ConsultantIntakeEntity::getHandoffConfirmedAt,
                                handoffConfirmedAt)
                        .set(ConsultantIntakeEntity::getNextMatchAt,
                                retryAt)
                        .set(ConsultantIntakeEntity::getUpdatedAt,
                                retryAt)
                        .setSql("version = version + 1")) == 1;
    }

    public Optional<ConsultantIntakeEntity> findIntakeById(Long id) {
        return Optional.ofNullable(intakeMapper.selectById(id));
    }

    public Optional<ConsultantIntakeEntity> findIntakeByIdForUpdate(Long id) {
        return Optional.ofNullable(intakeMapper.selectOne(
                new LambdaQueryWrapper<ConsultantIntakeEntity>()
                        .eq(ConsultantIntakeEntity::getId, id)
                        .last("FOR UPDATE")));
    }

    public Optional<ConsultantIntakeEntity> findIntakeByHumanChatId(String humanChatId) {
        return Optional.ofNullable(intakeMapper.selectOne(
                new LambdaQueryWrapper<ConsultantIntakeEntity>()
                        .eq(ConsultantIntakeEntity::getHumanChatId, humanChatId)));
    }

    public Optional<ConsultantIntakeEntity> findCurrentIntakeByCustomer(Long customerId) {
        return Optional.ofNullable(intakeMapper.selectOne(
                new LambdaQueryWrapper<ConsultantIntakeEntity>()
                        .eq(ConsultantIntakeEntity::getCustomerId, customerId)
                        .orderByDesc(ConsultantIntakeEntity::getUpdatedAt)
                        .orderByDesc(ConsultantIntakeEntity::getId)
                        .last("LIMIT 1")));
    }

    public long countActiveIntakesByDesigner(Long designerId) {
        return intakeMapper.selectCount(new LambdaQueryWrapper<ConsultantIntakeEntity>()
                .eq(ConsultantIntakeEntity::getMatchedDesignerId, designerId)
                .in(ConsultantIntakeEntity::getStatus,
                        ConsultantIntakeStatus.MATCHED,
                        ConsultantIntakeStatus.ACCEPTED));
    }

    public List<ConsultantIntakeEntity> listIntakesByDesigner(Long designerId) {
        return intakeMapper.selectList(new LambdaQueryWrapper<ConsultantIntakeEntity>()
                .eq(ConsultantIntakeEntity::getMatchedDesignerId, designerId)
                .orderByDesc(ConsultantIntakeEntity::getCreatedAt)
                .orderByDesc(ConsultantIntakeEntity::getId));
    }

    public List<DesignerProfileEntity> listEnabledDesignerProfiles() {
        return designerProfileMapper.selectList(new LambdaQueryWrapper<DesignerProfileEntity>()
                .eq(DesignerProfileEntity::getEnabled, true));
    }

    public Optional<DesignerProfileEntity> findDesignerProfile(Long designerId) {
        return Optional.ofNullable(designerProfileMapper.selectById(designerId));
    }

    public Optional<DesignerProfileEntity> findDesignerProfileForUpdate(Long designerId) {
        return Optional.ofNullable(designerProfileMapper.selectOne(
                new LambdaQueryWrapper<DesignerProfileEntity>()
                        .eq(DesignerProfileEntity::getDesignerId, designerId)
                        .last("FOR UPDATE")));
    }

    public DesignerProfileEntity saveDesignerProfile(DesignerProfileEntity profile) {
        profile.setUpdatedAt(LocalDateTime.now());
        if (designerProfileMapper.updateById(profile) != 1) {
            throw new OptimisticLockingFailureException(
                    "设计师档案已被并发修改: " + profile.getDesignerId());
        }
        return profile;
    }

    public List<ConsultantIntakeEntity> listDueHandoffIntakes(
            LocalDateTime dueAt,
            int limit) {
        return intakeMapper.selectList(new LambdaQueryWrapper<ConsultantIntakeEntity>()
                .eq(ConsultantIntakeEntity::getStatus, ConsultantIntakeStatus.READY_FOR_HANDOFF)
                .isNotNull(ConsultantIntakeEntity::getNextMatchAt)
                .le(ConsultantIntakeEntity::getNextMatchAt, dueAt)
                .orderByAsc(ConsultantIntakeEntity::getNextMatchAt)
                .orderByAsc(ConsultantIntakeEntity::getId)
                .last("LIMIT " + Math.max(1, limit)));
    }

    public ConsultationDesignerMatchEntity saveDesignerMatch(
            ConsultationDesignerMatchEntity match) {
        LocalDateTime now = LocalDateTime.now();
        if (match.getId() == null) {
            if (match.getCreatedAt() == null) {
                match.setCreatedAt(now);
            }
            match.setUpdatedAt(now);
            designerMatchMapper.insert(match);
        } else {
            match.setUpdatedAt(now);
            if (designerMatchMapper.updateById(match) != 1) {
                throw new OptimisticLockingFailureException(
                        "匹配记录已被并发修改: " + match.getId());
            }
        }
        return match;
    }

    public boolean hasPendingMatch(Long consultantIntakeId) {
        return designerMatchMapper.selectCount(
                new LambdaQueryWrapper<ConsultationDesignerMatchEntity>()
                        .eq(ConsultationDesignerMatchEntity::getConsultantIntakeId,
                                consultantIntakeId)
                        .eq(ConsultationDesignerMatchEntity::getStatus,
                                ConsultationMatchStatus.PENDING_ACK)) > 0;
    }

    public Optional<ConsultationDesignerMatchEntity> findPendingMatchForUpdate(
            Long consultantIntakeId) {
        return Optional.ofNullable(designerMatchMapper.selectOne(
                new LambdaQueryWrapper<ConsultationDesignerMatchEntity>()
                        .eq(ConsultationDesignerMatchEntity::getConsultantIntakeId, consultantIntakeId)
                        .eq(ConsultationDesignerMatchEntity::getStatus,
                                com.advertisementdesign.back.consultation.enums.ConsultationMatchStatus.PENDING_ACK)
                        .orderByDesc(ConsultationDesignerMatchEntity::getAttemptNo)
                        .last("LIMIT 1 FOR UPDATE")));
    }

    public List<Long> listFailedDesignerIds(Long consultantIntakeId) {
        return designerMatchMapper.selectList(
                        new LambdaQueryWrapper<ConsultationDesignerMatchEntity>()
                                .eq(ConsultationDesignerMatchEntity::getConsultantIntakeId,
                                        consultantIntakeId)
                                .in(ConsultationDesignerMatchEntity::getStatus,
                                        com.advertisementdesign.back.consultation.enums.ConsultationMatchStatus.EXPIRED,
                                        com.advertisementdesign.back.consultation.enums.ConsultationMatchStatus.REJECTED))
                .stream()
                .map(ConsultationDesignerMatchEntity::getDesignerId)
                .distinct()
                .toList();
    }

    public List<ConsultationDesignerMatchEntity> listExpiredPendingMatches(
            LocalDateTime now,
            int limit) {
        return designerMatchMapper.selectList(
                new LambdaQueryWrapper<ConsultationDesignerMatchEntity>()
                        .eq(ConsultationDesignerMatchEntity::getStatus,
                                com.advertisementdesign.back.consultation.enums.ConsultationMatchStatus.PENDING_ACK)
                        .le(ConsultationDesignerMatchEntity::getExpiresAt, now)
                        .orderByAsc(ConsultationDesignerMatchEntity::getExpiresAt)
                        .orderByAsc(ConsultationDesignerMatchEntity::getId)
                        .last("LIMIT " + Math.max(1, limit)));
    }

    public ConsultantHumanMessageEntity saveHumanMessage(ConsultantHumanMessageEntity message) {
        if (message.getId() == null) {
            if (message.getCreatedAt() == null) {
                message.setCreatedAt(LocalDateTime.now());
            }
            humanMessageMapper.insert(message);
        } else {
            humanMessageMapper.updateById(message);
        }
        return message;
    }

    public List<ConsultantHumanMessageEntity> listHumanMessages(String humanChatId) {
        return humanMessageMapper.selectList(new LambdaQueryWrapper<ConsultantHumanMessageEntity>()
                .eq(ConsultantHumanMessageEntity::getHumanChatId, humanChatId)
                .orderByAsc(ConsultantHumanMessageEntity::getCreatedAt)
                .orderByAsc(ConsultantHumanMessageEntity::getId));
    }
}
