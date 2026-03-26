package ru.practicum.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.user.UserDto;
import ru.practicum.service.UserService;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/users")
public class InternalUserController {
    private final UserService userService;

    @GetMapping("/{userId}")
    public UserDto getUserById(@PathVariable Long userId) {
        log.info("Получен GET запрос на /users/{}", userId);
        UserDto user = userService.getUserById(userId);
        log.info("Получен пользователь с id {}: {}", userId, user);
        return user;
    }

    @GetMapping("/existing")
    public List<Long> findExistingUserIds(@RequestParam("ids") List<Long> userIds) {
        log.debug("Finding existing user IDs from: {}", userIds);
        return userService.findExistingUserIds(userIds);
    }
}
