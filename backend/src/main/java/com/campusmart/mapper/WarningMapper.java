package com.campusmart.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campusmart.entity.Warning;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface WarningMapper extends BaseMapper<Warning> {
    
    @Select("SELECT w.*, u.nickname AS admin_nickname " +
            "FROM tb_warning w " +
            "LEFT JOIN tb_user u ON w.admin_id = u.id " +
            "WHERE w.user_id = #{userId} " +
            "ORDER BY w.create_time DESC")
    List<Warning> findByUserId(Long userId);
    
    @Select("SELECT w.*, " +
            "u.nickname AS nickname, u.username AS username, " +
            "a.nickname AS admin_nickname " +
            "FROM tb_warning w " +
            "LEFT JOIN tb_user u ON w.user_id = u.id " +
            "LEFT JOIN tb_user a ON w.admin_id = a.id " +
            "ORDER BY w.create_time DESC")
    List<Warning> findAllWithUserInfo();
}
