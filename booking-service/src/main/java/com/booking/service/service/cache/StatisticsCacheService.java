package com.booking.service.service.cache;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

@Service
public class StatisticsCacheService {
    @CacheEvict(value = CacheNames.STATISTICS, allEntries = true)
    public void evictStatisticsCache() {
    }
}