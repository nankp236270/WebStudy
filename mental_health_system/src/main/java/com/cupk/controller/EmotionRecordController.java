package com.cupk.controller;

import com.cupk.entity.EmotionRecord;
import com.cupk.service.EmotionRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/emotion-records")
public class EmotionRecordController {
    @Autowired
    private EmotionRecordService emotionRecordService;

    @GetMapping
    public List<EmotionRecord> list() {
        return emotionRecordService.list();
    }

    @GetMapping("/{id}")
    public EmotionRecord get(@PathVariable Long id) {
        return emotionRecordService.getById(id);
    }

    @PostMapping
    public boolean add(@RequestBody EmotionRecord record) {
        return emotionRecordService.save(record);
    }

    @PutMapping
    public boolean update(@RequestBody EmotionRecord record) {
        return emotionRecordService.updateById(record);
    }

    @DeleteMapping("/{id}")
    public boolean delete(@PathVariable Long id) {
        return emotionRecordService.removeById(id);
    }
}