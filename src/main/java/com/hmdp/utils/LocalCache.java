package com.hmdp.utils;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class LocalCache {

    private Cache<String, Object> cache;

    @PostConstruct
    public void init() {
        cache = Caffeine.newBuilder()
                .maximumSize(10000)                    // 最多缓存1万个对象
                .expireAfterWrite(5, TimeUnit.MINUTES) // 写入后5分钟过期
                .expireAfterAccess(3, TimeUnit.MINUTES)// 3分钟没访问就过期
                .recordStats()                         // 开启统计
                .removalListener((key, value, cause) -> {
                    log.debug("缓存被移除: key={}, 原因={}", key, cause);
                })
                .build();
    }

    /**
     * 存入缓存
     */
    public void put(String key, Object value) {
        cache.put(key, value);
    }

    /**
     * 获取缓存
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) cache.getIfPresent(key);
    }

    /**
     * 获取缓存，如果不存在则加载
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key, java.util.function.Function<String, T> loader) {
        return (T) cache.get(key, loader);
    }

    /**
     * 删除缓存
     */
    public void remove(String key) {
        cache.invalidate(key);
    }

    /**
     * 清空所有缓存
     */
    public void clear() {
        cache.invalidateAll();
    }

    /**
     * 判断是否存在
     */
    public boolean exists(String key) {
        return cache.getIfPresent(key) != null;
    }

    /**
     * 获取缓存大小
     */
    public long size() {
        return cache.estimatedSize();
    }

    /**
     * 获取统计信息
     */
    public String getStats() {
        return String.format(
            "命中率: %.2f%%, 命中: %d, 未命中: %d",
            cache.stats().hitRate() * 100,
            cache.stats().hitCount(),
            cache.stats().missCount()
        );
    }
}