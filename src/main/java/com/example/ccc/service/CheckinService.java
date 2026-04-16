package com.example.ccc.service;

import com.example.ccc.common.RedisKeys;
import com.example.ccc.entity.CheckinVO;
import com.example.ccc.utils.RedisUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class CheckinService {

    private static final Logger logger = LoggerFactory.getLogger(CheckinService.class);

    private static final int DAILY_POINTS = 5;
    private static final int CONTINUOUS_BONUS_POINTS = 20;
    private static final int CONTINUOUS_BONUS_DAYS = 7;

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private LeaderboardService leaderboardService;

    public CheckinVO checkin(Long userId) {
        LocalDate today = LocalDate.now();
        String key = buildCheckinKey(userId, today);
        int dayOfMonth = today.getDayOfMonth();
        long offset = dayOfMonth - 1;

        Boolean alreadyChecked = redisUtil.getBit(key, offset);
        if (Boolean.TRUE.equals(alreadyChecked)) {
            logger.info("User {} already checked in today", userId);
            return getCheckinStatus(userId);
        }

        redisUtil.setBit(key, offset, true);

        int continuousDays = calculateContinuousDays(userId, today);
        int todayPoints = DAILY_POINTS;
        int bonusPoints = 0;

        if (continuousDays > 0 && continuousDays % CONTINUOUS_BONUS_DAYS == 0) {
            bonusPoints = CONTINUOUS_BONUS_POINTS;
            logger.info("User {} reached {} continuous days, bonus +{}", userId, continuousDays, bonusPoints);
        }

        int totalPoints = todayPoints + bonusPoints;
        leaderboardService.updateScore(userId, totalPoints);

        logger.info("User {} checked in, continuous={}, points=+{}", userId, continuousDays, totalPoints);

        CheckinVO vo = new CheckinVO();
        vo.setCheckedToday(true);
        vo.setContinuousDays(continuousDays);
        vo.setTodayPoints(todayPoints);
        vo.setBonusPoints(bonusPoints);
        vo.setTotalDays(getYearlyCheckinDays(userId, today.getYear()));
        vo.setMonthlyCheckins(getMonthlyCheckinDays(userId, today));

        return vo;
    }

    public CheckinVO getCheckinStatus(Long userId) {
        LocalDate today = LocalDate.now();
        String key = buildCheckinKey(userId, today);
        int dayOfMonth = today.getDayOfMonth();
        long offset = dayOfMonth - 1;

        Boolean checkedToday = redisUtil.getBit(key, offset);

        CheckinVO vo = new CheckinVO();
        vo.setCheckedToday(Boolean.TRUE.equals(checkedToday));
        vo.setContinuousDays(calculateContinuousDays(userId, today));
        vo.setTotalDays(getYearlyCheckinDays(userId, today.getYear()));
        vo.setMonthlyCheckins(getMonthlyCheckinDays(userId, today));

        return vo;
    }

    public List<Integer> getMonthlyCheckins(Long userId, YearMonth yearMonth) {
        String key = buildCheckinKey(userId, yearMonth.getYear(), yearMonth.getMonthValue());
        int daysInMonth = yearMonth.lengthOfMonth();
        List<Integer> checkinDays = new ArrayList<>();

        for (int day = 1; day <= daysInMonth; day++) {
            long offset = day - 1;
            Boolean checked = redisUtil.getBit(key, offset);
            if (Boolean.TRUE.equals(checked)) {
                checkinDays.add(day);
            }
        }

        return checkinDays;
    }

    private int calculateContinuousDays(Long userId, LocalDate today) {
        int continuousDays = 0;
        LocalDate checkDate = today;

        Boolean checkedToday = redisUtil.getBit(buildCheckinKey(userId, today), today.getDayOfMonth() - 1);
        if (!Boolean.TRUE.equals(checkedToday)) {
            checkDate = today.minusDays(1);
        }

        while (true) {
            String key = buildCheckinKey(userId, checkDate);
            int dayOfMonth = checkDate.getDayOfMonth();
            long offset = dayOfMonth - 1;

            Boolean checked = redisUtil.getBit(key, offset);
            if (Boolean.TRUE.equals(checked)) {
                continuousDays++;
                checkDate = checkDate.minusDays(1);
            } else {
                break;
            }

            if (continuousDays > 365) {
                break;
            }
        }

        return continuousDays;
    }

    private int getYearlyCheckinDays(Long userId, int year) {
        int totalDays = 0;
        for (int month = 1; month <= 12; month++) {
            String key = buildCheckinKey(userId, year, month);
            Long count = redisUtil.bitCount(key);
            if (count != null) {
                totalDays += count.intValue();
            }
        }
        return totalDays;
    }

    private List<Integer> getMonthlyCheckinDays(Long userId, LocalDate date) {
        return getMonthlyCheckins(userId, YearMonth.from(date));
    }

    private String buildCheckinKey(Long userId, LocalDate date) {
        return buildCheckinKey(userId, date.getYear(), date.getMonthValue());
    }

    private String buildCheckinKey(Long userId, int year, int month) {
        return RedisKeys.CHECKIN + userId + ":" + year + String.format("%02d", month);
    }
}
