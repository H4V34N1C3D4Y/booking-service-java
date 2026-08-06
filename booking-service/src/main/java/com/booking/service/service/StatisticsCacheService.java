package com.booking.service.service;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

@Service
public class StatisticsCacheService {
    @CacheEvict(value = "statistics", allEntries = true)
    public void evictStatisticsCache() {
    }
}
