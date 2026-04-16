package com.example.ccc.controller;

import com.example.ccc.common.Result;
import com.example.ccc.entity.LeaderboardVO;
import com.example.ccc.service.LeaderboardService;
import com.example.ccc.utils.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/leaderboard")
public class LeaderboardController {

    @Autowired
    private LeaderboardService leaderboardService;

    @GetMapping("/top10")
    public Result<List<LeaderboardVO>> getTop10() {
        List<LeaderboardVO> top10 = leaderboardService.getTop10();
        return Result.success(top10);
    }

    @GetMapping("/my-rank")
    public Result<LeaderboardVO> getMyRank() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return Result.error(401, "请先登录");
        }

        LeaderboardVO myRank = leaderboardService.getUserRank(userId);
        if (myRank == null) {
            return Result.error(404, "暂无积分记录");
        }

        return Result.success(myRank);
    }

    @GetMapping("/user/{userId}")
    public Result<LeaderboardVO> getUserRank(@PathVariable Long userId) {
        LeaderboardVO userRank = leaderboardService.getUserRank(userId);
        if (userRank == null) {
            return Result.error(404, "用户暂无积分记录");
        }
        return Result.success(userRank);
    }
}
