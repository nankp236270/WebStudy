package com.cupk.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("post")
public class Post {
    @TableId
    private Long id;
    private Long userId;
    private String content;
    private String emotionTag;
    private Boolean anonymous;
    private LocalDateTime createTime;
}