package com.cupk.controller;

import com.cupk.service.PostLikeService;
import com.cupk.service.Result;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/post-likes")
public class PostLikeController {

    private final PostLikeService postLikeService;

    public PostLikeController(PostLikeService postLikeService) {
        this.postLikeService = postLikeService;
    }

    @PostMapping("/{postId}")
    public Result<Boolean> likePost(@PathVariable Long postId, @RequestParam Long userId) {
        boolean success = postLikeService.likePost(postId, userId);
        return success ? Result.success(true) : Result.error("已经点赞过该帖子");
    }

    @DeleteMapping("/{postId}")
    public Result<Boolean> unlikePost(@PathVariable Long postId, @RequestParam Long userId) {
        boolean success = postLikeService.unlikePost(postId, userId);
        return success ? Result.success(true) : Result.error("未点赞该帖子");
    }

    @GetMapping("/{postId}/status")
    public Result<Boolean> hasLiked(@PathVariable Long postId, @RequestParam Long userId) {
        boolean hasLiked = postLikeService.hasLiked(postId, userId);
        return Result.success(hasLiked);
    }

    @GetMapping("/{postId}/count")
    public Result<Long> getLikeCount(@PathVariable Long postId) {
        Long count = postLikeService.getLikeCount(postId);
        return Result.success(count);
    }
}