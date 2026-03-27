package ru.practicum.events.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;
import ru.practicum.dto.events.*;
import ru.practicum.enums.event.AdminUpdateStateAction;
import ru.practicum.enums.event.EventState;
import ru.practicum.enums.event.UpdateStateAction;
import ru.practicum.events.model.Event;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface EventMapper {
    @Mapping(source = "category", target = "id", ignore = true)
    Event toEvent(EventCreateDto newEventDto);

    @Mapping(source = "category", target = "id", ignore = true)
    @Mapping(source = "stateAction", target = "state", qualifiedByName = "stateFromAdminAction")
    Event toEvent(EventAdminUpdateDto updateEventAdminRequest);

    @Mapping(source = "category", target = "id", ignore = true)
    @Mapping(source = "stateAction", target = "state", qualifiedByName = "stateFromUserAction")
    Event toEvent(EventUpdateDto updateEventUserRequest);

    EventShortDto toEventShortDto(Event event);

    EventFullDto toEventFullDto(Event event);

    UpdateEventParam toUpdateParam(EventAdminUpdateDto request);

    UpdateEventParam toUpdateParam(EventUpdateDto request);

    @Named("stateFromAdminAction")
    default EventState stateFromAdminAction(AdminUpdateStateAction action) {
        if (action == null) {
            return null;
        }

        if (action == AdminUpdateStateAction.PUBLISH_EVENT) {
            return EventState.PUBLISHED;
        } else {
            return EventState.REJECTED;
        }
    }

    @Named("stateFromUserAction")
    default EventState stateFromUserAction(UpdateStateAction action) {
        if (action == null) {
            return null;
        }

        if (action == UpdateStateAction.SEND_TO_REVIEW) {
            return EventState.PENDING;
        } else {
            return EventState.CANCELED;
        }
    }
}