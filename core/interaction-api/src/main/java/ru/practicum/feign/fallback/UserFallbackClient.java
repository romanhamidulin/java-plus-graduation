package ru.practicum.feign.fallback;

import org.springframework.stereotype.Component;
import ru.practicum.dto.user.UserDto;
import ru.practicum.dto.user.UserShortDto;
import ru.practicum.exception.ServiceUnavailableException;
import ru.practicum.feign.api.UserInternalApi;

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
}
