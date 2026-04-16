package com.example.ccc.service;

import com.example.ccc.common.RedisKeys;
import com.example.ccc.config.RabbitMQConfig;
import com.example.ccc.entity.GrabReleaseMessage;
import com.example.ccc.entity.GrabResultVO;
import com.example.ccc.entity.GrabTaskMessage;
import com.example.ccc.entity.Task;
import com.example.ccc.repository.TaskRepository;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class GrabTaskService {

    private static final Logger logger = LoggerFactory.getLogger(GrabTaskService.class);

    private static final long RELEASE_DELAY_MINUTES = 30;

    private static final String GRAB_LUA_SCRIPT = """
            local slotsKey = KEYS[1]
            local recordKey = KEYS[2]
            local userId = ARGV[1]
            local totalSlots = tonumber(ARGV[2])
            
            if redis.call('SISMEMBER', recordKey, userId) == 1 then
                return {-1, 0}
            end
            
            local currentSlots = redis.call('GET', slotsKey)
            if currentSlots == false then
                redis.call('SET', slotsKey, totalSlots)
                currentSlots = totalSlots
            else
                currentSlots = tonumber(currentSlots)
            end
            
            if currentSlots <= 0 then
                return {0, 0}
            end
            
            local newSlots = redis.call('DECR', slotsKey)
            if newSlots < 0 then
                redis.call('INCR', slotsKey)
                return {0, 0}
            end
            
            redis.call('SADD', recordKey, userId)
            
            return {1, newSlots}
            """;

    private static final String RELEASE_LUA_SCRIPT = """
            local slotsKey = KEYS[1]
            local recordKey = KEYS[2]
            local userId = ARGV[1]
            
            if redis.call('SISMEMBER', recordKey, userId) == 0 then
                return {0, 0}
            end
            
            redis.call('SREM', recordKey, userId)
            local newSlots = redis.call('INCR', slotsKey)
            
            return {1, newSlots}
            """;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    public GrabResultVO grabTask(Long taskId, Long userId) {
        Task task = taskRepository.findById(taskId).orElse(null);
        if (task == null) {
            return GrabResultVO.fail("任务不存在");
        }

        if (!Boolean.TRUE.equals(task.getIsLimited())) {
            return GrabResultVO.fail("该任务不是限量任务");
        }

        String slotsKey = RedisKeys.TASK_SLOTS + taskId;
        String recordKey = RedisKeys.TASK_GRAB_RECORD + taskId;

        RScript script = redissonClient.getScript();

        List<Object> keys = Arrays.asList(slotsKey, recordKey);
        Object result = script.eval(
                RScript.Mode.READ_WRITE,
                GRAB_LUA_SCRIPT,
                RScript.ReturnType.MULTI,
                keys,
                userId.toString(),
                task.getAvailableSlots().toString()
        );

        List<Long> resultList = (List<Long>) result;
        long code = resultList.get(0);
        long remainingSlots = resultList.get(1);

        if (code == -1) {
            logger.debug("User {} already grabbed task {}", userId, taskId);
            return GrabResultVO.fail("您已经抢过该任务了");
        }

        if (code == 0) {
            logger.debug("Task {} slots exhausted, user {} failed", taskId, userId);
            return GrabResultVO.fail("名额已被抢光");
        }

        sendGrabMessage(taskId, userId, (int) remainingSlots);

        scheduleRelease(taskId, userId);

        logger.debug("User {} grabbed task {} successfully, remaining: {}", userId, taskId, remainingSlots);

        return GrabResultVO.success(taskId, (int) remainingSlots, task.getRewardPoints());
    }

    private void sendGrabMessage(Long taskId, Long userId, Integer remainingSlots) {
        GrabTaskMessage message = new GrabTaskMessage(taskId, userId, remainingSlots);
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.GRAB_TASK_EXCHANGE,
                RabbitMQConfig.GRAB_TASK_ROUTING_KEY,
                message
        );
        logger.debug("Sent grab task message to MQ: taskId={}, userId={}", taskId, userId);
    }

    private void scheduleRelease(Long taskId, Long userId) {
        GrabReleaseMessage message = new GrabReleaseMessage(taskId, userId);
        
        long delayMillis = TimeUnit.MINUTES.toMillis(RELEASE_DELAY_MINUTES);
        
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.GRAB_RELEASE_DELAY_QUEUE,
                message,
                msg -> {
                    msg.getMessageProperties().setDelay((int) delayMillis);
                    return msg;
                }
        );
        
        logger.debug("Scheduled release for task {} user {} in {} minutes", taskId, userId, RELEASE_DELAY_MINUTES);
    }

    public void releaseSlot(Long taskId, Long userId) {
        Task task = taskRepository.findById(taskId).orElse(null);
        if (task == null || !Boolean.TRUE.equals(task.getIsLimited())) {
            logger.warn("Task {} not found or not limited", taskId);
            return;
        }

        String slotsKey = RedisKeys.TASK_SLOTS + taskId;
        String recordKey = RedisKeys.TASK_GRAB_RECORD + taskId;

        RScript script = redissonClient.getScript();

        List<Object> keys = Arrays.asList(slotsKey, recordKey);
        Object result = script.eval(
                RScript.Mode.READ_WRITE,
                RELEASE_LUA_SCRIPT,
                RScript.ReturnType.MULTI,
                keys,
                userId.toString()
        );

        List<Long> resultList = (List<Long>) result;
        long code = resultList.get(0);
        long newSlots = resultList.get(1);

        if (code == 1) {
            asyncUpdateDatabase(taskId, (int) newSlots);
            logger.info("Released slot for task {} user {}, new available: {}", taskId, userId, newSlots);
        } else {
            logger.debug("User {} already released or not grabbed task {}", userId, taskId);
        }
    }

    private void asyncUpdateDatabase(Long taskId, int remainingSlots) {
        try {
            Task task = taskRepository.findById(taskId).orElse(null);
            if (task != null) {
                task.setAvailableSlots(remainingSlots);
                taskRepository.save(task);
                logger.debug("Updated task {} available slots to {}", taskId, remainingSlots);
            }
        } catch (Exception e) {
            logger.error("Failed to update database for task {}: {}", taskId, e.getMessage());
        }
    }

    public void initSlots(Long taskId, Integer availableSlots) {
        String slotsKey = RedisKeys.TASK_SLOTS + taskId;
        RAtomicLong atomicSlots = redissonClient.getAtomicLong(slotsKey);
        atomicSlots.set(availableSlots);
        logger.debug("Initialized slots for task {}: {}", taskId, availableSlots);
    }

    public Integer getAvailableSlots(Long taskId) {
        String slotsKey = RedisKeys.TASK_SLOTS + taskId;
        RAtomicLong availableSlots = redissonClient.getAtomicLong(slotsKey);

        if (!availableSlots.isExists()) {
            Task task = taskRepository.findById(taskId).orElse(null);
            if (task != null && Boolean.TRUE.equals(task.getIsLimited())) {
                initSlots(taskId, task.getAvailableSlots());
                return task.getAvailableSlots();
            }
            return null;
        }

        return (int) availableSlots.get();
    }

    public void resetTaskSlots(Long taskId) {
        Task task = taskRepository.findById(taskId).orElse(null);
        if (task == null || !Boolean.TRUE.equals(task.getIsLimited())) {
            return;
        }

        String slotsKey = RedisKeys.TASK_SLOTS + taskId;
        String recordKey = RedisKeys.TASK_GRAB_RECORD + taskId;

        redissonClient.getAtomicLong(slotsKey).set(task.getTotalSlots());
        redissonClient.getSet(recordKey).delete();

        logger.debug("Reset slots for task {} to {}", taskId, task.getTotalSlots());
    }
}
