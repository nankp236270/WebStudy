package com.cupk.entity;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class Post {
    private Long id;
    private Long userId;
    private String title;
    private String content;
    private String images;
    private String tags;
    private Integer likeCount;
    private Integer commentCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}