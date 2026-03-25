package ru.practicum.feign.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import ru.practicum.dto.events.EventDto;

import java.util.Optional;

public interface EventInternalApi {

    @GetMapping("/{eventId}")
    Optional<EventDto> findByIdAndInitiatorId(@PathVariable Long eventId, Long userId);

    @GetMapping("/{eventId}")
    Optional<EventDto> getEventById(@PathVariable Long eventId);
}
