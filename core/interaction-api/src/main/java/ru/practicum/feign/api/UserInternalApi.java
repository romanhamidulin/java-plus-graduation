package ru.practicum.feign.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import ru.practicum.dto.user.UserDto;
import ru.practicum.dto.user.UserShortDto;

import java.util.Optional;

public interface UserInternalApi {

    @GetMapping("/{userId}")
    Optional<UserDto> getUserById(@PathVariable Long userId);

    @GetMapping("/{userId}")
    UserShortDto getUserByIdShort(@PathVariable Long userId);
}
