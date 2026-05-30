package com.campusmart.controller;

import com.campusmart.common.Constants;
import com.campusmart.common.Result;
import com.campusmart.entity.Comment;
import com.campusmart.service.CommentService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/comment")
public class CommentController {
    
    @Autowired
    private CommentService commentService;
    
    @GetMapping("/list/{productId}")
    public Result<?> getCommentList(@PathVariable Long productId,
                                   @RequestParam(defaultValue = "1") Integer page,
                                   @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(commentService.getCommentList(productId, page, pageSize));
    }
    
    @PostMapping("/add")
    public Result<?> addComment(HttpServletRequest request, @RequestBody Map<String, Object> params) {
        Long userId = (Long) request.getAttribute(Constants.JWT_USER_ID);
        Long productId = Long.parseLong(params.get("productId").toString());
        String content = params.get("content").toString();
        Integer rating = params.containsKey("rating") ?
                Integer.parseInt(params.get("rating").toString()) : 5;
        Long parentId = null;
        if (params.containsKey("parentId") && params.get("parentId") != null) {
            parentId = Long.parseLong(params.get("parentId").toString());
        }

        Comment comment = commentService.addComment(productId, userId, content, rating, parentId);
        return Result.success(comment);
    }
}
