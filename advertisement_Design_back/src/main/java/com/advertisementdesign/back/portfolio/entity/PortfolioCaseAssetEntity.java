package com.advertisementdesign.back.portfolio.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@TableName("portfolio_case_asset")
public class PortfolioCaseAssetEntity {
    @TableId(type = IdType.AUTO) private Long id;
    private Long portfolioCaseId;
    private Long fileAssetId;
    private String assetRole;
    private Integer displayOrder;
    private String caption;
    private LocalDateTime createdAt;
}
