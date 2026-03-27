package ru.practicum.events.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.events.*;
import ru.practicum.events.service.EventService;
import ru.practicum.dto.events.EventRequestStatusUpdateRequest;
import ru.practicum.dto.events.EventRequestStatusUpdateResult;
import ru.practicum.dto.request.ParticipationRequestDto;
import ru.practicum.feign.client.RequestFeignClient;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/users/{userId}/events")
@Validated
public class EventPrivateController {
    private final EventService eventService;
    private final RequestFeignClient requestFeignClient;

    @GetMapping
    public ResponseEntity<List<EventShortDto>> getEventsByOwner(@PathVariable Long userId,
                                                                @RequestParam(defaultValue = "0") @Min(0) Integer from,
                                                                @RequestParam(defaultValue = "10") @Min(1) Integer size) {
        List<EventShortDto> events = eventService.getEventsByOwner(userId, PageRequest.of(from, size));
        return ResponseEntity.ok(events);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<EventFullDto> addEvent(@PathVariable Long userId,
                                 @Valid @RequestBody EventCreateDto eventCreateDto) {
        EventFullDto event = eventService.addEvent(userId, eventCreateDto);
        return new ResponseEntity<>(event, HttpStatus.CREATED);
    }

    @GetMapping("/{eventId}")
    public ResponseEntity<EventFullDto> getEventByOwner(@PathVariable Long userId,
                                    @PathVariable Long eventId) {
        EventFullDto event = eventService.getEventByOwner(userId, eventId);
        return ResponseEntity.ok(event);

    }

    @PatchMapping("/{eventId}")
    public ResponseEntity<EventFullDto> updateEvent(@PathVariable Long userId,
                                @PathVariable Long eventId,
                                @Valid @RequestBody EventUpdateDto eventUpdateDto) {
        EventFullDto event = eventService.updateEvent(userId, eventId, eventUpdateDto);
        return ResponseEntity.ok(event);
    }

    @GetMapping("/{eventId}/requests")
    public ResponseEntity<List<ParticipationRequestDto>> getUserEventRequests(@PathVariable Long userId,
                                                              @PathVariable Long eventId) {
        List<ParticipationRequestDto> requests = eventService.getUserEventRequests(userId, eventId);
        return ResponseEntity.ok(requests);
    }

    @PatchMapping("/{eventId}/requests")
    public ResponseEntity<EventRequestStatusUpdateResult> updateUserEventRequests(@PathVariable Long userId,
                                                                  @PathVariable Long eventId,
                                                                  @Valid @RequestBody EventRequestStatusUpdateRequest dto) {
        EventRequestStatusUpdateResult result = eventService.updateUserEventRequests(userId, eventId, dto);
        log.info("Отправлен ответ на PATCH /users/{}/events/{},requests с телом: {}", userId, eventId, result);
        return ResponseEntity.ok(result);
    }
}
