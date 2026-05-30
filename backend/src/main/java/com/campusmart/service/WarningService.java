package com.campusmart.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campusmart.entity.Warning;
import com.campusmart.mapper.WarningMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WarningService extends ServiceImpl<WarningMapper, Warning> {
    
    public Warning sendWarning(Long adminId, Long userId, String reason) {
        Warning warning = new Warning();
        warning.setAdminId(adminId);
        warning.setUserId(userId);
        warning.setReason(reason);
        warning.setIsRead(0);
        this.save(warning);
        return warning;
    }
    
    public List<Warning> getUserWarnings(Long userId) {
        return baseMapper.findByUserId(userId);
    }
    
    public List<Warning> getAllWarnings() {
        return baseMapper.findAllWithUserInfo();
    }
    
    public void markAsRead(Long warningId) {
        Warning warning = this.getById(warningId);
        if (warning != null) {
            warning.setIsRead(1);
            this.updateById(warning);
        }
    }
    
    public void markAllAsRead(Long userId) {
        LambdaQueryWrapper<Warning> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Warning::getUserId, userId)
               .eq(Warning::getIsRead, 0);
        List<Warning> warnings = this.list(wrapper);
        warnings.forEach(w -> w.setIsRead(1));
        this.updateBatchById(warnings);
    }
    
    public int getUnreadCount(Long userId) {
        LambdaQueryWrapper<Warning> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Warning::getUserId, userId)
               .eq(Warning::getIsRead, 0);
        return (int) this.count(wrapper);
    }
}
