package com.cupk.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cupk.entity.Post;
import com.cupk.mapper.PostMapper;
import com.cupk.service.PostService;
import com.cupk.service.PostLikeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PostServiceImpl extends ServiceImpl<PostMapper, Post> implements PostService {

    private final PostMapper postMapper;
    private final PostLikeService postLikeService;

    public PostServiceImpl(PostMapper postMapper, PostLikeService postLikeService) {
        this.postMapper = postMapper;
        this.postLikeService = postLikeService;
    }

    @Override
    @Transactional
    public boolean likePost(Long postId, Long userId) {
        // 检查帖子是否存在
        Post post = this.getById(postId);
        if (post == null) {
            return false;
        }

        // 尝试点赞
        boolean success = postLikeService.likePost(postId, userId);
        if (success) {
            // 更新帖子点赞数
            postMapper.incrementLikeCount(postId);
        }
        return success;
    }

    @Override
    @Transactional
    public boolean unlikePost(Long postId, Long userId) {
        // 检查帖子是否存在
        Post post = this.getById(postId);
        if (post == null) {
            return false;
        }

        // 尝试取消点赞
        boolean success = postLikeService.unlikePost(postId, userId);
        if (success) {
            // 更新帖子点赞数
            postMapper.decrementLikeCount(postId);
        }
        return success;
    }

    @Override
    public boolean hasLiked(Long postId, Long userId) {
        return postLikeService.hasLiked(postId, userId);
    }

    @Override
    public Long getLikeCount(Long postId) {
        return postLikeService.getLikeCount(postId);
    }
}