package ru.practicum.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.request.ConfirmedRequests;
import ru.practicum.dto.request.ParticipationRequestDto;
import ru.practicum.enums.request.RequestStatus;
import ru.practicum.exception.NotFoundException;
import ru.practicum.feign.api.RequestInternalApi;
import ru.practicum.service.RequestService;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/internal/requests")
@RequiredArgsConstructor
public class InternalRequestController implements RequestInternalApi {

    private final RequestService requestService;

    @Override
    @GetMapping("/event/{eventId}")
    public List<ParticipationRequestDto> getRequestsByEventId(@PathVariable Long eventId) {
        log.debug("Getting requests for event: {}", eventId);
        return requestService.getRequestsByEventId(eventId);
    }

    @Override
    @GetMapping("/event/{eventId}/count")
    public int getRequestsCountByEventIdAndStatus(@PathVariable Long eventId, @RequestParam(name = "status") RequestStatus status) {
        log.debug("Getting requests count for event: {}, status: {}", eventId, status);
        return requestService.getRequestsCountByEventIdAndStatus(eventId, status);
    }

    @Override
    @GetMapping("/confirmed")
    public List<ConfirmedRequests> getConfirmedRequestsByEventId(@RequestParam(name = "ids") List<Long> eventsIds) {
        log.debug("Getting confirmed requests for events: {}", eventsIds);
        return requestService.getConfirmedRequestsByEventId(eventsIds);
    }

    @Override
    @PutMapping("/{requestId}/confirm")
    public ParticipationRequestDto updateRequestStatus(@PathVariable Long requestId, @RequestBody RequestStatus status) {
        log.debug("Updating request: {} status to: {}", requestId, status);
        return requestService.changeRequestStatus(requestId, status);
    }

    @Override
    @GetMapping("/check")
    public Optional<ParticipationRequestDto> findByRequesterIdAndEventIdAndStatus(
            @RequestParam("requesterId") long requesterId,
            @RequestParam("eventId") long eventId,
            @RequestParam("status") RequestStatus status) {

        log.debug("Finding request by requesterId: {}, eventId: {}, status: {}", requesterId, eventId, status);

        return requestService.findByRequesterIdAndEventIdAndStatus(requesterId, eventId, status);
    }

    @Override
    @GetMapping("/by-status-and-event")
    public List<ParticipationRequestDto> findAllByStatusAndEvent_Id(
            @RequestParam("status") RequestStatus status,
            @RequestParam("eventId") Long eventId) {

        log.debug("Finding all requests by status: {} and eventId: {}", status, eventId);

        return requestService.findAllByStatusAndEventId(status, eventId);
    }

    @Override
    @GetMapping("/counts")
    public Map<Long, Long> getConfirmedRequestsCountsForEvents(@RequestParam("ids") List<Long> eventIds) {
        log.debug("Getting confirmed requests counts for events: {}", eventIds);

        return requestService.getConfirmedRequestsCountsForEvents(eventIds);
    }
}
