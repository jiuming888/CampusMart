package com.campusmart.controller;

import com.campusmart.common.Constants;
import com.campusmart.common.Result;
import com.campusmart.entity.Warning;
import com.campusmart.service.WarningService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/warning")
public class WarningController {
    
    @Autowired
    private WarningService warningService;
    
    @PostMapping("/send")
    public Result<?> sendWarning(HttpServletRequest request, @RequestBody Map<String, Object> params) {
        String role = (String) request.getAttribute(Constants.JWT_ROLE);
        
        if (!Constants.ROLE_ADMIN.equals(role)) {
            return Result.error("只有管理员可以发送警告");
        }
        
        Long userId = Long.valueOf(params.get("userId").toString());
        String reason = params.get("reason").toString();
        Long adminId = (Long) request.getAttribute(Constants.JWT_USER_ID);
        
        Warning warning = warningService.sendWarning(adminId, userId, reason);
        return Result.success(warning);
    }
    
    @GetMapping("/my")
    public Result<?> getMyWarnings(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(Constants.JWT_USER_ID);
        List<Warning> warnings = warningService.getUserWarnings(userId);
        return Result.success(warnings);
    }
    
    @GetMapping("/all")
    public Result<?> getAllWarnings(HttpServletRequest request) {
        String role = (String) request.getAttribute(Constants.JWT_ROLE);
        
        if (!Constants.ROLE_ADMIN.equals(role)) {
            return Result.error("只有管理员可以查看所有警告");
        }
        
        List<Warning> warnings = warningService.getAllWarnings();
        return Result.success(warnings);
    }
    
    @PutMapping("/read/{id}")
    public Result<?> markAsRead(@PathVariable Long id) {
        warningService.markAsRead(id);
        return Result.success();
    }
    
    @PutMapping("/readAll")
    public Result<?> markAllAsRead(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(Constants.JWT_USER_ID);
        warningService.markAllAsRead(userId);
        return Result.success();
    }
    
    @GetMapping("/unreadCount")
    public Result<?> getUnreadCount(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(Constants.JWT_USER_ID);
        int count = warningService.getUnreadCount(userId);
        return Result.success(count);
    }
}
