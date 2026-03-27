package ru.practicum.events.service;

import org.springframework.data.domain.Pageable;
import ru.practicum.dto.events.*;
import ru.practicum.dto.events.EventRequestStatusUpdateRequest;
import ru.practicum.dto.events.EventRequestStatusUpdateResult;
import ru.practicum.dto.request.ParticipationRequestDto;
import ru.practicum.events.model.GetEventAdminParam;

import java.util.List;

public interface EventService {
    List<EventShortDto> getEventsByOwner(Long userId, Pageable page);

    EventFullDto addEvent(Long userId, EventCreateDto eventCreateDto);

    List<EventShortDto> allEvents(EntityParam params);

    EventFullDto getEventByOwner(Long userId, Long eventId);

    EventFullDto updateEvent(Long userId, Long eventId, EventUpdateDto eventUpdateDto);

    EventFullDto eventById(Long evenId);

    List<EventFullDto> getEvents(GetEventAdminParam param);

    EventFullDto updateEvent(Long eventId, EventAdminUpdateDto updateRequest);

    EventFullDto getEventByIdAnyState(Long eventId);

    List<ParticipationRequestDto> getUserEventRequests(Long userId, Long eventId);

    EventRequestStatusUpdateResult updateUserEventRequests(Long userId, Long eventId, EventRequestStatusUpdateRequest dto);
}
