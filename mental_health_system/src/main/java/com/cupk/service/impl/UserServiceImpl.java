package com.cupk.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cupk.entity.User;
import com.cupk.mapper.UserMapper;
import com.cupk.service.UserService;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
}