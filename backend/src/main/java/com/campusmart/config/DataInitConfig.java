package com.campusmart.config;

import com.campusmart.entity.User;
import com.campusmart.mapper.UserMapper;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitConfig implements CommandLineRunner {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public DataInitConfig(UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        // 检查管理员账号是否存在
        User admin = userMapper.findByUsername("admin");
        if (admin == null) {
            User newAdmin = new User();
            newAdmin.setUsername("admin");
            newAdmin.setPassword(passwordEncoder.encode("admin123"));
            newAdmin.setNickname("管理员");
            newAdmin.setRole("ADMIN");
            newAdmin.setStatus(1);
            userMapper.insert(newAdmin);
            System.out.println("管理员账号已创建: admin / admin123");
        } else {
            // 确保管理员密码正确
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setRole("ADMIN");
            admin.setStatus(1);
            userMapper.updateById(admin);
            System.out.println("管理员账号已更新: admin / admin123");
        }
    }
}
