package com.campusmart.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@RestController
@RequestMapping("/api/upload")
public class UploadController {

    @Value("${app.upload.path:./uploads}")
    private String uploadPath;

    /**
     * 上传图片
     * @param file 图片文件
     * @param type 上传类型: product(商品图片) / avatar(用户头像)
     */
    @PostMapping("/image")
    public com.campusmart.common.Result<String> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "type", defaultValue = "product") String type) {
        if (file.isEmpty()) {
            return com.campusmart.common.Result.error("请选择要上传的文件");
        }

        // 校验上传类型
        if (!type.equals("product") && !type.equals("avatar")) {
            return com.campusmart.common.Result.error("不支持的上传类型");
        }

        // 检查文件类型
        String originalFilename = file.getOriginalFilename();
        String suffix = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
        if (!suffix.matches("\\.(jpg|jpeg|png|gif|webp)")) {
            return com.campusmart.common.Result.error("只支持 jpg、jpeg、png、gif、webp 格式的图片");
        }

        // 检查文件大小（最大5MB）
        if (file.getSize() > 5 * 1024 * 1024) {
            return com.campusmart.common.Result.error("文件大小不能超过5MB");
        }

        try {
            // 确保上传目录存在: public/images/products/ 或 public/images/avatars/
            String subDir = getSubDir(type);
            Path uploadDir = Paths.get(uploadPath, subDir).toAbsolutePath().normalize();
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }

            // 生成文件名
            String filename = UUID.randomUUID().toString() + suffix;
            Path filePath = uploadDir.resolve(filename);

            // 保存文件
            file.transferTo(filePath.toFile());

            // 返回绝对路径
            String url = "/images/" + subDir + "/" + filename;
            return com.campusmart.common.Result.success(url);
        } catch (IOException e) {
            return com.campusmart.common.Result.error("上传失败：" + e.getMessage());
        }
    }

    private String getSubDir(String type) {
        if ("avatar".equals(type)) {
            return "avatars";
        }
        return "products";
    }
}
