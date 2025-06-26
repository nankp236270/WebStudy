package com.cupk.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("ai_conversation")
public class AiConversation {
    @TableId
    private Long id;
    private Long userId;
    private String sessionId;
    private String userMessage;
    private String aiResponse;
    private String emotionAnalysis;
    private LocalDateTime createdAt;
}