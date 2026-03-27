package ru.practicum.events.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.events.EventAdminUpdateDto;
import ru.practicum.dto.events.EventFullDto;
import ru.practicum.enums.event.EventState;
import ru.practicum.events.model.GetEventAdminParam;
import ru.practicum.events.service.EventService;

import java.time.LocalDateTime;
import java.util.List;


@Slf4j
@RestController
@RequestMapping(path = "/admin/events")
@RequiredArgsConstructor
@Validated
public class EventAdminController {
    private final EventService eventAdminService;

    @GetMapping
    public ResponseEntity<List<EventFullDto>>  getEvents(@RequestParam(required = false) List<Long> users,
                                    @RequestParam(required = false) List<EventState> states,
                                    @RequestParam(required = false) List<Long> categories,
                                    @RequestParam(required = false)
                                        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime rangeStart, // ← ВАЖНО!
                                    @RequestParam(required = false)
                                        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime rangeEnd,
                                    @RequestParam(defaultValue = "0") Integer from,
                                    @RequestParam(defaultValue = "10") Integer size) {

        GetEventAdminParam param = GetEventAdminParam.builder()
                .users(users)
                .states(states)
                .categories(categories)
                .rangeStart(rangeStart)
                .rangeEnd(rangeEnd)
                .page(PageRequest.of(from, size))
                .build();

        List<EventFullDto> events = eventAdminService.getEvents(param);
        log.info("Отправлен ответ GET /admin/events с телом: {}", events);
        return ResponseEntity.ok(events);
    }

    @PatchMapping("/{eventId}")
    public ResponseEntity<EventFullDto> updateEventAdmin(@PathVariable Long eventId,
                                     @Valid @RequestBody EventAdminUpdateDto updateRequest) {

        log.info("PATCH /admin/events/{} получен запрос: {}", eventId, updateRequest);

        EventFullDto event = eventAdminService.updateEvent(eventId, updateRequest);
        return ResponseEntity.ok(event);
    }
}
