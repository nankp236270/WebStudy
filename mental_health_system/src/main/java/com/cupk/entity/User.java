package com.cupk.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("user")
public class User {
    @TableId
    private Long id;
    private String username;
    private String password;
    private String email;
    private String role;
    private String avatar;
    private LocalDateTime createTime;
}