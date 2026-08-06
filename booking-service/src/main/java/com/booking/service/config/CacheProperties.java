package com.booking.service.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@ConfigurationProperties(prefix = "cache.statistics")
@Getter
@Setter
public class CacheProperties {

    private Duration ttl = Duration.ofMinutes(10);

    private long maximumSize = 100;
}