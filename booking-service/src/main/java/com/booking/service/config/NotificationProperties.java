package com.booking.service.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "notification")
@Getter
@Setter
public class NotificationProperties {

    private String url;

    private Retry retry = new Retry();

    @Getter
    @Setter
    public static class Retry {

        private int maxAttempts;

        private long delay;
    }
}