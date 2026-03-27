package ru.practicum.feign.fallback;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import ru.practicum.dto.events.EventFullDto;
import ru.practicum.exception.ServiceUnavailableException;
import ru.practicum.feign.api.EventInternalApi;

import java.util.Optional;

@Component
public class EventFallbackClient implements EventInternalApi {

    private static final String SERVICE_NAME = "event-service";

    @Override
    public Optional<ResponseEntity<EventFullDto>> getEventById(Long eventId) {
        throw new ServiceUnavailableException(SERVICE_NAME);
    }
}
