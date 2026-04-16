package com.example.ccc.controller;

import com.alibaba.excel.EasyExcel;
import com.example.ccc.common.Result;
import com.example.ccc.dto.RegisterDTO;
import com.example.ccc.dto.UserSearchDTO;
import com.example.ccc.entity.User;
import com.example.ccc.entity.UserHistoryVO;
import com.example.ccc.entity.UserScoreExcel;
import com.example.ccc.repository.TaskSubmissionRepository;
import com.example.ccc.repository.UserRepository;
import com.example.ccc.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import com.example.ccc.annotation.LogSearch;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('PUBLISHER')")
public class AdminUserController {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TaskSubmissionRepository submissionRepository;

    @GetMapping
    public Result<List<User>> listUsers() {
        return Result.success(userRepository.findAll());
    }

    @PostMapping
    public Result addUser(@RequestBody @Validated RegisterDTO dto) {
        userService.register(dto);
        return Result.success("用户添加成功");
    }

    @DeleteMapping("/{id}")
    public Result deleteUser(@PathVariable Long id) {
        userRepository.deleteById(id);
        return Result.success("用户删除成功");
    }

    @PutMapping("/{id}")
    public Result updateUser(@PathVariable Long id, @RequestBody User userParams) {
        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        if (userParams.getNickname() != null) existingUser.setNickname(userParams.getNickname());
        if (userParams.getEmail() != null) existingUser.setEmail(userParams.getEmail());
        if (userParams.getRole() != null) existingUser.setRole(userParams.getRole());

        userRepository.save(existingUser);
        return Result.success("用户信息修改成功");
    }

    @GetMapping("/{userId}/report")
    public Result<Map<String, Object>> getUserReport(@PathVariable Long userId) {
        User targetUser = userRepository.findById(userId).orElse(null);
        if (targetUser == null) return Result.error(404, "目标用户不存在");

        List<UserHistoryVO> history = submissionRepository.findHistoryByUserId(userId);

        double avgScore = history.stream()
                .filter(h -> h.getScore() != null)
                .mapToInt(UserHistoryVO::getScore)
                .average()
                .orElse(0.0);

        Map<String, Object> report = new HashMap<>();
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", targetUser.getId());
        userInfo.put("nickname", targetUser.getNickname());
        userInfo.put("email", targetUser.getEmail());
        userInfo.put("role", targetUser.getRole());

        report.put("userInfo", userInfo);
        report.put("submissions", history);
        report.put("averageScore", String.format("%.1f", avgScore));

        return Result.success(report);
    }

    @GetMapping("/{userId}/report/export")
    public void exportUserReport(@PathVariable Long userId, HttpServletResponse response) throws Exception {
        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        List<UserHistoryVO> history = submissionRepository.findHistoryByUserId(userId);

        List<UserScoreExcel> excelData = new ArrayList<>();
        for (UserHistoryVO vo : history) {
            UserScoreExcel data = new UserScoreExcel();
            data.setTaskTitle(vo.getTaskTitle());
            data.setFileName(vo.getFileName());
            data.setScore(vo.getScore());
            data.setFeedback(vo.getFeedback());
            if (vo.getSubmitTime() != null) {
                data.setSubmitTime(java.sql.Timestamp.valueOf(vo.getSubmitTime()));
            }
            excelData.add(data);
        }

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode(targetUser.getNickname() + "_成绩单", StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + fileName + ".xlsx");

        EasyExcel.write(response.getOutputStream(), UserScoreExcel.class)
                .sheet("作业详情")
                .doWrite(excelData);
    }

    @PostMapping("/search")
    @LogSearch("搜索用户")
    public Result<List<User>> searchUsers(@RequestBody UserSearchDTO searchDTO) {
        List<User> result = userService.searchUsers(searchDTO);
        return Result.success(result);
    }
}
