package com.campusmart.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campusmart.entity.Comment;
import com.campusmart.entity.User;
import com.campusmart.mapper.CommentMapper;
import com.campusmart.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CommentService extends ServiceImpl<CommentMapper, Comment> {

    @Autowired
    private UserMapper userMapper;

    public IPage<Comment> getCommentList(Long productId, Integer page, Integer pageSize) {
        Page<Comment> pageParam = new Page<>(page, pageSize);
        // 只获取顶级评论（parentId 为 NULL）
        IPage<Comment> result = this.baseMapper.selectCommentPageByProductId(pageParam, productId, null);

        // 加载每个顶级评论的回复
        for (Comment comment : result.getRecords()) {
            List<Comment> replies = getReplies(comment.getId());
            comment.setReplies(replies);
        }

        return result;
    }

    public Comment addComment(Long productId, Long userId, String content, Integer rating, Long parentId) {
        Comment comment = new Comment();
        comment.setProductId(productId);
        comment.setUserId(userId);
        comment.setContent(content);
        comment.setRating(rating);
        comment.setParentId(parentId);

        this.save(comment);

        // 填充用户信息
        User user = userMapper.selectById(userId);
        if (user != null) {
            comment.setUserNickname(user.getNickname());
            comment.setUserAvatar(user.getAvatar());
        }

        return comment;
    }

    // 获取评论的所有回复
    public List<Comment> getReplies(Long parentId) {
        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Comment::getParentId, parentId)
               .orderByAsc(Comment::getCreateTime);
        List<Comment> replies = this.list(wrapper);

        // 填充回复的用户信息
        for (Comment reply : replies) {
            User user = userMapper.selectById(reply.getUserId());
            if (user != null) {
                reply.setUserNickname(user.getNickname());
                reply.setUserAvatar(user.getAvatar());
            }
        }

        return replies;
    }
}
