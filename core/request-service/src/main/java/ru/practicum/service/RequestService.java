package ru.practicum.service;

import ru.practicum.dto.request.ConfirmedRequestsDto;
import ru.practicum.dto.request.ParticipationRequestDto;
import ru.practicum.enums.request.RequestStatus;

import java.util.Collection;
import java.util.List;

public interface RequestService {
    List<ParticipationRequestDto> getRequestsByRequester(Long userId);

    ParticipationRequestDto addRequest(Long userId, Long eventId);

    ParticipationRequestDto cancelRequest(Long userId, Long requestId);

    List<ParticipationRequestDto> getRequestsByEventId(Long eventId);

    int getRequestsCountByEventIdAndStatus(Long eventId, RequestStatus status);

    List<ConfirmedRequestsDto> getConfirmedRequestsByEventId(Collection<Long> eventIds);

    ParticipationRequestDto changeRequestStatus(Long requestId, RequestStatus status);
}
