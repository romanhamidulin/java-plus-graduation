package ru.practicum.feign.fallback;

import org.springframework.stereotype.Component;
import ru.practicum.dto.events.EventDto;
import ru.practicum.exception.ServiceUnavailableException;
import ru.practicum.feign.api.EventInternalApi;

import java.util.Optional;

@Component
public class EventFallbackClient implements EventInternalApi {

    private static final String SERVICE_NAME = "event-service";

    @Override
    public Optional<EventDto> findByIdAndInitiatorId(Long eventId, Long userId) {
        throw new ServiceUnavailableException(SERVICE_NAME);
    }

    @Override
    public Optional<EventDto> getEventById(Long eventId) {
        throw new ServiceUnavailableException(SERVICE_NAME);
    }
}
