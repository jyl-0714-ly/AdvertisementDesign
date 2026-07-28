package com.advertisementdesign.back.consultation.repository;

import com.advertisementdesign.back.consultation.entity.ConsultantIntakeEntity;
import com.advertisementdesign.back.consultation.entity.ConsultationDesignerMatchEntity;
import com.advertisementdesign.back.consultation.entity.DesignerProfileEntity;
import com.advertisementdesign.back.consultation.mapper.ConsultantHumanMessageMapper;
import com.advertisementdesign.back.consultation.mapper.ConsultantIntakeMapper;
import com.advertisementdesign.back.consultation.mapper.ConsultationDesignerMatchMapper;
import com.advertisementdesign.back.consultation.mapper.DesignerProfileMapper;
import org.junit.jupiter.api.Test;
import org.springframework.dao.OptimisticLockingFailureException;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConsultationRepositoryTest {

    @Test
    void designerProfileUpdateRequiresExactlyOneAffectedRow() {
        DesignerProfileMapper profileMapper = mock(DesignerProfileMapper.class);
        ConsultationRepository repository = repository(profileMapper);
        DesignerProfileEntity profile = DesignerProfileEntity.builder()
                .designerId(2L)
                .version(1)
                .build();
        when(profileMapper.updateById(profile)).thenReturn(0);

        assertThrows(OptimisticLockingFailureException.class,
                () -> repository.saveDesignerProfile(profile));
        verify(profileMapper).updateById(profile);
    }

    @Test
    void successfulDesignerProfileUpdateReturnsSameEntity() {
        DesignerProfileMapper profileMapper = mock(DesignerProfileMapper.class);
        ConsultationRepository repository = repository(profileMapper);
        DesignerProfileEntity profile = DesignerProfileEntity.builder()
                .designerId(2L)
                .version(1)
                .build();
        when(profileMapper.updateById(profile)).thenReturn(1);

        assertSame(profile, repository.saveDesignerProfile(profile));
    }

    @Test
    void intakeAndMatchUpdatesRejectOptimisticLockConflicts() {
        ConsultantIntakeMapper intakeMapper = mock(ConsultantIntakeMapper.class);
        ConsultationDesignerMatchMapper matchMapper =
                mock(ConsultationDesignerMatchMapper.class);
        ConsultationRepository repository = repository(
                intakeMapper, mock(DesignerProfileMapper.class), matchMapper);
        ConsultantIntakeEntity intake = ConsultantIntakeEntity.builder()
                .id(7L)
                .version(1)
                .build();
        ConsultationDesignerMatchEntity match =
                ConsultationDesignerMatchEntity.builder()
                        .id(10L)
                        .version(1)
                        .build();
        when(intakeMapper.updateById(intake)).thenReturn(0);
        when(matchMapper.updateById(match)).thenReturn(0);

        assertThrows(OptimisticLockingFailureException.class,
                () -> repository.saveIntake(intake));
        assertThrows(OptimisticLockingFailureException.class,
                () -> repository.saveDesignerMatch(match));
    }

    private ConsultationRepository repository(DesignerProfileMapper profileMapper) {
        return repository(
                mock(ConsultantIntakeMapper.class),
                profileMapper,
                mock(ConsultationDesignerMatchMapper.class));
    }

    private ConsultationRepository repository(
            ConsultantIntakeMapper intakeMapper,
            DesignerProfileMapper profileMapper,
            ConsultationDesignerMatchMapper matchMapper) {
        return new ConsultationRepository(
                intakeMapper,
                mock(ConsultantHumanMessageMapper.class),
                profileMapper,
                matchMapper);
    }
}
