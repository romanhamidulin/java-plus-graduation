package ru.practicum.feign.client;

import org.springframework.cloud.openfeign.FeignClient;
import ru.practicum.feign.api.CommentInternalApi;
import ru.practicum.feign.config.FeignConfig;
import ru.practicum.feign.fallback.CommentFallbackClient;

@FeignClient(name = "comment-service", path = "/internal/comments", fallback = CommentFallbackClient.class,
        configuration = FeignConfig.class)
public interface CommentFeignClient extends CommentInternalApi {
}
