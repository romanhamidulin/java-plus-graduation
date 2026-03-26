package ru.practicum.events.service;

import ru.practicum.client.StatsClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.practicum.dto.events.EventDto;
import ru.practicum.dto.events.EventFullDto;
import ru.practicum.dto.events.ResponseEvent;
import ru.practicum.dto.request.ConfirmedRequests;
import ru.practicum.stats.dto.ViewStats;
import ru.practicum.dto.comment.CommentDto;
import ru.practicum.dto.events.EventShortDto;
import ru.practicum.dto.user.UserDto;
import ru.practicum.dto.user.UserShortDto;
import ru.practicum.enums.request.RequestStatus;
import ru.practicum.events.mapper.EventMapper;
import ru.practicum.events.model.Event;
import ru.practicum.feign.client.CommentFeignClient;
import ru.practicum.feign.client.RequestFeignClient;
import ru.practicum.feign.client.UserFeignClient;

import java.time.LocalDateTime;
import java.util.*;


@Slf4j
@Component
@RequiredArgsConstructor
public class ResponseEventBuilder {
    private final EventMapper eventMapper;
    private final RequestFeignClient requestFeignClient;
    private final CommentFeignClient commentFeignClient;
    private final UserFeignClient userFeignClient;
    private final StatsClient statsClient;

    public <T extends ResponseEvent> T buildOneEventResponseDto(Event event, Class<T> type) {
        T dto;
        UserDto user = userFeignClient.getUserById(event.getInitiatorId()).orElseThrow();
        UserShortDto initiator = new UserShortDto();
        initiator.setId(user.getId());
        initiator.setName(user.getName());
        if (type == EventFullDto.class) {
            EventDto dtoTemp = EventMapper.toEventDto(event,initiator);
            dtoTemp.setInitiator(initiator);
            dto = type.cast(dtoTemp);
        } else {
            EventShortDto dtoTemp = eventMapper.toEventShortDto(event,initiator);
            dtoTemp.setInitiator(initiator);
            dto = type.cast(dtoTemp);
        }

        long eventId = event.getId();
        LocalDateTime created = event.getCreatedOn();

        dto.setConfirmedRequests(getOneEventConfirmedRequests(eventId));
        dto.setViews(getOneEventViews(created, eventId));
        dto.setComments(getOneEventComments(eventId));
        return dto;
    }

    public <T extends ResponseEvent> List<T> buildManyEventResponseDto(List<Event> events, Class<T> type) {
        Map<Long, T> dtoById = new HashMap<>();

        for (Event event : events) {
            UserDto initiator = userFeignClient.getUserById(event.getInitiatorId()).orElseThrow();
            UserShortDto initiators = new UserShortDto();
            initiator.setId(initiator.getId());
            initiator.setName(initiator.getName());
            if (type == EventFullDto.class) {
                EventDto dtoTemp = EventMapper.toEventDto(event,initiators);
                dtoTemp.setInitiator(new UserShortDto(initiator.getId(), initiator.getName()));
                dtoById.put(event.getId(), type.cast(dtoTemp));
            } else {
                EventShortDto dtoTemp = eventMapper.toEventShortDto(event,initiators);
                dtoTemp.setInitiator(new UserShortDto(initiator.getId(), initiator.getName()));
                dtoById.put(event.getId(), type.cast(dtoTemp));
            }
        }

        getManyEventsConfirmedRequests(dtoById.keySet()).forEach(req ->
                dtoById.get(req.getEvent()).setConfirmedRequests((int) req.getCount()));


        getManyEventsViews(dtoById.keySet()).forEach(stats -> {
            Long id = Long.parseLong(stats.getUri().replace("/events/", ""));
            dtoById.get(id).setViews(stats.getHits());
        });

        getManyEventsComments(dtoById.keySet()).forEach(comment -> {
            T t = dtoById.get(comment);

            if (t.getComments() == null) {
                t.setComments(new ArrayList<>());
            }

            t.getComments().add(comment);
        });

        return new ArrayList<>(dtoById.values());
    }

    private int getOneEventConfirmedRequests(long eventId) {
        return requestFeignClient.getRequestsCountByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);
    }

    private long getOneEventViews(LocalDateTime created, long eventId) {
        List<ViewStats> viewStats = statsClient.getStats(created.minusMinutes(1), LocalDateTime.now().plusMinutes(1), List.of("/events/" + eventId), true).getBody();
        return viewStats == null || viewStats.isEmpty() ? 0 : viewStats.getFirst().getHits();
    }

    private List<CommentDto> getOneEventComments(long eventId) {
        List<CommentDto> comments = commentFeignClient.getCommentsByEventId(eventId);
        return comments == null ? new ArrayList<>() : comments;
    }

    private List<ConfirmedRequests> getManyEventsConfirmedRequests(Collection<Long> eventIds) {
        List<ConfirmedRequests> requests = requestFeignClient.getConfirmedRequestsByEventId(eventIds.stream().toList());
        return requests == null ? new ArrayList<>() : requests;
    }

    private List<ViewStats> getManyEventsViews(Collection<Long> eventIds) {
        List<String> uris = eventIds.stream()
                .map(id -> "/events/" + id)
                .toList();

        return statsClient.getStats(LocalDateTime.of(1970, 1, 1, 0, 0), LocalDateTime.now().plusMinutes(1), uris, true).getBody();
    }

    private List<CommentDto> getManyEventsComments(Set<Long> eventsIds) {
        return commentFeignClient.getLastCommentsForEvents(eventsIds);
    }
}
