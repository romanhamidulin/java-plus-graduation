package ru.practicum.feign.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import ru.practicum.dto.events.EventFullDto;

import java.util.Optional;

public interface EventInternalApi {

    @GetMapping("/{eventId}")
    Optional<ResponseEntity<EventFullDto>> getEventById(@PathVariable Long eventId);
}
