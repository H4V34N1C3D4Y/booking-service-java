package com.booking.service.service;

import com.booking.service.notification.contracts.StatisticsCacheEvictEvent;
import com.booking.service.service.cache.StatisticsCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class StatisticsCacheListener {

    private final StatisticsCacheService statisticsCacheService;

    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT
    )
    public void evictCache(StatisticsCacheEvictEvent event) {
        statisticsCacheService.evictStatisticsCache();
    }
}