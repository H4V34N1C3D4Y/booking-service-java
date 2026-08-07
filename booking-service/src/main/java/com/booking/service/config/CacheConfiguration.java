package com.booking.service.config;

import com.booking.service.service.cache.CacheNames;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(CacheProperties.class)
public class CacheConfiguration {

    private final CacheProperties properties;

    @Bean
    public CacheManager cacheManager() {

        CaffeineCacheManager manager =
                new CaffeineCacheManager(CacheNames.STATISTICS);

        manager.setCaffeine(
                Caffeine.newBuilder()
                        .expireAfterWrite(properties.getTtl())
                        .maximumSize(properties.getMaximumSize())
        );

        return manager;
    }
}