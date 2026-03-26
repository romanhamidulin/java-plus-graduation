package ru.practicum.mapper;

import lombok.experimental.UtilityClass;
import ru.practicum.dto.request.ConfirmedRequests;
import ru.practicum.dto.request.ParticipationRequestDto;
import ru.practicum.model.Request;

@UtilityClass
public class RequestMapper {
    public ParticipationRequestDto toParticipationRequestDto(Request req) {
        return ParticipationRequestDto.builder()
                .id(req.getId())
                .event(req.getEventId())
                .requester(req.getRequesterId())
                .created(req.getCreatedOn())
                .status(req.getStatus())
                .build();
    }

    public ConfirmedRequests toConfirmedRequestsDto(ConfirmedRequests confirmedRequests){
        return confirmedRequests.builder().
                event(confirmedRequests.getEvent()).
                count(confirmedRequests.getCount())
                .build();
    }
}
