package com.example.ccc.component;
import com.example.ccc.entity.UserHistoryVO;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// 这是一个纯 POJO，通过配置类注册为 Bean
public class ScoreCalculator {

    public Map<String, Double> analyze(List<UserHistoryVO> history) {
        Map<String, Double> radar = new HashMap<>();

        // 模拟三个维度的计算
        // 1. 勤奋度 (提交次数越多分越高)
        double diligence = Math.min(history.size() * 20.0, 100.0);

        // 2. 质量 (平均分)
        double quality = history.stream()
                .filter(h -> h.getScore() != null)
                .mapToInt(UserHistoryVO::getScore)
                .average().orElse(60.0);

        // 3. 速度 (这里简单模拟：假设分高就代表快，实际可根据 submitTime 和 dueDate 计算)
        double speed = quality * 0.9;

        radar.put("勤奋度", diligence);
        radar.put("作业质量", quality);
        radar.put("提交速度", speed);

        return radar;
    }
}
