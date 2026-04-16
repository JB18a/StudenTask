package com.example.ccc.aspect;
import com.example.ccc.utils.UserContext;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.util.List;

@Aspect
@Component
public class ReminderLogAspect {

    private static final Logger logger = LoggerFactory.getLogger(ReminderLogAspect.class);

    @AfterReturning(pointcut = "execution(* com.example.ccc.controller.TaskController.getUrgentTasks(..))", returning = "result")
    public void logUserUrgentTaskQuery(JoinPoint joinPoint, Object result) {
        Long userId = UserContext.getUserId();
        logger.info("【AOP审计】用户 {} 主动查询了即将过期的任务列表", userId);
    }

    @Before("execution(* com.example.ccc.scheduler.TaskReminderScheduler.checkExpiringTasks())")
    public void logScheduledReminderStart(JoinPoint joinPoint) {
        logger.info("【AOP审计】定时任务提醒功能开始执行...");
    }

    @AfterReturning("execution(* com.example.ccc.scheduler.TaskReminderScheduler.checkExpiringTasks())")
    public void logScheduledReminderEnd(JoinPoint joinPoint) {
        logger.info("【AOP审计】定时任务提醒功能执行完毕");
    }
}
