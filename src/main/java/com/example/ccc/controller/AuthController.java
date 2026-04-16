package com.example.ccc.controller;

import com.example.ccc.common.Result;
import com.example.ccc.dto.RegisterDTO;
import com.example.ccc.dto.TokenDTO;
import com.example.ccc.entity.User;
import com.example.ccc.service.UserService;
import com.example.ccc.utils.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public Result register(@Validated @RequestBody RegisterDTO dto) {
        userService.register(dto);
        return Result.success("注册成功");
    }

    @PostMapping("/login")
    public Result<TokenDTO> login(@RequestBody User user) {
        TokenDTO tokenDTO = userService.login(user.getUsername(), user.getPassword());
        return Result.success(tokenDTO);
    }

    @PostMapping("/refresh")
    public Result<TokenDTO> refreshToken(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");
        TokenDTO tokenDTO = userService.refreshToken(refreshToken);
        return Result.success(tokenDTO);
    }

    @PostMapping("/logout")
    public Result logout(@RequestBody(required = false) Map<String, String> request) {
        Long userId = UserContext.getUserId();
        if (userId != null) {
            userService.logout(userId);
        } else if (request != null && request.get("refreshToken") != null) {
            userService.logoutByRefreshToken(request.get("refreshToken"));
        }
        return Result.success("登出成功");
    }
}
