package com.campusmart.controller;

import com.campusmart.common.Result;
import com.campusmart.dto.ChatRequestDTO;
import com.campusmart.service.ChatbotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/chatbot")
public class ChatbotController {

    @Autowired
    private ChatbotService chatbotService;

    @PostMapping("/chat")
    public Result<?> chat(@RequestBody ChatRequestDTO dto) {
        try {
            String reply = chatbotService.chat(dto.getMessage(), dto.getHistory());
            return Result.success(Map.of("reply", reply));
        } catch (Exception e) {
            return Result.error("对话失败：" + e.getMessage());
        }
    }
}
