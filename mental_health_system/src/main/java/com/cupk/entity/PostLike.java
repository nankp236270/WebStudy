package com.cupk.entity;

import java.time.LocalDateTime;

public class PostLike {
    private Long id;
    private Long postId;
    private Long userId;
    private LocalDateTime createTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}