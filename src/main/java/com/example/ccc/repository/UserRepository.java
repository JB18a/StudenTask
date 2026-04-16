package com.example.ccc.repository;
import com.example.ccc.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

// JpaRepository<实体类, 主键类型>
public interface UserRepository extends JpaRepository<User, Long> {

    // JPA 只要按照命名规范写方法名，自动生成 SQL
    User findByUsername(String username);

    // 自带了 save(), findById(), deleteById(), findAll() 等方法
    /**
     * 【新增】查询指定任务下未提交作业的所有学生
     * 逻辑：用户角色是 USER，且该用户的 ID 不在 task_submission 表中（针对该 taskId）
     */
    @Query("SELECT u FROM User u WHERE u.role = 'USER' " +
            "AND u.id NOT IN " +
            "(SELECT s.userId FROM TaskSubmission s WHERE s.taskId = :taskId)")
    List<User> findUnsubmittedUsers(@Param("taskId") Long taskId);

    // 【新增】根据用户名模糊查询 (JPA 会自动生成 LIKE %...% 的 SQL)
    List<User> findByUsernameContaining(String username);

    List<User> findByRole(String role);

    List<User> findTop10ByRoleOrderByTotalScoreDesc(String role);
}
