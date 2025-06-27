package com.cupk.controller;

import com.cupk.ai.DeepSeekService;
import com.cupk.ai.DeepSeekRequest;
import com.cupk.ai.EmotionData;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final DeepSeekService deepSeekService;

    public ChatController(DeepSeekService deepSeekService) {
        this.deepSeekService = deepSeekService;
    }

    @PostMapping
    public Map<String, Object> chat(@RequestBody List<DeepSeekRequest.Message> messages) throws Exception {
        String aiReply = deepSeekService.chatWithAI(messages);
        EmotionData emotionData = null;
        try {
            emotionData = deepSeekService.parseEmotion(aiReply);
        } catch (Exception e) {
            // 若没有情绪JSON，emotionData为null
        }
        return Map.of(
                "aiReply", aiReply,
                "emotion", emotionData
        );
    }
}
