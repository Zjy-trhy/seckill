package com.zjy.seckill.service.impl;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.zjy.seckill.service.CacheService;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.concurrent.TimeUnit;

@Service
public class CacheServiceImpl implements CacheService {

    private Cache<String, Object> commonCache;

    @PostConstruct
    public void init() {
        commonCache = CacheBuilder.newBuilder()
                //设置缓存容器的初始容量为10
                .initialCapacity(10)
                //设置缓存中最大可以存储100个key，超过100个后，按照LRU的策略，移除缓存项
                .maximumSize(100)
                //写缓存后，多少时间过期
                .expireAfterWrite(60, TimeUnit.SECONDS)
                .build();
    }

    @Override
    public void setCommonCache(String key, Object value) {
        commonCache.put(key, value);
    }

    @Override
    public Object getFromCommonCache(String key) {
        return commonCache.getIfPresent(key);
    }
}
