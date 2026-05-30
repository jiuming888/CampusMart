package com.campusmart.config;

import com.campusmart.interceptor.JwtInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    
    @Value("${app.upload.path:./uploads}")
    private String uploadPath;
    
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new JwtInterceptor())
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                    "/api/user/login",
                    "/api/user/register",
                    "/api/user/admin/login",
                    "/api/category/list",
                    "/api/product/list",
                    "/api/product/detail/**",
                    "/api/product/user/**",
                    "/api/comment/list/**",
                    "/api/product/hot",
                    "/api/product/latest",
                    "/api/alipay/**",
                    "/uploads/**",
                    "/images/**"
                );
    }
    
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 上传图片存到 public/images/，开发时 Vite 直接提供；生产时由后端提供
        String absolutePath = new File(uploadPath).getAbsolutePath();
        registry.addResourceHandler("/images/**")
                .addResourceLocations("file:" + absolutePath + File.separator);
        // 兼容旧版 /uploads/ 路径
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + absolutePath + File.separator);
    }
}
