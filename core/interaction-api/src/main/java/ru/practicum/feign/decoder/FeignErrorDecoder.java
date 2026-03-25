package ru.practicum.feign.decoder;

import feign.Response;
import feign.codec.ErrorDecoder;
import ru.practicum.exception.BadRequestException;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.InternalServerException;
import ru.practicum.exception.NotFoundException;

public class FeignErrorDecoder implements ErrorDecoder {
    private final ErrorDecoder defaultDecoder = new Default();

    @Override
    public Exception decode(String methodKey, Response response) {
        String msg = "Body empty";

        switch (response.status()) {
            case 400 -> {
                return new BadRequestException(msg);
            }
            case 404 -> {
                return new NotFoundException(msg);
            }
            case 409 -> {
                return new ConflictException(msg);
            }
            case 500 -> {
                return new InternalServerException(msg);
            }
            default -> {
                return defaultDecoder.decode(methodKey, response);
            }
        }
    }
}
