package com.advertisementdesign.back.communication.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("message_file")
public class MessageFileEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long messageId;
    private Long fileId;
    private LocalDateTime createdAt;
}
