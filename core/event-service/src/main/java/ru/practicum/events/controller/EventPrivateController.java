package ru.practicum.events.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.events.EventCreateDto;
import ru.practicum.dto.events.EventDto;
import ru.practicum.dto.events.EventShortDto;
import ru.practicum.dto.events.EventUpdateDto;
import ru.practicum.events.service.EventService;
import ru.practicum.dto.request.EventRequestStatusUpdateRequest;
import ru.practicum.dto.request.EventRequestStatusUpdateResult;
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
    public List<EventShortDto> getEventsByOwner(@PathVariable Long userId,
                                                @RequestParam(defaultValue = "0") @PositiveOrZero Integer from,
                                                @RequestParam(defaultValue = "10") @Positive Integer size) {
        return eventService.getEventsByOwner(userId, from, size);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EventDto addEvent(@PathVariable Long userId,
                             @Valid @RequestBody EventCreateDto eventCreateDto) {
        return eventService.addEvent(userId, eventCreateDto);
    }

    @GetMapping("/{eventId}")
    public EventDto getEventByOwner(@PathVariable Long userId,
                                    @PathVariable Long eventId) {
        return eventService.getEventByOwner(userId, eventId);
    }

    @PatchMapping("/{eventId}")
    public EventDto updateEvent(@PathVariable Long userId,
                                @PathVariable Long eventId,
                                @Valid @RequestBody EventUpdateDto eventUpdateDto) {
        return eventService.updateEvent(userId, eventId, eventUpdateDto);
    }

    @GetMapping("/{eventId}/requests")
    public List<ParticipationRequestDto> getUserEventRequests(@PathVariable Long userId,
                                                              @PathVariable Long eventId) {
        return eventService.getUserEventRequests(userId, eventId);
    }

    @PatchMapping("/{eventId}/requests")
    public EventRequestStatusUpdateResult updateUserEventRequests(@PathVariable Long userId,
                                                                  @PathVariable Long eventId,
                                                                  @Valid @RequestBody EventRequestStatusUpdateRequest dto) {
        return eventService.updateUserEventRequests(userId, eventId, dto);
    }
}
