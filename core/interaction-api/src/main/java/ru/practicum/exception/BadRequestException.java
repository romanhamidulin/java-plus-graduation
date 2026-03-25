package ru.practicum.exception;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BadRequestException extends RuntimeException {
    ApiError error;

    public BadRequestException(String message) {
        super(message);
        error = new ApiError(
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "Неправильно создан запрос",
                message,
                LocalDateTime.now());
    }
}
