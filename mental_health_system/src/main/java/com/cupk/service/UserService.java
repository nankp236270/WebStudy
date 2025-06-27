package com.cupk.service;

import com.cupk.dto.UserRegisterDTO;
import com.cupk.dto.UserLoginDTO;
import com.cupk.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;

public interface UserService extends IService<User> {
    void register(UserRegisterDTO dto);

    String login(UserLoginDTO dto);

    User findByUsername(String username);

    User findByEmail(String email);
}
