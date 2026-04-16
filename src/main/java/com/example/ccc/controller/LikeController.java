package com.example.ccc.controller;

import com.example.ccc.common.Result;
import com.example.ccc.entity.ExcellentWorkVO;
import com.example.ccc.entity.LikeVO;
import com.example.ccc.entity.User;
import com.example.ccc.repository.UserRepository;
import com.example.ccc.service.LikeService;
import com.example.ccc.utils.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class LikeController {

    @Autowired
    private LikeService likeService;

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/submissions/{submissionId}/like")
    public Result<LikeVO> like(@PathVariable Long submissionId) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return Result.error(401, "请先登录");
        }

        LikeVO result = likeService.like(submissionId, userId);
        return Result.success(result);
    }

    @DeleteMapping("/submissions/{submissionId}/like")
    public Result<LikeVO> unlike(@PathVariable Long submissionId) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return Result.error(401, "请先登录");
        }

        LikeVO result = likeService.unlike(submissionId, userId);
        return Result.success(result);
    }

    @GetMapping("/submissions/{submissionId}/like")
    public Result<LikeVO> getLikeStatus(@PathVariable Long submissionId) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return Result.error(401, "请先登录");
        }

        LikeVO result = likeService.getLikeStatus(submissionId, userId);
        return Result.success(result);
    }

    @GetMapping("/excellent-works")
    public Result<List<ExcellentWorkVO>> getExcellentWorks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long userId = UserContext.getUserId();
        List<ExcellentWorkVO> works = likeService.getExcellentWorks(userId, page, size);
        return Result.success(works);
    }

    @PostMapping("/submissions/{submissionId}/excellent")
    public Result<String> markAsExcellent(@PathVariable Long submissionId) {
        Long currentUserId = UserContext.getUserId();
        if (currentUserId == null) {
            return Result.error(401, "请先登录");
        }

        User currentUser = userRepository.findById(currentUserId).orElse(null);
        if (currentUser == null || !"PUBLISHER".equals(currentUser.getRole())) {
            return Result.error(403, "权限不足：只有发布者才能标记优秀作业");
        }

        likeService.markAsExcellent(submissionId);
        return Result.success("已标记为优秀作业");
    }

    @DeleteMapping("/submissions/{submissionId}/excellent")
    public Result<String> unmarkAsExcellent(@PathVariable Long submissionId) {
        Long currentUserId = UserContext.getUserId();
        if (currentUserId == null) {
            return Result.error(401, "请先登录");
        }

        User currentUser = userRepository.findById(currentUserId).orElse(null);
        if (currentUser == null || !"PUBLISHER".equals(currentUser.getRole())) {
            return Result.error(403, "权限不足：只有发布者才能取消优秀标记");
        }

        likeService.unmarkAsExcellent(submissionId);
        return Result.success("已取消优秀作业标记");
    }
}
