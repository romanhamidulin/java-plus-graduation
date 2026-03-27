package ru.practicum.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import ru.practicum.config.FeignRetryConfig;
import ru.practicum.dto.event.EventDto;

@FeignClient(
        name = "event-service",
        path = "/internal/events",
        configuration = FeignRetryConfig.class,
        fallback = EventClientFallback.class
)
public interface EventClient {

    @GetMapping("/{eventId}")
    EventDto getEvent(@PathVariable Long eventId);
}
