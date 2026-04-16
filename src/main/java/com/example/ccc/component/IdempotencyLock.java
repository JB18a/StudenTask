package com.example.ccc.component;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class IdempotencyLock {

    private static final Logger logger = LoggerFactory.getLogger(IdempotencyLock.class);

    private final Cache<String, Long> lockCache = Caffeine.newBuilder()
            .maximumSize(10000)
            .expireAfterWrite(5, TimeUnit.SECONDS)
            .build();

    private final Cache<String, Long> resultCache = Caffeine.newBuilder()
            .maximumSize(10000)
            .expireAfterWrite(60, TimeUnit.SECONDS)
            .build();

    public boolean tryLock(String key, long waitMillis) {
        Long existing = lockCache.getIfPresent(key);
        if (existing != null) {
            logger.debug("Idempotency lock exists for key: {}", key);
            return false;
        }

        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < waitMillis) {
            Long value = lockCache.asMap().putIfAbsent(key, 1L);
            if (value == null) {
                logger.debug("Idempotency lock acquired for key: {}", key);
                return true;
            }
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }

        logger.debug("Idempotency lock timeout for key: {}", key);
        return false;
    }

    public void unlock(String key) {
        lockCache.invalidate(key);
        logger.debug("Idempotency lock released for key: {}", key);
    }

    public <T> T executeWithLock(String key, long waitMillis, java.util.function.Supplier<T> action) {
        if (!tryLock(key, waitMillis)) {
            throw new RuntimeException("请求过于频繁，请稍后重试");
        }

        try {
            return action.get();
        } finally {
            unlock(key);
        }
    }

    public <T> T getResult(String key) {
        return (T) resultCache.getIfPresent(key);
    }

    public void putResult(String key, Object result) {
        resultCache.put(key, (Long) result);
    }

    public boolean hasResult(String key) {
        return resultCache.getIfPresent(key) != null;
    }
}
