package ru.practicum.events.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.dto.events.EventDto;
import ru.practicum.events.service.EventService;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/events")
public class InternalEventController {

    private final EventService eventService;

    @GetMapping("/{eventId}")
    public ResponseEntity<EventDto> getEventByIdWithoutHit(@PathVariable Long eventId) {
        EventDto event = eventService.getEventByIdAnyState(eventId);
        return ResponseEntity.ok(event);
    }
}
