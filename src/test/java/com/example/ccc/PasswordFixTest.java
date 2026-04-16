package com.example.ccc;
import com.example.ccc.entity.User;
import com.example.ccc.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
public class PasswordFixTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * 修复指定用户的密码
     */
    @Test
    @Transactional
    @Rollback(false) // 关键：关闭回滚，让修改真正写入数据库
    void fixOneUserPassword() {
        String targetUsername = "jj"; // 你要修复的用户名
        String newPassword = "123456";        // 你想要的正确密码

        User user = userRepository.findByUsername(targetUsername);
        if (user != null) {
            // 1. 加密
            String encodedPassword = passwordEncoder.encode(newPassword);
            // 2. 设置回对象
            user.setPassword(encodedPassword);
            // 3. 保存 (JPA会自动更新)
            userRepository.save(user);

            System.out.println("✅ 用户 [" + targetUsername + "] 密码已修复为: " + newPassword);
            System.out.println("加密后: " + encodedPassword);
        } else {
            System.err.println("❌ 找不到该用户");
        }
    }

    /**
     * 暴力修复：把数据库里所有用户的密码都重置为 123456
     * (慎用！仅限开发阶段)
     */
    @Test
    @Transactional
    @Rollback(false)
    void fixAllUsers() {
        String commonPassword = passwordEncoder.encode("123456");

        var allUsers = userRepository.findAll();
        for (User user : allUsers) {
            user.setPassword(commonPassword);
            userRepository.save(user);
        }
        System.out.println("✅ 所有用户的密码已重置为 123456,是暴力修复");
    }
}
