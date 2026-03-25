package ru.practicum.feign.client;

import org.springframework.cloud.openfeign.FeignClient;
import ru.practicum.feign.api.RequestInternalApi;
import ru.practicum.feign.config.FeignConfig;
import ru.practicum.feign.fallback.RequestFallbackClient;

@FeignClient(value = "request-service", path = "/internal/requests", fallback = RequestFallbackClient.class,
configuration = FeignConfig .class)
public interface RequestFeignClient extends RequestInternalApi {
}
