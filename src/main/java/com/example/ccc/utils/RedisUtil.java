package com.example.ccc.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
public class RedisUtil {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    public void set(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }

    public void set(String key, Object value, long timeout, TimeUnit unit) {
        redisTemplate.opsForValue().set(key, value, timeout, unit);
    }

    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    public Boolean delete(String key) {
        return redisTemplate.delete(key);
    }

    public Boolean hasKey(String key) {
        return redisTemplate.hasKey(key);
    }

    public Long deleteByPattern(String pattern) {
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            return redisTemplate.delete(keys);
        }
        return 0L;
    }

    public Boolean expire(String key, long timeout, TimeUnit unit) {
        return redisTemplate.expire(key, timeout, unit);
    }

    public Long getExpire(String key) {
        return redisTemplate.getExpire(key);
    }

    public void hashPut(String key, String hashKey, Object value) {
        redisTemplate.opsForHash().put(key, hashKey, value);
    }

    public Object hashGet(String key, String hashKey) {
        return redisTemplate.opsForHash().get(key, hashKey);
    }

    public Boolean hashHasKey(String key, String hashKey) {
        return redisTemplate.opsForHash().hasKey(key, hashKey);
    }

    public Long hashDelete(String key, Object... hashKeys) {
        return redisTemplate.opsForHash().delete(key, hashKeys);
    }

    public void increment(String key) {
        stringRedisTemplate.opsForValue().increment(key);
    }

    public Long increment(String key, long delta) {
        return stringRedisTemplate.opsForValue().increment(key, delta);
    }

    public Boolean zAdd(String key, String member, double score) {
        return stringRedisTemplate.opsForZSet().add(key, member, score);
    }

    public Double zIncrementScore(String key, String member, double delta) {
        return stringRedisTemplate.opsForZSet().incrementScore(key, member, delta);
    }

    public Double zScore(String key, String member) {
        return stringRedisTemplate.opsForZSet().score(key, member);
    }

    public Set<String> zReverseRange(String key, long start, long end) {
        return stringRedisTemplate.opsForZSet().reverseRange(key, start, end);
    }

    public Set<ZSetOperations.TypedTuple<String>> zReverseRangeWithScores(String key, long start, long end) {
        return stringRedisTemplate.opsForZSet().reverseRangeWithScores(key, start, end);
    }

    public Long zRank(String key, String member) {
        return stringRedisTemplate.opsForZSet().reverseRank(key, member);
    }

    public Long zCard(String key) {
        return stringRedisTemplate.opsForZSet().zCard(key);
    }

    public Long zRemove(String key, String... members) {
        return stringRedisTemplate.opsForZSet().remove(key, members);
    }

    public Boolean tryLock(String lockKey, long expireSeconds) {
        Boolean success = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, "1", expireSeconds, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(success);
    }

    public void unlock(String lockKey) {
        stringRedisTemplate.delete(lockKey);
    }

    public Boolean setBit(String key, long offset, boolean value) {
        return stringRedisTemplate.opsForValue().setBit(key, offset, value);
    }

    public Boolean getBit(String key, long offset) {
        return stringRedisTemplate.opsForValue().getBit(key, offset);
    }

    public Long bitCount(String key) {
        return stringRedisTemplate.execute(
                (RedisCallback<Long>) connection ->
                        connection.stringCommands().bitCount(key.getBytes())
        );
    }

    public Long bitCount(String key, long start, long end) {
        return stringRedisTemplate.execute(
                (RedisCallback<Long>) connection ->
                        connection.stringCommands().bitCount(key.getBytes(), start, end)
        );
    }

    public Long sAdd(String key, String... values) {
        return stringRedisTemplate.opsForSet().add(key, values);
    }

    public Long sRemove(String key, Object... values) {
        return stringRedisTemplate.opsForSet().remove(key, values);
    }

    public Boolean sIsMember(String key, Object value) {
        return stringRedisTemplate.opsForSet().isMember(key, value);
    }

    public Set<String> sMembers(String key) {
        return stringRedisTemplate.opsForSet().members(key);
    }

    public Long sCard(String key) {
        return stringRedisTemplate.opsForSet().size(key);
    }
}
