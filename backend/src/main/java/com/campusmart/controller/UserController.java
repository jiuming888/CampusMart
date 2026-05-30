package com.campusmart.controller;

import com.campusmart.common.Constants;
import com.campusmart.common.Result;
import com.campusmart.dto.LoginDTO;
import com.campusmart.dto.RegisterDTO;
import com.campusmart.entity.User;
import com.campusmart.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {
    
    @Autowired
    private UserService userService;
    
    @PostMapping("/login")
    public Result<?> login(@Valid @RequestBody LoginDTO loginDTO) {
        try {
            Map<String, Object> result = userService.login(loginDTO);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    @PostMapping("/admin/login")
    public Result<?> adminLogin(@Valid @RequestBody LoginDTO loginDTO) {
        try {
            Map<String, Object> result = userService.login(loginDTO);
            boolean isAdmin = (boolean) result.get("isAdmin");
            if (!isAdmin) {
                return Result.error("非管理员账号，无权登录后台");
            }
            return Result.success(result);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    @PostMapping("/register")
    public Result<?> register(@Valid @RequestBody RegisterDTO registerDTO) {
        try {
            User user = userService.register(registerDTO);
            return Result.success(user);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    @GetMapping("/info")
    public Result<?> getUserInfo(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(Constants.JWT_USER_ID);
        if (userId == null) {
            return Result.error(401, "未登录或Token已过期");
        }
        try {
            User user = userService.getUserInfo(userId);
            if (user != null) {
                user.setPassword(null);
            }
            return Result.success(user);
        } catch (Exception e) {
            return Result.error("获取用户信息失败");
        }
    }
    
    @PutMapping("/info")
    public Result<?> updateUserInfo(HttpServletRequest request, @RequestBody User user) {
        Long userId = (Long) request.getAttribute(Constants.JWT_USER_ID);
        user.setId(userId);
        user.setPassword(null);
        user.setRole(null);
        user.setStatus(null);
        User updatedUser = userService.updateUser(userId, user);
        updatedUser.setPassword(null);
        return Result.success(updatedUser);
    }
    
    @PutMapping("/password")
    public Result<?> updatePassword(HttpServletRequest request, 
                                    @RequestBody Map<String, String> params) {
        Long userId = (Long) request.getAttribute(Constants.JWT_USER_ID);
        String oldPassword = params.get("oldPassword");
        String newPassword = params.get("newPassword");
        
        try {
            userService.updatePassword(userId, oldPassword, newPassword);
            return Result.success("密码修改成功");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    @GetMapping("/list")
    public Result<?> getUserList(@RequestParam(defaultValue = "1") Integer page,
                                  @RequestParam(defaultValue = "10") Integer pageSize) {
        // 管理员功能
        return Result.success(userService.page(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, pageSize)));
    }

    @PutMapping("/disable/{id}")
    public Result<?> disableUser(@PathVariable Long id) {
        try {
            userService.disableUser(id);
            return Result.success("禁用成功");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/enable/{id}")
    public Result<?> enableUser(@PathVariable Long id) {
        try {
            userService.enableUser(id);
            return Result.success("启用成功");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @DeleteMapping("/delete/{id}")
    public Result<?> deleteUser(@PathVariable Long id) {
        try {
            userService.deleteUser(id);
            return Result.success("删除成功");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}
