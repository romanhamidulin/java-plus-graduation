package ru.practicum.feign.client;

import org.springframework.cloud.openfeign.FeignClient;
import ru.practicum.feign.api.EventInternalApi;
import ru.practicum.feign.config.FeignConfig;
import ru.practicum.feign.fallback.EventFallbackClient;

@FeignClient(name = "event-service", path = "/internal/events", fallback = EventFallbackClient.class,
        configuration = FeignConfig.class)
public interface EventFeignClient extends EventInternalApi {
}
