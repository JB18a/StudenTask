package com.example.ccc.entity;

public class LikeVO {

    private Boolean liked;
    private Long likeCount;

    public LikeVO() {}

    public LikeVO(Boolean liked, Long likeCount) {
        this.liked = liked;
        this.likeCount = likeCount;
    }

    public Boolean getLiked() { return liked; }
    public void setLiked(Boolean liked) { this.liked = liked; }

    public Long getLikeCount() { return likeCount; }
    public void setLikeCount(Long likeCount) { this.likeCount = likeCount; }
}
