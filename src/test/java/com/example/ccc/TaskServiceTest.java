package com.example.ccc;
import com.example.ccc.entity.Task;
import com.example.ccc.service.TaskService;
import com.example.ccc.utils.UserContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page; // 1. 引入 JPA 分页类
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@SpringBootTest
public class TaskServiceTest {

    @Autowired
    private TaskService taskService;

    // 假设我们要测试的用户 ID (请确保数据库里有这个 ID，或者像 AdminFlowTest 那样动态创建)
    private final Long TEST_USER_ID = 2L;

    @BeforeEach
    void setUp() {
        // 模拟登录
        UserContext.setUserId(TEST_USER_ID);
    }

    @AfterEach
    void tearDown() {
        UserContext.remove();
    }

    /**
     * 测试：创建任务
     * 加 @Transactional 保证测试数据会自动回滚，不污染数据库
     */
    @Test
    @Transactional
    void testCreateTask() {
        System.out.println(">>> 测试创建任务...");

        Task task = new Task();
        task.setUserId(TEST_USER_ID); // 手动设置 ID
        task.setTitle("JPA单元测试任务");
        task.setDescription("测试 JPA Repository 保存功能");
        task.setPriority(1);
        task.setDueDate(LocalDateTime.now().plusDays(3));

        // 现在的 TaskService.createTask 主要是保存数据，不再处理文件上传(逻辑移到了SubmissionService)
        // 所以第二个参数传 null
        taskService.createTask(task, null);

        System.out.println("任务创建成功！ID: " + task.getId());
        Assertions.assertNotNull(task.getId(), "任务ID不应为空");
    }

    /**
     * 测试：查询任务列表 (分页变化的核心测试)
     */
    @Test
    @Transactional
    void testGetTaskList() {
        System.out.println(">>> 测试查询列表 (JPA Page版)...");

        // 1. 先插入一条数据，确保查的时候有东西
        Task task = new Task();
        task.setUserId(TEST_USER_ID);
        task.setTitle("用于列表测试的任务");
        taskService.createTask(task, null);

        // 2. 调用分页查询
        // 参数：页码(1), 大小(5), 用户ID, 状态(null), 优先级(null)
        Page<Task> page = taskService.getTaskList(1, 5, TEST_USER_ID, null, null,1L,false);

        // 3. 验证 JPA Page 对象
        // 【核心修改点】：
        // PageInfo.getTotal() -> Page.getTotalElements()
        // PageInfo.getList()  -> Page.getContent()

        long total = page.getTotalElements();
        List<Task> list = page.getContent();

        System.out.println("查询结果总数: " + total);
        System.out.println("当前页条数: " + list.size());

        Assertions.assertTrue(total > 0, "总数应该大于0");
        Assertions.assertFalse(list.isEmpty(), "列表不应为空");

        // 打印第一条数据验证
        System.out.println("第一条任务标题: " + list.get(0).getTitle());
    }

    /**
     * 测试：修改任务状态
     */
    @Test
    @Transactional
    void testUpdateStatus() {
        // 1. 准备数据
        Task task = new Task();
        task.setUserId(TEST_USER_ID);
        task.setTitle("待修改状态的任务");
        task.setStatus(0); // 初始状态
        taskService.createTask(task, null);
        Long taskId = task.getId();

        System.out.println(">>> 测试修改任务 ID=" + taskId + " 的状态...");

        // 2. 修改状态为 1 (已完成)
        taskService.updateTaskStatus(taskId, 1, TEST_USER_ID);

        // 3. 重新查询并验证
        Task updatedTask = taskService.getTask(taskId, TEST_USER_ID);
        Assertions.assertNotNull(updatedTask);
        Assertions.assertEquals(1, updatedTask.getStatus(), "状态应该被修改为 1");

        System.out.println("状态修改验证成功！");
    }
}