package com.example.ccc.controller;

import com.example.ccc.common.Result;
import com.example.ccc.entity.CheckinVO;
import com.example.ccc.service.CheckinService;
import com.example.ccc.utils.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/checkin")
public class CheckinController {

    @Autowired
    private CheckinService checkinService;

    @PostMapping
    public Result<CheckinVO> checkin() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return Result.error(401, "请先登录");
        }

        CheckinVO result = checkinService.checkin(userId);
        return Result.success(result);
    }

    @GetMapping("/status")
    public Result<CheckinVO> getStatus() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return Result.error(401, "请先登录");
        }

        CheckinVO status = checkinService.getCheckinStatus(userId);
        return Result.success(status);
    }

    @GetMapping("/monthly/{year}/{month}")
    public Result<List<Integer>> getMonthlyCheckins(
            @PathVariable int year,
            @PathVariable int month) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return Result.error(401, "请先登录");
        }

        java.time.YearMonth yearMonth = java.time.YearMonth.of(year, month);
        List<Integer> checkins = checkinService.getMonthlyCheckins(userId, yearMonth);
        return Result.success(checkins);
    }
}
