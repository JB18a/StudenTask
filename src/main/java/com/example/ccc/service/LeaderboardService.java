package com.example.ccc.service;

import com.example.ccc.common.RedisKeys;
import com.example.ccc.entity.LeaderboardVO;
import com.example.ccc.entity.User;
import com.example.ccc.repository.UserRepository;
import com.example.ccc.utils.RedisUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class LeaderboardService {

    private static final Logger logger = LoggerFactory.getLogger(LeaderboardService.class);
    private static final int TOP_N = 10;
    private static final int LOCK_EXPIRE_SECONDS = 5;

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private UserRepository userRepository;

    public List<LeaderboardVO> getTop10() {
        Set<ZSetOperations.TypedTuple<String>> tuples = redisUtil.zReverseRangeWithScores(
                RedisKeys.LEADERBOARD, 0, TOP_N - 1);

        if (tuples == null || tuples.isEmpty()) {
            logger.info("Redis leaderboard cache miss, loading from database...");
            return loadFromDatabase();
        }

        List<LeaderboardVO> result = new ArrayList<>();
        int rank = 1;
        for (ZSetOperations.TypedTuple<String> tuple : tuples) {
            String userIdStr = tuple.getValue();
            Double score = tuple.getScore();

            if (userIdStr == null || score == null) {
                continue;
            }

            Long userId = Long.parseLong(userIdStr);
            User user = userRepository.findById(userId).orElse(null);

            if (user != null) {
                LeaderboardVO vo = new LeaderboardVO(
                        rank++,
                        userId,
                        user.getUsername(),
                        user.getNickname(),
                        score.intValue());
                result.add(vo);
            }
        }

        return result;
    }

    public LeaderboardVO getUserRank(Long userId) {
        Long rank = redisUtil.zRank(RedisKeys.LEADERBOARD, userId.toString());
        Double score = redisUtil.zScore(RedisKeys.LEADERBOARD, userId.toString());

        if (rank == null || score == null) {
            User user = userRepository.findById(userId).orElse(null);
            if (user != null && user.getTotalScore() != null && user.getTotalScore() > 0) {
                syncUserScore(userId, user.getTotalScore());
                rank = redisUtil.zRank(RedisKeys.LEADERBOARD, userId.toString());
                score = user.getTotalScore().doubleValue();
            } else {
                return null;
            }
        }

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return null;
        }

        return new LeaderboardVO(
                rank != null ? rank.intValue() + 1 : null,
                userId,
                user.getUsername(),
                user.getNickname(),
                score != null ? score.intValue() : 0);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateScore(Long userId, int scoreDelta) {
        if (scoreDelta <= 0) {
            logger.warn("Invalid score delta: {} for user: {}", scoreDelta, userId);
            return;
        }

        String lockKey = RedisKeys.LEADERBOARD_UPDATE_LOCK + userId;
        boolean locked = false;

        try {
            locked = redisUtil.tryLock(lockKey, LOCK_EXPIRE_SECONDS);
            if (!locked) {
                logger.warn("Failed to acquire lock for user: {}, retry later", userId);
                return;
            }

            User user = userRepository.findById(userId).orElse(null);
            if (user == null) {
                logger.warn("User not found: {}", userId);
                return;
            }

            int newTotalScore = (user.getTotalScore() == null ? 0 : user.getTotalScore()) + scoreDelta;
            user.setTotalScore(newTotalScore);
            userRepository.save(user);

            redisUtil.zAdd(RedisKeys.LEADERBOARD, userId.toString(), newTotalScore);

            logger.info("Updated score for user {}: +{}, total={}", userId, scoreDelta, newTotalScore);

        } finally {
            if (locked) {
                redisUtil.unlock(lockKey);
            }
        }
    }

    public void syncUserScore(Long userId, int score) {
        redisUtil.zAdd(RedisKeys.LEADERBOARD, userId.toString(), score);
        logger.info("Synced score for user {}: {}", userId, score);
    }

    private List<LeaderboardVO> loadFromDatabase() {
        List<User> topUsers = userRepository.findTop10ByRoleOrderByTotalScoreDesc("USER");

        List<LeaderboardVO> result = new ArrayList<>();
        int rank = 1;

        for (User user : topUsers) {
            if (user.getTotalScore() != null && user.getTotalScore() > 0) {
                redisUtil.zAdd(RedisKeys.LEADERBOARD, user.getId().toString(), user.getTotalScore());

                LeaderboardVO vo = new LeaderboardVO(
                        rank++,
                        user.getId(),
                        user.getUsername(),
                        user.getNickname(),
                        user.getTotalScore());
                result.add(vo);
            }
        }

        return result;
    }

    public void rebuildCache() {
        logger.info("Starting to rebuild leaderboard cache...");

        List<User> allUsers = userRepository.findByRole("USER");
        int count = 0;

        for (User user : allUsers) {
            if (user.getTotalScore() != null && user.getTotalScore() > 0) {
                redisUtil.zAdd(RedisKeys.LEADERBOARD, user.getId().toString(), user.getTotalScore());
                count++;
            }
        }

        logger.info("Leaderboard cache rebuilt successfully, {} users cached", count);
    }
}
