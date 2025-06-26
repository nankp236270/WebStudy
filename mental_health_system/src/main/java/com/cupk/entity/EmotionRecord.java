package com.cupk.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("emotion_record")
public class EmotionRecord {
    @TableId
    private Long id;
    private Long userId;
    private String content;
    private String emotionType;
    private BigDecimal confidence;
    private LocalDateTime recordTime;
    private String suggestions;
}