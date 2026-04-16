package com.example.ccc.aspect;
import com.alibaba.fastjson2.JSON;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import java.util.Map;

@Aspect
@Component
public class GradingAspect {

    @Around("@annotation(com.example.ccc.annotation.AutoAvgScore)")
    public Object calculateScore(ProceedingJoinPoint point) throws Throwable {
        Object[] args = point.getArgs();

        // 拦截 Controller 的第二个参数 (RequestBody Map)
        if (args.length > 1 && args[1] instanceof Map) {
            Map<String, Object> body = (Map<String, Object>) args[1];

            // 如果前端传了 details: { "质量": 90, "速度": 80 }
            if (body.containsKey("details")) {
                Map<String, Integer> details = (Map<String, Integer>) body.get("details");

                if (details != null && !details.isEmpty()) {
                    // 1. 自动计算平均分
                    double avg = details.values().stream()
                            .mapToInt(Integer::intValue)
                            .average()
                            .orElse(0);

                    // 2. 篡改参数：写入总分 score
                    body.put("score", (int) Math.round(avg));

                    // 3. 篡改参数：写入详情字符串 detailScores
                    body.put("detailScores", JSON.toJSONString(details));

                    System.out.println("【AOP】自动计算平均分完成: " + avg);
                }
            }
        }
        return point.proceed(args);
    }
}
