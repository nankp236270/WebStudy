package com.cupk.service;

public interface PostLikeService {
    /**
     * 点赞帖子
     * 
     * @param postId 帖子ID
     * @param userId 用户ID
     * @return 是否点赞成功
     */
    boolean likePost(Long postId, Long userId);

    /**
     * 取消点赞帖子
     * 
     * @param postId 帖子ID
     * @param userId 用户ID
     * @return 是否取消成功
     */
    boolean unlikePost(Long postId, Long userId);

    /**
     * 检查用户是否已点赞帖子
     * 
     * @param postId 帖子ID
     * @param userId 用户ID
     * @return 是否已点赞
     */
    boolean hasLiked(Long postId, Long userId);

    /**
     * 获取帖子点赞数
     * 
     * @param postId 帖子ID
     * @return 点赞数
     */
    Long getLikeCount(Long postId);
}