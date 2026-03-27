package ru.practicum.events.service;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.BooleanExpression;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.category.model.Category;
import ru.practicum.category.repository.CategoryRepository;
import ru.practicum.dto.events.EventRequestStatusUpdateRequest;
import ru.practicum.dto.events.EventRequestStatusUpdateResult;
import ru.practicum.dto.request.ParticipationRequestDto;
import ru.practicum.enums.request.RequestStatus;
import ru.practicum.dto.events.*;
import ru.practicum.dto.user.UserDto;
import ru.practicum.events.mapper.EventMapper;
import ru.practicum.events.mapper.LocationMapper;
import ru.practicum.events.model.Event;
import ru.practicum.enums.event.EventState;
import ru.practicum.events.model.GetEventAdminParam;
import ru.practicum.events.model.Location;
import ru.practicum.events.repository.EventRepository;
import ru.practicum.events.repository.LocationRepository;
import ru.practicum.exception.*;
import ru.practicum.feign.client.RequestFeignClient;
import ru.practicum.feign.client.UserFeignClient;
import ru.practicum.events.model.QEvent;

import java.time.LocalDateTime;
import java.util.*;

import static ru.practicum.enums.event.AdminUpdateStateAction.PUBLISH_EVENT;
import static ru.practicum.enums.event.EventState.*;
import static ru.practicum.enums.event.UpdateStateAction.SEND_TO_REVIEW;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventServiceImpl implements EventService {
    private final EventRepository eventRepository;
    private final UserFeignClient userFeignClient;
    private final CategoryRepository categoryRepository;
    private final LocationRepository locationRepository;
    private final RequestFeignClient requestFeignClient;
    private final ResponseEventBuilder responseEventBuilder;
    private final EventMapper eventMapper;

    @Override
    public List<EventShortDto> getEventsByOwner(Long userId, Pageable page) {
        List<Event> events = eventRepository.findByInitiatorId(userId, page);
        return responseEventBuilder.buildManyEventResponseDto(events, EventShortDto.class);
    }

    @Override
    @Transactional
    public EventFullDto addEvent(Long userId, EventCreateDto eventCreateDto) {
        Event event = eventMapper.toEvent(eventCreateDto);


        if (ValidatorEventTime.isEventTimeBad(eventCreateDto.getEventDate(), 2)) {
            throw new BadRequestException("Дата и время на которые намечено событие не может быть раньше, чем через два часа от текущего момента");
        }

        Category category = categoryRepository.findById(eventCreateDto.getCategory()).orElseThrow(
                () -> new NotFoundException("Категория не найдена"));
        event.setCategory(category);

        UserDto initiator = userFeignClient.getUserById(userId).orElseThrow(() -> new NotFoundException(String.format("Пользователь с ID %s не найден", userId)));
        event.setInitiatorId(userId);

        Location location = getOrSaveLocation(eventCreateDto.getLocation());
        event.setLocation(location);

        event = eventRepository.save(event);
        return responseEventBuilder.buildOneEventResponseDto(event, EventFullDto.class);
    }

    @Override
    public EventFullDto getEventByOwner(Long userId, Long eventId) {
        if (eventId == null || userId == null) {
            throw new ValidationException("Id должен быть указан");
        }

        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException(
                        "Событие или пользователь с данным id не найдены, или событие недоступно к просмотру данным пользователем"));

        return responseEventBuilder.buildOneEventResponseDto(event, EventFullDto.class);

    }

    @Override
    @Transactional
    public EventFullDto updateEvent(Long userId, Long eventId, EventUpdateDto eventUpdateDto) {
        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Событие не найдено"));

        EventState state = event.getState();
        if (state == PUBLISHED) {
            throw new ConflictException("Изменить можно только не опубликованные события, текущий статус " + state);
        }

        if (eventUpdateDto.hasStateAction()) {
            if (eventUpdateDto.getStateAction().equals(SEND_TO_REVIEW)) {
                event.setState(PENDING);
            } else {
                event.setState(CANCELED);
            }
        }

        if (eventUpdateDto.hasEventDate()) {
            if (ValidatorEventTime.isEventTimeBad(eventUpdateDto.getEventDate(), 2)) {
                throw new BadRequestException("Дата начала изменяемого события должна быть не ранее чем за 2 часа от даты публикации");
            }
            event.setEventDate(eventUpdateDto.getEventDate());
        }

        UpdateEventParam param = eventMapper.toUpdateParam(eventUpdateDto);
        updateEvent(event, param);

        return responseEventBuilder.buildOneEventResponseDto(event, EventFullDto.class);
    }

    private Location getOrSaveLocation(LocationDto dto) {
        Location location = LocationMapper.toLocation(dto);
        return locationRepository.findFirstByLatAndLon(location.getLat(), location.getLon()).orElseGet(
                () -> locationRepository.save(location)
        );
    }

    @Override
    public List<EventShortDto> allEvents(EntityParam param) {
        QEvent event = QEvent.event;

        BooleanBuilder requestBuilder = new BooleanBuilder();

        requestBuilder.and(event.state.eq(PUBLISHED));

        if (param.hasText()) {
            BooleanExpression descriptionExpression = event.description.like(param.getText());
            BooleanExpression annotationExpression = event.annotation.like(param.getText());
            requestBuilder.andAnyOf(descriptionExpression, annotationExpression);
        }

        if (param.hasCategories()) {
            requestBuilder.and(event.category.id.in(param.getCategories()));
        }

        if (param.hasPaid()) {
            requestBuilder.and(event.paid.eq(param.getPaid()));
        }

        requestBuilder.and(event.eventDate.gt(Objects.requireNonNullElseGet(param.getRangeStart(), LocalDateTime::now)));

        if (param.hasRangeEnd()) {
            requestBuilder.and(event.eventDate.lt(param.getRangeEnd()));
        }
        Pageable pageable = PageRequest.of(param.getFrom() / param.getSize(), param.getSize());
        List<Event> events = eventRepository.findAll(requestBuilder, pageable).getContent();
        List<EventShortDto> eventDtos = responseEventBuilder.buildManyEventResponseDto(events, EventShortDto.class);

        if (param.getOnlyAvailable()) {
            eventDtos.removeIf(dto -> dto.getConfirmedRequests() == dto.getParticipantLimit());
        }

        return eventDtos;
    }

    @Override
    public EventFullDto eventById(Long eventId) {
        Event eventDomain = eventRepository.findByIdAndState(eventId, PUBLISHED)
                .orElseThrow(() -> new NotFoundException("Событие не найдено"));

        return responseEventBuilder.buildOneEventResponseDto(eventDomain, EventFullDto.class);
    }

    @Override
    public List<EventFullDto> getEvents(GetEventAdminParam param) {
        QEvent event = QEvent.event;
        BooleanBuilder requestBuilder = new BooleanBuilder();
        if (param.hasUsers()) {
            requestBuilder.and(event.initiatorId.in(param.getUsers()));
        }

        if (param.hasStates()) {
            requestBuilder.and(event.state.in(param.getStates()));
        }

        if (param.hasCategories()) {
            requestBuilder.and(event.category.id.in(param.getCategories()));
        }

        if (param.hasRangeStart()) {
            requestBuilder.and(event.createdOn.gt(param.getRangeStart()));
        }

        if (param.hasRangeEnd()) {
            requestBuilder.and(event.createdOn.lt(param.getRangeEnd()));
        }

        List<Event> events = eventRepository.findAll(requestBuilder, param.getPage()).getContent();
        return responseEventBuilder.buildManyEventResponseDto(events, EventFullDto.class);
    }

    @Override
    @Transactional
    public EventFullDto updateEvent(Long eventId, EventAdminUpdateDto updateRequest) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие не найдено"));

        if (event.getState() != EventState.PENDING) {
            throw new ConflictException("Изменить можно только события ожидающие модерацию, текущий статус " + event.getState());
        }

        if (updateRequest.hasStateAction()) {
            EventState state;

            if (updateRequest.getStateAction() == PUBLISH_EVENT) {
                state = PUBLISHED;
                event.setPublishedOn(LocalDateTime.now());
            } else {
                state = REJECTED;
            }

            event.setState(state);
        }

        if (updateRequest.hasEventDate()) {
            if (ValidatorEventTime.isEventTimeBad(updateRequest.getEventDate(), 1)) {
                throw new BadRequestException("Дата начала изменяемого события должна быть не ранее чем за час от даты публикации");
            }
            event.setEventDate(updateRequest.getEventDate());
        }

        UpdateEventParam param = eventMapper.toUpdateParam(updateRequest);
        updateEvent(event, param);

        return responseEventBuilder.buildOneEventResponseDto(event, EventFullDto.class);

    }

    @Override
    public EventFullDto getEventByIdAnyState(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие не найдено"));
        return responseEventBuilder.buildOneEventResponseDto(event, EventFullDto.class);
    }

    @Override
    public List<ParticipationRequestDto> getUserEventRequests(Long userId, Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие не найдено"));
        if (!event.getInitiatorId().equals(userId)) {
            throw new ConflictException("Пользователь не может смотреть заявки на мероприятие, если он не является его инициатором");
        }
        return requestFeignClient.getRequestsByEventId(eventId);
    }

    @Override
    @Transactional
    public EventRequestStatusUpdateResult updateUserEventRequests(Long userId, Long eventId, EventRequestStatusUpdateRequest dto) {
        EventRequestStatusUpdateResult result = new EventRequestStatusUpdateResult();

        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Событие не найдено"));

        if (isPreModerationOff(event.getRequestModeration(), event.getParticipantLimit())) {
            return result;
        }

        List<ParticipationRequestDto> requestsAll = requestFeignClient.getRequestsByEventId(eventId);
        List<ParticipationRequestDto> requestsStatusPending = requestsAll.stream()
                .filter(r -> r.getStatus() == RequestStatus.PENDING)
                .filter(r -> dto.getRequestIds().contains(r.getId()))
                .toList();

        if (requestsStatusPending.size() != dto.getRequestIds().size()) {
            throw new ConflictException("Один или более запросов не находится в статусе PENDING");
        }

        if (dto.getStatus().equals(EventRequestStatusUpdateRequest.Status.REJECTED)) {
            for (ParticipationRequestDto request : requestsStatusPending) {
                request.setStatus(RequestStatus.REJECTED);
                result.getRejectedRequests().add(request);
            }

            return result;
        }

        long participantCount = requestsAll.stream()
                .filter(r -> r.getStatus() == RequestStatus.CONFIRMED)
                .count();

        if (participantCount == event.getParticipantLimit()) {
            throw new ConflictException("Достигнут лимит заявок на событие");
        }

        long limitLeft = event.getParticipantLimit() - participantCount;

        int idx = 0;
        while (idx < requestsStatusPending.size() && limitLeft > 0) {
            ParticipationRequestDto request = requestsStatusPending.get(idx);
            request.setStatus(RequestStatus.CONFIRMED);

            result.getConfirmedRequests().add(request);
            requestFeignClient.updateRequestStatus(request.getId(), RequestStatus.CONFIRMED);
            limitLeft--;
            idx++;
        }

        while (idx < requestsStatusPending.size()) {
            ParticipationRequestDto request = requestsStatusPending.get(idx);
            request.setStatus(RequestStatus.REJECTED);
            requestFeignClient.updateRequestStatus(request.getId(), RequestStatus.REJECTED);
            result.getRejectedRequests().add(request);

            idx++;
        }

        return result;
    }

    private void updateEvent(Event event, UpdateEventParam param) {
        if (param.hasCategory()) {
            Category category = categoryRepository.findById(param.getCategory())
                    .orElseThrow(() -> new NotFoundException("Категория не найдена"));
            event.setCategory(category);
        }

        if (param.hasAnnotation()) {
            event.setAnnotation(param.getAnnotation());
        }

        if (param.hasDescription()) {
            event.setDescription(param.getDescription());
        }

        if (param.hasLocation()) {
            event.getLocation().setLat(param.getLocation().getLat());
            event.getLocation().setLon(param.getLocation().getLon());
        }

        if (param.hasPaid()) {
            event.setPaid(param.getPaid());
        }

        if (param.hasParticipantLimit()) {
            event.setParticipantLimit(param.getParticipantLimit());
        }

        if (param.hasRequestModeration()) {
            event.setRequestModeration(param.getRequestModeration());
        }

        if (param.hasTitle()) {
            event.setTitle(param.getTitle());
        }
    }

    private boolean isPreModerationOff(boolean moderationStatus, int limit) {
        return !moderationStatus || limit == 0;
    }
}
