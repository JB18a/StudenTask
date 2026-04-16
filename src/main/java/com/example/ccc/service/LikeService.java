package com.example.ccc.service;

import com.example.ccc.common.RedisKeys;
import com.example.ccc.entity.ExcellentWorkVO;
import com.example.ccc.entity.LikeVO;
import com.example.ccc.entity.Task;
import com.example.ccc.entity.TaskSubmission;
import com.example.ccc.entity.User;
import com.example.ccc.repository.TaskRepository;
import com.example.ccc.repository.TaskSubmissionRepository;
import com.example.ccc.repository.UserRepository;
import com.example.ccc.utils.RedisUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class LikeService {

    private static final Logger logger = LoggerFactory.getLogger(LikeService.class);

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private TaskSubmissionRepository submissionRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private UserRepository userRepository;

    public LikeVO like(Long submissionId, Long userId) {
        String key = RedisKeys.SUBMISSION_LIKES + submissionId;

        Boolean isMember = redisUtil.sIsMember(key, userId.toString());
        if (Boolean.TRUE.equals(isMember)) {
            Long count = redisUtil.sCard(key);
            return new LikeVO(true, count);
        }

        redisUtil.sAdd(key, userId.toString());

        Long likeCount = redisUtil.sCard(key);

        asyncUpdateLikeCount(submissionId, likeCount);

        logger.info("User {} liked submission {}, total likes: {}", userId, submissionId, likeCount);

        return new LikeVO(true, likeCount);
    }

    public LikeVO unlike(Long submissionId, Long userId) {
        String key = RedisKeys.SUBMISSION_LIKES + submissionId;

        Boolean isMember = redisUtil.sIsMember(key, userId.toString());
        if (!Boolean.TRUE.equals(isMember)) {
            Long count = redisUtil.sCard(key);
            return new LikeVO(false, count);
        }

        redisUtil.sRemove(key, userId.toString());

        Long likeCount = redisUtil.sCard(key);

        asyncUpdateLikeCount(submissionId, likeCount);

        logger.info("User {} unliked submission {}, total likes: {}", userId, submissionId, likeCount);

        return new LikeVO(false, likeCount);
    }

    public LikeVO getLikeStatus(Long submissionId, Long userId) {
        String key = RedisKeys.SUBMISSION_LIKES + submissionId;

        Boolean liked = redisUtil.sIsMember(key, userId.toString());
        Long likeCount = redisUtil.sCard(key);

        return new LikeVO(Boolean.TRUE.equals(liked), likeCount != null ? likeCount : 0L);
    }

    public List<ExcellentWorkVO> getExcellentWorks(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<TaskSubmission> submissions = submissionRepository.findByIsExcellentTrueOrderBySubmitTimeDesc(pageable);

        List<ExcellentWorkVO> result = new ArrayList<>();
        for (TaskSubmission submission : submissions) {
            ExcellentWorkVO vo = convertToVO(submission, userId);
            result.add(vo);
        }

        return result;
    }

    @Transactional
    public void markAsExcellent(Long submissionId) {
        TaskSubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new RuntimeException("提交记录不存在"));

        if (submission.getScore() == null) {
            throw new RuntimeException("该作业尚未评分，无法标记为优秀");
        }

        submission.setIsExcellent(true);
        submissionRepository.save(submission);

        logger.info("Submission {} marked as excellent", submissionId);
    }

    @Transactional
    public void unmarkAsExcellent(Long submissionId) {
        TaskSubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new RuntimeException("提交记录不存在"));

        submission.setIsExcellent(false);
        submissionRepository.save(submission);

        logger.info("Submission {} unmarked as excellent", submissionId);
    }

    @Async
    protected void asyncUpdateLikeCount(Long submissionId, Long likeCount) {
        try {
            TaskSubmission submission = submissionRepository.findById(submissionId).orElse(null);
            if (submission != null) {
                submission.setLikeCount(likeCount.intValue());
                submissionRepository.save(submission);
                logger.debug("Async updated like count for submission {}: {}", submissionId, likeCount);
            }
        } catch (Exception e) {
            logger.error("Failed to async update like count for submission {}: {}", submissionId, e.getMessage());
        }
    }

    private ExcellentWorkVO convertToVO(TaskSubmission submission, Long currentUserId) {
        ExcellentWorkVO vo = new ExcellentWorkVO();

        vo.setSubmissionId(submission.getId());
        vo.setTaskId(submission.getTaskId());
        vo.setUserId(submission.getUserId());
        vo.setFileUrl(submission.getFileUrl());
        vo.setFileName(submission.getFileName());
        vo.setScore(submission.getScore());
        vo.setFeedback(submission.getFeedback());
        vo.setSubmitTime(submission.getSubmitTime());

        Task task = taskRepository.findById(submission.getTaskId()).orElse(null);
        if (task != null) {
            vo.setTaskTitle(task.getTitle());
        }

        User user = userRepository.findById(submission.getUserId()).orElse(null);
        if (user != null) {
            vo.setUsername(user.getUsername());
            vo.setNickname(user.getNickname());
        }

        String likeKey = RedisKeys.SUBMISSION_LIKES + submission.getId();
        Long likeCount = redisUtil.sCard(likeKey);
        vo.setLikeCount(likeCount != null ? likeCount : 0L);

        if (currentUserId != null) {
            Boolean liked = redisUtil.sIsMember(likeKey, currentUserId.toString());
            vo.setLiked(Boolean.TRUE.equals(liked));
        } else {
            vo.setLiked(false);
        }

        return vo;
    }
}
