package ru.practicum.feign.client;

import org.springframework.cloud.openfeign.FeignClient;
import ru.practicum.feign.api.UserInternalApi;
import ru.practicum.feign.config.FeignConfig;
import ru.practicum.feign.fallback.UserFallbackClient;

@FeignClient(name = "user-service", path = "/internal/users", fallback = UserFallbackClient.class,
        configuration = FeignConfig.class)
public interface UserFeignClient extends UserInternalApi {

}
