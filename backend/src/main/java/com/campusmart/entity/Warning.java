package com.campusmart.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("tb_warning")
public class Warning {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long adminId;
    private Long userId;
    private String reason;
    private Integer isRead;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    // 关联的用户信息（被警告者）
    @TableField(exist = false)
    private String username;
    
    @TableField(exist = false)
    private String nickname;
    
    // 关联的管理员信息
    @TableField(exist = false)
    private String adminNickname;
}
