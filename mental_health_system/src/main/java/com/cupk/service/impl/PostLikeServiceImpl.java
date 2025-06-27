package com.cupk.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cupk.entity.PostLike;
import com.cupk.mapper.PostLikeMapper;
import com.cupk.service.PostLikeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class PostLikeServiceImpl implements PostLikeService {

    private final PostLikeMapper postLikeMapper;

    public PostLikeServiceImpl(PostLikeMapper postLikeMapper) {
        this.postLikeMapper = postLikeMapper;
    }

    @Override
    @Transactional
    public boolean likePost(Long postId, Long userId) {
        // 检查是否已经点赞
        if (hasLiked(postId, userId)) {
            return false;
        }

        // 创建新的点赞记录
        PostLike postLike = new PostLike();
        postLike.setPostId(postId);
        postLike.setUserId(userId);
        postLike.setCreateTime(LocalDateTime.now());

        return postLikeMapper.insert(postLike) > 0;
    }

    @Override
    @Transactional
    public boolean unlikePost(Long postId, Long userId) {
        LambdaQueryWrapper<PostLike> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PostLike::getPostId, postId)
                .eq(PostLike::getUserId, userId);

        return postLikeMapper.delete(wrapper) > 0;
    }

    @Override
    public boolean hasLiked(Long postId, Long userId) {
        LambdaQueryWrapper<PostLike> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PostLike::getPostId, postId)
                .eq(PostLike::getUserId, userId);

        return postLikeMapper.selectCount(wrapper) > 0;
    }

    @Override
    public Long getLikeCount(Long postId) {
        LambdaQueryWrapper<PostLike> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PostLike::getPostId, postId);

        return postLikeMapper.selectCount(wrapper);
    }
}