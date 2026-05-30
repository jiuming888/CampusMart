package com.campusmart.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("tb_comment")
public class Comment {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long productId;
    private Long userId;
    private String content;
    private Integer rating;
    private Long parentId; // 回复的评论ID，NULL表示顶级评论

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    // 扩展字段
    @TableField(exist = false)
    private String userNickname;

    @TableField(exist = false)
    private String userAvatar;

    // 回复列表
    @TableField(exist = false)
    private List<Comment> replies;
}
