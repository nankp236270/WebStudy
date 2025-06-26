package com.cupk.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cupk.entity.AiConversation;
import com.cupk.mapper.AiConversationMapper;
import com.cupk.service.AiConversationService;
import org.springframework.stereotype.Service;

@Service
public class AiConversationServiceImpl extends ServiceImpl<AiConversationMapper, AiConversation>
        implements AiConversationService {
}