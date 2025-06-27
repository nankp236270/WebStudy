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

    // 4. 点赞帖子
    @PostMapping("/{id}/like")
    public Result<?> likePost(@PathVariable("id") Long postId, @RequestParam Long userId) {
        boolean success = postService.likePost(postId, userId);
        return success ? Result.success() : Result.error("点赞失败，可能已经点赞过或帖子不存在");
    }

    // 5. 取消点赞
    @DeleteMapping("/{id}/like")
    public Result<?> unlikePost(@PathVariable("id") Long postId, @RequestParam Long userId) {
        boolean success = postService.unlikePost(postId, userId);
        return success ? Result.success() : Result.error("取消点赞失败，可能尚未点赞或帖子不存在");
    }

    // 6. 获取点赞状态
    @GetMapping("/{id}/like/status")
    public Result<?> getLikeStatus(@PathVariable("id") Long postId, @RequestParam Long userId) {
        boolean hasLiked = postService.hasLiked(postId, userId);
        return Result.success(hasLiked);
    }

    // 7. 获取点赞数
    @GetMapping("/{id}/like/count")
    public Result<?> getLikeCount(@PathVariable("id") Long postId) {
        Long count = postService.getLikeCount(postId);
        return Result.success(count);
    }
}