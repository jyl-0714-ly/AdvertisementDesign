package com.advertisementdesign.back.consultation.repository;

import com.advertisementdesign.back.consultation.entity.ConsultantHumanMessageEntity;
import com.advertisementdesign.back.consultation.entity.ConsultantIntakeEntity;
import com.advertisementdesign.back.consultation.entity.DesignerProfileEntity;
import com.advertisementdesign.back.consultation.mapper.ConsultantHumanMessageMapper;
import com.advertisementdesign.back.consultation.mapper.ConsultantIntakeMapper;
import com.advertisementdesign.back.consultation.mapper.DesignerProfileMapper;
import com.advertisementdesign.back.consultation.enums.ConsultantIntakeStatus;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
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
            intakeMapper.updateById(intake);
        }
        return intake;
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
