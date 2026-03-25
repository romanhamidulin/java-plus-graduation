package ru.practicum.service;

import ru.practicum.dto.request.ConfirmedRequests;
import ru.practicum.dto.request.EventRequestStatusUpdateRequest;
import ru.practicum.dto.request.EventRequestStatusUpdateResult;
import ru.practicum.dto.request.ParticipationRequestDto;
import ru.practicum.enums.request.RequestStatus;
import ru.practicum.model.Request;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RequestService {
    List<ParticipationRequestDto> getUserEventRequests(Long userId, Long eventId);

    EventRequestStatusUpdateResult updateUserEventRequests(Long userId, Long eventId, EventRequestStatusUpdateRequest dto);

    List<ParticipationRequestDto> getRequestsByRequester(Long userId);

    ParticipationRequestDto addRequest(Long userId, Long eventId);

    ParticipationRequestDto cancelRequest(Long userId, Long requestId);

    List<ParticipationRequestDto> getRequestsByEventId(Long eventId);

    int getRequestsCountByEventIdAndStatus(Long eventId, RequestStatus status);

    List<ConfirmedRequests> getConfirmedRequestsByEventId(List<Long> eventIds);

    ParticipationRequestDto changeRequestStatus(Long requestId, RequestStatus status);

    Optional<ParticipationRequestDto> findByRequesterIdAndEventIdAndStatus(long authorId, long eventId, RequestStatus requestStatus);
}
