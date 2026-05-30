package com.campusmart.interceptor;

import com.campusmart.common.Constants;
import com.campusmart.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.HashMap;
import java.util.Map;

public class JwtInterceptor implements HandlerInterceptor {
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // OPTIONS 请求直接放行
        if ("OPTIONS".equals(request.getMethod())) {
            return true;
        }
        
        String token = request.getHeader(Constants.JWT_TOKEN);
        
        if (!StringUtils.hasText(token)) {
            // 尝试从请求参数中获取
            token = request.getParameter(Constants.JWT_TOKEN);
        }
        
        if (!StringUtils.hasText(token)) {
            // 尝试从 Header 中获取 (大写 Token 或小写 token)
            token = request.getHeader("Token");
        }
        
        if (!StringUtils.hasText(token)) {
            token = request.getHeader("token");
        }
        
        if (!StringUtils.hasText(token)) {
            Map<String, Object> result = new HashMap<>();
            result.put("code", 401);
            result.put("message", "未登录或Token已过期");
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(new ObjectMapper().writeValueAsString(result));
            return false;
        }
        
        try {
            Claims claims = JwtUtil.parseToken(token);
            request.setAttribute(Constants.JWT_USER_ID, Long.parseLong(claims.get(Constants.JWT_USER_ID).toString()));
            request.setAttribute(Constants.JWT_ROLE, claims.get(Constants.JWT_ROLE));
            return true;
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("code", 401);
            result.put("message", "Token无效或已过期");
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(new ObjectMapper().writeValueAsString(result));
            return false;
        }
    }
}
