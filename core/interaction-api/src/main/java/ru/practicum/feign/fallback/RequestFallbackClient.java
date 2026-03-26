package ru.practicum.feign.fallback;

import org.springframework.stereotype.Component;
import ru.practicum.dto.request.ConfirmedRequests;
import ru.practicum.dto.request.ParticipationRequestDto;
import ru.practicum.enums.request.RequestStatus;
import ru.practicum.exception.ServiceUnavailableException;
import ru.practicum.feign.api.RequestInternalApi;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class RequestFallbackClient implements RequestInternalApi {

    private static final String SERVICE_NAME = "request-service";

    @Override
    public List<ParticipationRequestDto> getRequestsByEventId(Long eventId) {
        throw new ServiceUnavailableException(SERVICE_NAME);
    }

    @Override
    public int getRequestsCountByEventIdAndStatus(Long eventId, RequestStatus status) {
        throw new ServiceUnavailableException(SERVICE_NAME);
    }

    @Override
    public List<ConfirmedRequests> getConfirmedRequestsByEventId(List<Long> eventsIds) {
        throw new ServiceUnavailableException(SERVICE_NAME);
    }

    @Override
    public ParticipationRequestDto updateRequestStatus(Long requestId, RequestStatus status) {
        throw new ServiceUnavailableException(SERVICE_NAME);
    }

    @Override
    public Optional<ParticipationRequestDto> findByRequesterIdAndEventIdAndStatus(long requesterId, long eventId, RequestStatus status) {
        throw new ServiceUnavailableException(SERVICE_NAME);
    }

    @Override
    public List<ParticipationRequestDto> findAllByStatusAndEvent_Id(RequestStatus status, Long eventId) {
        throw new ServiceUnavailableException(SERVICE_NAME);
    }

    @Override
    public Map<Long, Long> getConfirmedRequestsCountsForEvents(List<Long> eventIds) {
        throw new ServiceUnavailableException(SERVICE_NAME);
    }
}
