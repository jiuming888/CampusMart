package com.campusmart.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campusmart.entity.Comment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CommentMapper extends BaseMapper<Comment> {

    @Select("SELECT c.*, u.nickname AS user_nickname, u.avatar AS user_avatar " +
            "FROM tb_comment c " +
            "LEFT JOIN tb_user u ON c.user_id = u.id " +
            "WHERE c.product_id = #{productId} AND c.parent_id IS NULL " +
            "ORDER BY c.create_time DESC")
    IPage<Comment> selectCommentPageByProductId(Page<Comment> page, @Param("productId") Long productId, @Param("parentId") Long parentId);
}
