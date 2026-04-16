package com.example.ccc.service;

import com.example.ccc.common.RedisKeys;
import com.example.ccc.dto.RegisterDTO;
import com.example.ccc.dto.TokenDTO;
import com.example.ccc.dto.UserSearchDTO;
import com.example.ccc.entity.RefreshToken;
import com.example.ccc.entity.User;
import com.example.ccc.repository.RefreshTokenRepository;
import com.example.ccc.repository.UserRepository;
import com.example.ccc.utils.JwtUtil;
import com.example.ccc.utils.RedisUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
public class UserService {

    private static final String NULL_PLACEHOLDER = "NULL";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private RedisUtil redisUtil;

    @Transactional(rollbackFor = Exception.class)
    public void register(RegisterDTO dto) {
        if (dto.getUsername() == null || dto.getUsername().trim().isEmpty()) {
            throw new RuntimeException("用户名不能为空");
        }
        if (dto.getPassword() == null || dto.getPassword().length() < 6) {
            throw new RuntimeException("密码长度不能少于6位");
        }
        if (userRepository.findByUsername(dto.getUsername()) != null) {
            throw new RuntimeException("用户名已存在");
        }

        User user = new User();
        BeanUtils.copyProperties(dto, user);
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setRole("USER");
        userRepository.save(user);
    }

    @Transactional(rollbackFor = Exception.class)
    public TokenDTO login(String username, String password) {
        if (username == null || username.trim().isEmpty()) {
            throw new RuntimeException("用户名不能为空");
        }
        if (password == null || password.isEmpty()) {
            throw new RuntimeException("密码不能为空");
        }

        String cacheKey = RedisKeys.USER_INFO + username;
        User user = null;

        Object cachedUser = redisUtil.get(cacheKey);
        if (cachedUser != null) {
            if (cachedUser instanceof User) {
                user = (User) cachedUser;
            } else if (NULL_PLACEHOLDER.equals(cachedUser)) {
                throw new RuntimeException("用户名或密码错误");
            }
        } else {
            user = userRepository.findByUsername(username);
            if (user != null) {
                redisUtil.set(cacheKey, user, RedisKeys.USER_INFO_EXPIRE, TimeUnit.SECONDS);
            } else {
                redisUtil.set(cacheKey, NULL_PLACEHOLDER, RedisKeys.NULL_CACHE_EXPIRE, TimeUnit.SECONDS);
            }
        }

        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("用户名或密码错误");
        }

        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getUsername());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getUsername());

        refreshTokenRepository.revokeAllByUserId(user.getId());
        RefreshToken refreshTokenEntity = new RefreshToken(
                user.getId(),
                refreshToken,
                LocalDateTime.now().plusSeconds(jwtUtil.getRefreshTokenExpire())
        );
        refreshTokenRepository.save(refreshTokenEntity);

        redisUtil.set(RedisKeys.USER_TOKEN + user.getId(), accessToken, 
                jwtUtil.getAccessTokenExpire(), TimeUnit.SECONDS);

        return new TokenDTO(accessToken, refreshToken, jwtUtil.getAccessTokenExpire());
    }

    @Transactional(rollbackFor = Exception.class)
    public TokenDTO refreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isEmpty()) {
            throw new RuntimeException("Refresh Token不能为空");
        }

        if (!jwtUtil.validateToken(refreshToken) || jwtUtil.isTokenExpired(refreshToken)) {
            throw new RuntimeException("Refresh Token无效或已过期");
        }

        String tokenType = jwtUtil.getTokenType(refreshToken);
        if (!"refresh".equals(tokenType)) {
            throw new RuntimeException("无效的Token类型");
        }

        Optional<RefreshToken> storedToken = refreshTokenRepository.findByToken(refreshToken);
        if (storedToken.isEmpty()) {
            throw new RuntimeException("Refresh Token不存在");
        }

        RefreshToken storedRefreshToken = storedToken.get();
        if (storedRefreshToken.getRevoked()) {
            throw new RuntimeException("Refresh Token已被撤销");
        }

        Long userId = jwtUtil.getUserIdFromToken(refreshToken);
        String username = jwtUtil.getUsernameFromToken(refreshToken);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        String newAccessToken = jwtUtil.generateAccessToken(userId, username);
        String newRefreshToken = jwtUtil.generateRefreshToken(userId, username);

        storedRefreshToken.setRevoked(true);
        refreshTokenRepository.save(storedRefreshToken);

        RefreshToken newRefreshTokenEntity = new RefreshToken(
                userId,
                newRefreshToken,
                LocalDateTime.now().plusSeconds(jwtUtil.getRefreshTokenExpire())
        );
        refreshTokenRepository.save(newRefreshTokenEntity);

        redisUtil.set(RedisKeys.USER_TOKEN + userId, newAccessToken, 
                jwtUtil.getAccessTokenExpire(), TimeUnit.SECONDS);

        return new TokenDTO(newAccessToken, newRefreshToken, jwtUtil.getAccessTokenExpire());
    }

    @Transactional(rollbackFor = Exception.class)
    public void logout(Long userId) {
        refreshTokenRepository.revokeAllByUserId(userId);
        redisUtil.delete(RedisKeys.USER_TOKEN + userId);
        redisUtil.delete(RedisKeys.USER_INFO + userId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void logoutByRefreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isEmpty()) {
            return;
        }

        Optional<RefreshToken> storedToken = refreshTokenRepository.findByToken(refreshToken);
        if (storedToken.isPresent()) {
            Long userId = storedToken.get().getUserId();
            refreshTokenRepository.revokeAllByUserId(userId);
            redisUtil.delete(RedisKeys.USER_TOKEN + userId);
            redisUtil.delete(RedisKeys.USER_INFO + userId);
        }
    }

    public User getUserById(Long userId) {
        if (userId == null || userId <= 0) {
            return null;
        }

        String cacheKey = RedisKeys.USER_INFO + userId;
        Object cached = redisUtil.get(cacheKey);
        if (cached != null) {
            if (cached instanceof User) {
                return (User) cached;
            }
            if (NULL_PLACEHOLDER.equals(cached)) {
                return null;
            }
        }

        User user = userRepository.findById(userId).orElse(null);
        if (user != null) {
            redisUtil.set(cacheKey, user, RedisKeys.USER_INFO_EXPIRE, TimeUnit.SECONDS);
        } else {
            redisUtil.set(cacheKey, NULL_PLACEHOLDER, RedisKeys.NULL_CACHE_EXPIRE, TimeUnit.SECONDS);
        }
        return user;
    }

    public List<User> searchUsers(UserSearchDTO dto) {
        if (dto.getUsername() == null || dto.getUsername().trim().isEmpty()) {
            return userRepository.findAll();
        }
        return userRepository.findByUsernameContaining(dto.getUsername());
    }
}
