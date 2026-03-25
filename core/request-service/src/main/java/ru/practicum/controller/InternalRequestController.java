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
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/internal/requests")
@RequiredArgsConstructor
public class InternalRequestController implements RequestInternalApi {

    private final RequestService requestService;

    @GetMapping("/event/{eventId}")
    public List<ParticipationRequestDto> getRequestsByEventId(@PathVariable Long eventId) {
        List<ParticipationRequestDto> requests = requestService.getRequestsByEventId(eventId);
        return requests;
    }

    @GetMapping("/event/{eventId}/count")
    public int getRequestsCountByEventIdAndStatus(@PathVariable Long eventId, @RequestParam(name = "status") RequestStatus status) {
        int count = requestService.getRequestsCountByEventIdAndStatus(eventId, status);
        return count;
    }

    @GetMapping("/confirmed")
    public List<ConfirmedRequests> getConfirmedRequestsByEventId(@RequestParam(name = "ids") List<Long> eventsIds) {
        List<ConfirmedRequests> confirmedRequests = requestService.getConfirmedRequestsByEventId(eventsIds);
        return confirmedRequests;
    }

    @PutMapping("/{requestId}/confirm")
    public ParticipationRequestDto updateRequestStatus(@PathVariable Long requestId, @RequestBody RequestStatus status) {
        ParticipationRequestDto request = requestService.changeRequestStatus(requestId, status);
        return request;
    }

    @Override
    @GetMapping("/check")
    public Optional<ParticipationRequestDto> findByRequesterIdAndEventIdAndStatus(
            @RequestParam("requesterId") long requesterId,
            @RequestParam("eventId") long eventId,
            @RequestParam("status") RequestStatus status) {


        return requestService.findByRequesterIdAndEventIdAndStatus(requesterId, eventId, status);
    }
}
