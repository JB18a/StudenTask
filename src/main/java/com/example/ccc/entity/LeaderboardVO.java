package com.example.ccc.entity;

public class LeaderboardVO {

    private Integer rank;
    private Long userId;
    private String username;
    private String nickname;
    private Integer totalScore;

    public LeaderboardVO() {}

    public LeaderboardVO(Integer rank, Long userId, String username, String nickname, Integer totalScore) {
        this.rank = rank;
        this.userId = userId;
        this.username = username;
        this.nickname = nickname;
        this.totalScore = totalScore;
    }

    public Integer getRank() { return rank; }
    public void setRank(Integer rank) { this.rank = rank; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }

    public Integer getTotalScore() { return totalScore; }
    public void setTotalScore(Integer totalScore) { this.totalScore = totalScore; }
}
