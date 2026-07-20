package com.advertisementdesign.back.domain.entity;

import com.advertisementdesign.back.domain.enums.PortfolioStatus;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("portfolio_case")
public class PortfolioCaseEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String industry;
    private String style;
    private String serviceType;
    private String coverUrl;
    private List<String> imageUrls;
    private String description;
    private Integer sortOrder;
    private PortfolioStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
