package com.campusmart.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campusmart.entity.Message;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface MessageMapper extends BaseMapper<Message> {
    
    @Select("SELECT m.*, u.nickname AS from_user_nickname, u.avatar AS from_user_avatar " +
            "FROM tb_message m " +
            "LEFT JOIN tb_user u ON m.from_user_id = u.id " +
            "WHERE (m.from_user_id = #{userId} OR m.to_user_id = #{userId}) " +
            "ORDER BY m.create_time DESC")
    List<Message> selectConversationList(@Param("userId") Long userId);
    
    @Select("SELECT m.*, u.nickname AS from_user_nickname, u.avatar AS from_user_avatar " +
            "FROM tb_message m " +
            "LEFT JOIN tb_user u ON m.from_user_id = u.id " +
            "WHERE ((m.from_user_id = #{userId} AND m.to_user_id = #{targetUserId}) " +
            "OR (m.from_user_id = #{targetUserId} AND m.to_user_id = #{userId})) " +
            "ORDER BY m.create_time ASC")
    List<Message> selectMessageDetail(@Param("userId") Long userId, @Param("targetUserId") Long targetUserId);
}
