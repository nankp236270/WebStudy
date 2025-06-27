package com.cupk.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cupk.entity.Post;
import com.cupk.service.PostService;
import com.cupk.service.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/post")
public class PostController {
    @Autowired
    private PostService postService;

    // 1. 发帖
    @PostMapping
    public Result<?> addPost(@RequestBody Post post) {
        postService.save(post);
        return Result.success();
    }

    // 2. 帖子列表（分页）
    @GetMapping
    public Result<?> listPost(@RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<Post> postPage = postService.page(new Page<>(page, size),
                new QueryWrapper<Post>().orderByDesc("create_time"));
        return Result.success(postPage);
    }

    // 3. 帖子详情
    @GetMapping("/{id}")
    public Result<?> postDetail(@PathVariable Long id) {
        Post post = postService.getById(id);
        return Result.success(post);
    }
}