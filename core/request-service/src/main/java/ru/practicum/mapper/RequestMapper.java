package ru.practicum.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;
import ru.practicum.dto.request.ConfirmedRequestsDto;
import ru.practicum.dto.events.EventRequestStatusUpdateRequest;
import ru.practicum.dto.request.ParticipationRequestDto;
import ru.practicum.enums.request.RequestStatus;
import ru.practicum.model.ConfirmedRequests;
import ru.practicum.model.Request;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface RequestMapper {
    @Mapping(source = "eventId", target = "event")
    @Mapping(source = "requesterId", target = "requester")
    ParticipationRequestDto toParticipationRequestDto(Request request);

    @Named("stateFromEventRequestStatusUpdateRequest")
    default RequestStatus statusFromUpdateRequestStatus(EventRequestStatusUpdateRequest.Status status) {
        if (status == EventRequestStatusUpdateRequest.Status.REJECTED) {
            return RequestStatus.REJECTED;
        } else {
            return RequestStatus.CONFIRMED;
        }
    }

    ConfirmedRequestsDto toConfirmedRequestsDto(ConfirmedRequests confirmedRequests);
}
