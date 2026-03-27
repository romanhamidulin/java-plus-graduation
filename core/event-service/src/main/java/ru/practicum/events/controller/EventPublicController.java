package ru.practicum.events.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.client.StatsClient;
import ru.practicum.dto.events.EntityParam;
import ru.practicum.dto.events.EventFullDto;
import ru.practicum.dto.events.EventShortDto;
import ru.practicum.enums.event.EventSort;
import ru.practicum.events.service.EventService;
import ru.practicum.exception.BadRequestException;
import ru.practicum.stats.dto.EndpointHitDto;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
@Validated
public class EventPublicController {
    private final EventService service;
    private final StatsClient statClient;

    @GetMapping
    public ResponseEntity<List<EventShortDto>> allEvents(@RequestParam(required = false) String text,
                                                         @RequestParam(defaultValue = "views") String sort,
                                                         @Min(0) @RequestParam(defaultValue = "0") Integer from,
                                                         @Positive @RequestParam(defaultValue = "10") Integer size,
                                                         @RequestParam(required = false) List<Long> categories,
                                                         @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime rangeStart,
                                                         @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime rangeEnd,
                                                         @RequestParam(required = false) Boolean paid,
                                                         @RequestParam(defaultValue = "false") Boolean onlyOnAvailable,
                                                         HttpServletRequest request) {
        if (rangeStart != null && rangeEnd != null && rangeEnd.isBefore(rangeStart)) {
            throw new BadRequestException("rangeStart > rangeEnd");
        }
        hit(request);

        EntityParam entityParam = EntityParam.builder()
                .text(text)
                .sort(EventSort.from(sort))
                .from(from)
                .size(size)
                .categories(categories)
                .rangeStart(rangeStart)
                .rangeEnd(rangeEnd)
                .paid(paid)
                .onlyAvailable(onlyOnAvailable)
                .build();
        List<EventShortDto> events = service.allEvents(entityParam);
        return ResponseEntity.ok(events);
    }

    @GetMapping("/{eventId}")
    public ResponseEntity<EventFullDto> findEventById(@Positive @PathVariable Long eventId, HttpServletRequest request) {
        hit(request);
        EventFullDto event = service.eventById(eventId);
        return ResponseEntity.ok(event);
    }

    private void hit(HttpServletRequest request) {
        EndpointHitDto hitDto = new EndpointHitDto();
        hitDto.setApp("ewm-main-service");
        hitDto.setIp(request.getRemoteAddr());
        hitDto.setUri(request.getRequestURI());
        hitDto.setTimestamp(LocalDateTime.now());
        statClient.hit(hitDto);
    }
}
