package com.example.ccc.service;

import com.example.ccc.common.RedisKeys;
import com.example.ccc.dto.PageDTO;
import com.example.ccc.entity.Task;
import com.example.ccc.repository.TaskRepository;
import com.example.ccc.utils.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

@Service
public class TaskService {

    private static final String NULL_PLACEHOLDER = "NULL";

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private RedisUtil redisUtil;

    public Page<Task> getTaskList(int pageNum, int pageSize, Long creatorId, Integer status, Integer priority,
            Long currentUserId, boolean isStudent) {

        if (pageNum < 1) pageNum = 1;
        if (pageSize < 1 || pageSize > 100) pageSize = 10;

        String cacheKey = RedisKeys.TASK_LIST + pageNum + ":" + pageSize + ":" + creatorId + ":" + status + ":"
                + priority + ":" + currentUserId + ":" + isStudent;

        Object cached = redisUtil.get(cacheKey);
        if (cached != null) {
            if (cached instanceof PageDTO) {
                PageDTO<Task> dto = (PageDTO<Task>) cached;
                return new PageImpl<>(dto.getContent(), PageRequest.of(dto.getPageNumber(), dto.getPageSize()), dto.getTotalElements());
            }
        }

        int jpaPageNum = pageNum - 1;
        Pageable pageable = PageRequest.of(jpaPageNum, pageSize);

        Long excludeUserId = isStudent ? currentUserId : null;

        Page<Task> taskPage = taskRepository.findByCondition(creatorId, status, priority, excludeUserId, pageable);

        if (taskPage != null && taskPage.hasContent()) {
            redisUtil.set(cacheKey, PageDTO.from(taskPage), RedisKeys.TASK_LIST_EXPIRE, TimeUnit.SECONDS);
        } else {
            redisUtil.set(cacheKey, NULL_PLACEHOLDER, RedisKeys.NULL_CACHE_EXPIRE, TimeUnit.SECONDS);
        }

        return taskPage != null ? taskPage : new PageImpl<>(Collections.emptyList(), pageable, 0);
    }

    @Transactional(rollbackFor = Exception.class)
    public void createTask(Task task, Object filePlaceholder) {
        taskRepository.save(task);
        clearTaskCache();
    }

    public Task getTaskById(Long id) {
        if (id == null || id <= 0) {
            return null;
        }

        String cacheKey = RedisKeys.TASK_INFO + id;

        Object cached = redisUtil.get(cacheKey);
        if (cached != null) {
            if (cached instanceof Task) {
                return (Task) cached;
            }
            if (NULL_PLACEHOLDER.equals(cached)) {
                return null;
            }
        }

        Task task = taskRepository.findById(id).orElse(null);

        if (task != null) {
            redisUtil.set(cacheKey, task, RedisKeys.TASK_INFO_EXPIRE, TimeUnit.SECONDS);
        } else {
            redisUtil.set(cacheKey, NULL_PLACEHOLDER, RedisKeys.NULL_CACHE_EXPIRE, TimeUnit.SECONDS);
        }

        return task;
    }

    public Task getTask(Long id, Long userId) {
        Task task = getTaskById(id);
        if (task != null && task.getUserId().equals(userId)) {
            return task;
        }
        return null;
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateTask(Task task) {
        taskRepository.save(task);
        String cacheKey = RedisKeys.TASK_INFO + task.getId();
        redisUtil.delete(cacheKey);
        clearTaskCache();
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteTask(Long id, Long userId) {
        Task task = getTask(id, userId);
        if (task != null) {
            taskRepository.delete(task);
            String cacheKey = RedisKeys.TASK_INFO + id;
            redisUtil.delete(cacheKey);
            clearTaskCache();
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateTaskStatus(Long id, Integer status, Long userId) {
        Task task = getTask(id, userId);
        if (task != null) {
            task.setStatus(status);
            taskRepository.save(task);
            String cacheKey = RedisKeys.TASK_INFO + id;
            redisUtil.delete(cacheKey);
            clearTaskCache();
        }
    }

    private void clearTaskCache() {
        redisUtil.deleteByPattern(RedisKeys.TASK_LIST + "*");
    }
}
