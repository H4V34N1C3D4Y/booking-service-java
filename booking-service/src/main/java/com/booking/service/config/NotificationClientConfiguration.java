package com.booking.service.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@RequiredArgsConstructor
public class NotificationClientConfiguration {

    private final NotificationProperties properties;

    @Bean
    RestClient notificationRestClient(RestClient.Builder builder) {
        return builder
                .baseUrl(properties.getUrl())
                .build();
    }
}
