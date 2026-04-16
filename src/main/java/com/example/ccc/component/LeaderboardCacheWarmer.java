package com.example.ccc.component;

import com.example.ccc.service.LeaderboardService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class LeaderboardCacheWarmer implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(LeaderboardCacheWarmer.class);

    @Autowired
    private LeaderboardService leaderboardService;

    @Override
    public void run(ApplicationArguments args) {
        logger.info("Starting leaderboard cache warm-up...");
        try {
            leaderboardService.rebuildCache();
            logger.info("Leaderboard cache warm-up completed successfully");
        } catch (Exception e) {
            logger.error("Failed to warm up leaderboard cache: {}", e.getMessage());
        }
    }
}
