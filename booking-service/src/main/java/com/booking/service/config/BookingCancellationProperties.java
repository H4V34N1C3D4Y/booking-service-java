package com.booking.service.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import java.time.Duration;

@Getter
@Setter
@ConfigurationProperties(prefix = "booking.cancellation")
public class BookingCancellationProperties {
    private Duration timeout = Duration.ofMinutes(5);
}
