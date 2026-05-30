package com.campusmart.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campusmart.entity.Message;
import com.campusmart.entity.User;
import com.campusmart.mapper.MessageMapper;
import com.campusmart.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class MessageService extends ServiceImpl<MessageMapper, Message> {
    
    @Autowired
    private UserMapper userMapper;
    
    @Transactional
    public Message sendMessage(Long fromUserId, Long toUserId, Long productId, String content) {
        if (fromUserId.equals(toUserId)) {
            throw new RuntimeException("不能给自己发消息");
        }
        
        Message message = new Message();
        message.setFromUserId(fromUserId);
        message.setToUserId(toUserId);
        message.setProductId(productId);
        message.setContent(content);
        message.setIsRead(0);
        
        this.save(message);
        return message;
    }
    
    public List<Map<String, Object>> getConversationList(Long userId) {
        List<Message> messages = this.lambdaQuery()
                .and(wrapper -> wrapper
                        .eq(Message::getFromUserId, userId)
                        .or()
                        .eq(Message::getToUserId, userId))
                .orderByDesc(Message::getCreateTime)
                .list();
        
        // 按对话分组
        Map<Long, Map<String, Object>> conversationMap = new LinkedHashMap<>();
        
        for (Message msg : messages) {
            if (msg.getFromUserId() == null || msg.getToUserId() == null) {
                continue;
            }
            Long otherUserId = msg.getFromUserId().equals(userId) ? msg.getToUserId() : msg.getFromUserId();
            
            if (!conversationMap.containsKey(otherUserId)) {
                // 获取对方用户信息
                User otherUser = userMapper.selectById(otherUserId);
                
                Map<String, Object> conversation = new HashMap<>();
                conversation.put("userId", otherUserId);
                conversation.put("nickname", otherUser != null && otherUser.getNickname() != null ? otherUser.getNickname() : "未知用户");
                conversation.put("avatar", otherUser != null ? otherUser.getAvatar() : null);
                conversation.put("lastMessage", msg.getContent() != null ? msg.getContent() : "");
                conversation.put("lastTime", msg.getCreateTime());
                conversation.put("unreadCount", 0);
                conversationMap.put(otherUserId, conversation);
            }
            
            // 统计未读消息
            if (msg.getToUserId() != null && msg.getToUserId().equals(userId) && msg.getIsRead() != null && msg.getIsRead() == 0) {
                Map<String, Object> conv = conversationMap.get(otherUserId);
                if (conv != null) {
                    conv.put("unreadCount", ((Integer) conv.get("unreadCount")) + 1);
                }
            }
        }
        
        return new ArrayList<>(conversationMap.values());
    }
    
    public List<Message> getMessageDetail(Long userId, Long targetUserId) {
        List<Message> messages = this.lambdaQuery()
                .and(wrapper -> wrapper
                        .eq(Message::getFromUserId, userId)
                        .eq(Message::getToUserId, targetUserId)
                        .or()
                        .eq(Message::getFromUserId, targetUserId)
                        .eq(Message::getToUserId, userId))
                .orderByAsc(Message::getCreateTime)
                .list();
        
        // 填充用户信息
        User fromUser = userMapper.selectById(userId);
        User toUser = userMapper.selectById(targetUserId);
        
        for (Message msg : messages) {
            if (msg.getFromUserId() != null) {
                if (msg.getFromUserId().equals(userId) && fromUser != null) {
                    msg.setFromUserNickname(fromUser.getNickname());
                    msg.setFromUserAvatar(fromUser.getAvatar());
                }
                if (msg.getFromUserId().equals(targetUserId) && toUser != null) {
                    msg.setFromUserNickname(toUser.getNickname());
                    msg.setFromUserAvatar(toUser.getAvatar());
                }
            }
        }
        
        // 标记已读
        this.lambdaUpdate()
                .eq(Message::getToUserId, userId)
                .eq(Message::getFromUserId, targetUserId)
                .eq(Message::getIsRead, 0)
                .set(Message::getIsRead, 1)
                .update();
        
        return messages;
    }
    
    public int getUnreadCount(Long userId) {
        if (userId == null) {
            return 0;
        }
        long count = this.lambdaQuery()
                .eq(Message::getToUserId, userId)
                .eq(Message::getIsRead, 0)
                .count();
        return (int) count;
    }
    
    @Transactional
    public void markAsRead(Long userId, Long targetUserId) {
        if (userId == null || targetUserId == null) {
            return;
        }
        this.lambdaUpdate()
                .eq(Message::getToUserId, userId)
                .eq(Message::getFromUserId, targetUserId)
                .eq(Message::getIsRead, 0)
                .set(Message::getIsRead, 1)
                .update();
    }
}
