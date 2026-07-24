package com.advertisementdesign.back.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DesignerProfileEntity {
    private Long designerId;
    private Boolean enabled;
    private Boolean online;
    private List<String> specialties;
}
