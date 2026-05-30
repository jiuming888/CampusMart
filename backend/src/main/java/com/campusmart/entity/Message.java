package com.campusmart.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("tb_message")
public class Message {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long fromUserId;
    private Long toUserId;
    private Long productId;
    private String content;
    private Integer isRead;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    // 扩展字段
    @TableField(exist = false)
    private String fromUserNickname;
    
    @TableField(exist = false)
    private String fromUserAvatar;
    
    @TableField(exist = false)
    private Long toUserIdCopy;
    
    @TableField(exist = false)
    private String toUserNickname;
    
    @TableField(exist = false)
    private String toUserAvatar;
    
    @TableField(exist = false)
    private String productName;
    
    @TableField(exist = false)
    private Integer unreadCount;
}
