package com.campusmart.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campusmart.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper extends BaseMapper<User> {
    
    @Select("SELECT * FROM tb_user WHERE username = #{username}")
    User findByUsername(String username);
}
