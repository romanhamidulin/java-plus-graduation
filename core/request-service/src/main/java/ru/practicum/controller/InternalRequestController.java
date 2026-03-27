package ru.practicum.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.request.ConfirmedRequestsDto;
import ru.practicum.dto.request.ParticipationRequestDto;
import ru.practicum.enums.request.RequestStatus;
import ru.practicum.feign.api.RequestInternalApi;
import ru.practicum.service.RequestService;

import java.util.Collection;
import java.util.List;

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
    public List<ConfirmedRequestsDto> getConfirmedRequestsByEventId(@RequestParam(name = "ids") Collection<Long> eventsIds) {
        log.debug("Getting confirmed requests for events: {}", eventsIds);
        return requestService.getConfirmedRequestsByEventId(eventsIds);
    }

    @Override
    @PutMapping("/{requestId}/confirm")
    public ParticipationRequestDto updateRequestStatus(@PathVariable Long requestId, @RequestBody RequestStatus status) {
        log.debug("Updating request: {} status to: {}", requestId, status);
        return requestService.changeRequestStatus(requestId, status);
    }

}
