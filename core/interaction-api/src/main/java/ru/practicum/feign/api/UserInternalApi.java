package ru.practicum.feign.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import ru.practicum.dto.user.UserDto;

import java.util.Optional;

public interface UserInternalApi {

    @GetMapping("/{userId}")
    UserDto getUserById(@PathVariable Long userId);
}
