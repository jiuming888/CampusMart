package com.campusmart.controller;

import com.campusmart.common.Constants;
import com.campusmart.common.Result;
import com.campusmart.entity.Message;
import com.campusmart.entity.User;
import com.campusmart.mapper.UserMapper;
import com.campusmart.service.MessageService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/message")
public class MessageController {
    
    @Autowired
    private MessageService messageService;
    
    @Autowired
    private UserMapper userMapper;
    
    @GetMapping("/conversations")
    public Result<?> getConversations(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(Constants.JWT_USER_ID);
        return Result.success(messageService.getConversationList(userId));
    }
    
    @GetMapping("/detail/{userId}")
    public Result<?> getMessageDetail(HttpServletRequest request, @PathVariable Long userId) {
        Long currentUserId = (Long) request.getAttribute(Constants.JWT_USER_ID);
        return Result.success(messageService.getMessageDetail(currentUserId, userId));
    }
    
    @PostMapping("/send")
    public Result<?> sendMessage(HttpServletRequest request, @RequestBody Map<String, Object> params) {
        Long fromUserId = (Long) request.getAttribute(Constants.JWT_USER_ID);
        Long toUserId = Long.parseLong(params.get("toUserId").toString());
        Long productId = params.containsKey("productId") ? 
                Long.parseLong(params.get("productId").toString()) : null;
        String content = params.get("content").toString();
        
        try {
            Message message = messageService.sendMessage(fromUserId, toUserId, productId, content);
            return Result.success(message);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    @GetMapping("/unread")
    public Result<?> getUnreadCount(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(Constants.JWT_USER_ID);
        return Result.success(messageService.getUnreadCount(userId));
    }
    
    @PutMapping("/read/{userId}")
    public Result<?> markAsRead(HttpServletRequest request, @PathVariable Long userId) {
        Long currentUserId = (Long) request.getAttribute(Constants.JWT_USER_ID);
        messageService.markAsRead(currentUserId, userId);
        return Result.success("标记已读成功");
    }
    
    @GetMapping("/user/{userId}")
    public Result<?> getUserInfo(@PathVariable("userId") String userId) {
        try {
            Long id = Long.parseLong(userId);
            User user = userMapper.selectById(id);
            if (user == null) {
                return Result.error("用户不存在");
            }
            Map<String, Object> userData = new HashMap<>();
            userData.put("id", user.getId());
            userData.put("nickname", user.getNickname() != null ? user.getNickname() : user.getUsername());
            userData.put("avatar", user.getAvatar() != null ? user.getAvatar() : "");
            return Result.success(userData);
        } catch (NumberFormatException e) {
            return Result.error("无效的用户ID");
        } catch (Exception e) {
            return Result.error("获取用户信息失败");
        }
    }
}
