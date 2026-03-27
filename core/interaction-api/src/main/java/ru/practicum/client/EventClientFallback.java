package ru.practicum.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.practicum.dto.event.EventDto;

@Component
@Slf4j
public class EventClientFallback implements EventClient {

    @Override
    public EventDto getEvent(Long eventId) {
        log.warn("Сервис событий недоступен");
        return null;
    }
}
