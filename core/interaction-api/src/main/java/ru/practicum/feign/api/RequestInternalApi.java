package ru.practicum.feign.api;

import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.request.ConfirmedRequests;
import ru.practicum.dto.request.ParticipationRequestDto;
import ru.practicum.enums.request.RequestStatus;

import java.util.List;
import java.util.Optional;

public interface RequestInternalApi {
    @GetMapping("/event/{eventId}")
    List<ParticipationRequestDto> getRequestsByEventId(@PathVariable Long eventId);

    @GetMapping("/event/{eventId}/count")
    int getRequestsCountByEventIdAndStatus(@PathVariable Long eventId, @RequestParam(name = "status") RequestStatus status);

    @GetMapping("/confirmed")
    List<ConfirmedRequests> getConfirmedRequestsByEventId(@RequestParam(name = "ids") List<Long> eventsIds);

    @PutMapping("/{requestId}/confirm")
    ParticipationRequestDto updateRequestStatus(@PathVariable Long requestId, @RequestBody RequestStatus status);

    @GetMapping("/check")
    Optional<ParticipationRequestDto> findByRequesterIdAndEventIdAndStatus(
            @RequestParam("requesterId") long requesterId,
            @RequestParam("eventId") long eventId,
            @RequestParam("status") RequestStatus status);
}
