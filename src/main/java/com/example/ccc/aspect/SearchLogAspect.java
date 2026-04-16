package com.example.ccc.aspect;
import com.example.ccc.dto.UserSearchDTO;
import com.example.ccc.utils.UserContext;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class SearchLogAspect {

    @AfterReturning(pointcut = "@annotation(logSearch)")
    public void recordSearchLog(JoinPoint joinPoint, com.example.ccc.annotation.LogSearch logSearch) {
        // 1. 获取当前操作员 ID
        Long adminId = UserContext.getUserId();

        // 2. 获取搜索参数
        Object[] args = joinPoint.getArgs();
        String keyword = "无关键词";

        for (Object arg : args) {
            if (arg instanceof UserSearchDTO) {
                UserSearchDTO dto = (UserSearchDTO) arg;
                if (dto.getUsername() != null && !dto.getUsername().isEmpty()) {
                    keyword = dto.getUsername();
                }
                break;
            }
        }

        // 3. 模拟记录日志 (实际开发中这里会存入 sys_log 表)
        System.out.println("【AOP审计日志】管理员(ID:" + adminId + ") " + logSearch.value() + "，关键词: [" + keyword + "]");
    }
}
