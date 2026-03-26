package ru.practicum.service;

import ru.practicum.dto.user.NewUserRequest;
import ru.practicum.dto.user.UserDto;
import ru.practicum.dto.user.UserRequest;

import java.util.List;

public interface UserService {
    List<UserDto> getUsers(UserRequest request);

    UserDto createUser(NewUserRequest newUserRequest);

    void deleteUser(Long userId);

    UserDto getUserById(Long userId);

    List<Long> findExistingUserIds(List<Long> userIds);
}
