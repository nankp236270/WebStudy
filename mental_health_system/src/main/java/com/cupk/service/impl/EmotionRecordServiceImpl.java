package com.cupk.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cupk.entity.EmotionRecord;
import com.cupk.mapper.EmotionRecordMapper;
import com.cupk.service.EmotionRecordService;
import org.springframework.stereotype.Service;

@Service
public class EmotionRecordServiceImpl extends ServiceImpl<EmotionRecordMapper, EmotionRecord>
        implements EmotionRecordService {
}