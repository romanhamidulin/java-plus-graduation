package ru.practicum.feign.fallback;

import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.dto.user.UserDto;
import ru.practicum.dto.user.UserShortDto;
import ru.practicum.exception.ServiceUnavailableException;
import ru.practicum.feign.api.UserInternalApi;

import java.util.List;
import java.util.Optional;

@Component
public class UserFallbackClient implements UserInternalApi {

    private static final String SERVICE_NAME = "user-service";

    @Override
    public Optional<UserDto> getUserById(Long userId) {
        throw new ServiceUnavailableException(SERVICE_NAME);
    }

    @Override
    public UserShortDto getUserByIdShort(Long userId) {
        throw new ServiceUnavailableException(SERVICE_NAME);
    }

    @Override
    public List<Long> findExistingUserIds(@RequestParam("ids") List<Long> userIds) {
        throw new ServiceUnavailableException(SERVICE_NAME);
    }
}
