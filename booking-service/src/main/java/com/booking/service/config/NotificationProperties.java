package com.booking.service.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@ConfigurationProperties(prefix = "notification")
@Getter
@Setter
public class NotificationProperties {

    private String url;

    private Retry retry = new Retry();
    private Duration connectTimeout = Duration.ofSeconds(3);
    private Duration readTimeout = Duration.ofSeconds(3);

    @Getter
    @Setter
    public static class Retry {

        private int maxAttempts = 3;

        private long delay = 1000;

        private double multiplier = 2.0;
    }
}