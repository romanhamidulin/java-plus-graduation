package ru.practicum.events.service;

import com.querydsl.core.types.Expression;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.category.model.Category;
import ru.practicum.category.repository.CategoryRepository;
import ru.practicum.client.StatsClient;
import ru.practicum.dto.request.EventRequestStatusUpdateRequest;
import ru.practicum.dto.request.EventRequestStatusUpdateResult;
import ru.practicum.dto.request.ParticipationRequestDto;
import ru.practicum.dto.user.UserShortDto;
import ru.practicum.enums.comment.CommentStatus;
import ru.practicum.enums.event.AdminUpdateStateAction;
import ru.practicum.enums.request.RequestStatus;
import ru.practicum.dto.comment.CommentDto;
import ru.practicum.dto.events.*;
import ru.practicum.dto.user.UserDto;
import ru.practicum.enums.event.EventSort;
import ru.practicum.events.mapper.EventMapper;
import ru.practicum.events.mapper.LocationMapper;
import ru.practicum.events.model.Event;
import ru.practicum.enums.event.EventState;
import ru.practicum.events.model.Location;
import ru.practicum.events.repository.EventRepository;
import ru.practicum.events.repository.LocationRepository;
import ru.practicum.exception.*;
import ru.practicum.feign.client.CommentFeignClient;
import ru.practicum.feign.client.RequestFeignClient;
import ru.practicum.feign.client.UserFeignClient;
import ru.practicum.stats.dto.EndpointHitDto;
import ru.practicum.stats.dto.ViewStats;
import ru.practicum.events.model.QEvent;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

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
    private final StatsClient client;
    private final CommentFeignClient commentFeignClient;

    @Override
    public List<EventShortDto> getEventsByOwner(Long userId, Integer from, Integer size) {
        Pageable pageable = PageRequest.of(from / size, size);
        List<Event> events = eventRepository.findAllByInitiatorId(userId, pageable).toList();
        return buildEvents(events);
    }

    @Override
    @Transactional
    public EventDto addEvent(Long userId, EventCreateDto eventCreateDto) {
        log.info("Валидация даты и времени события");
        validateEventDate(eventCreateDto.getEventDate(), EventState.PENDING);

        Event event = EventMapper.toEvent(eventCreateDto);

        log.info("Добавление инициатора события");
        UserDto user = userFeignClient.getUserById(userId).orElseThrow(
                () -> new NotFoundException("Пользователь с данным id не найден")
        );
        event.setInitiatorId(user.getId());

        log.info("Добавление категории события");
        Category category = categoryRepository.findById(eventCreateDto.getCategory()).orElseThrow(
                () -> new NotFoundException("Категория с данным id не найдена")
        );
        event.setCategory(category);

        log.info("Добавление локации события");
        Location location = getOrSaveLocation(eventCreateDto.getLocation());
        event.setLocation(location);

        event.setState(EventState.PENDING);
        event.setCreatedOn(LocalDateTime.now());

        event = eventRepository.save(event);
        UserShortDto initiator = getUserShortDto(event.getInitiatorId());
        EventDto res = EventMapper.toEventDto(event, initiator);
        res.setConfirmedRequests((long) requestFeignClient.getRequestsCountByEventIdAndStatus(event.getId(), RequestStatus.CONFIRMED));
        res.setComments(new ArrayList<>());

        return res;
    }

    @Override
    public EventDto getEventByOwner(Long userId, Long eventId) {
        if (eventId == null || userId == null) {
            throw new ValidationException("Id должен быть указан");
        }

        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException(
                        "Событие или пользователь с данным id не найдены, или событие недоступно к просмотру данным пользователем"));

        UserShortDto initiator = getUserShortDto(event.getInitiatorId());
        EventDto result = EventMapper.toEventDto(event, initiator);
        enrichEventDtoWithAdditionalData(result, eventId);

        return result;

    }

    @Override
    @Transactional
    public EventDto updateEvent(Long userId, Long eventId, EventUpdateDto eventUpdateDto) {
        if (eventId == null || userId == null) {
            throw new ValidationException("Id должен быть указан");
        }

        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException(
                        "Событие или пользователь с данным id не найдены, или событие недоступно к редактированию данным пользователем"));

        if (event.getState().equals(EventState.PUBLISHED)) {
            throw new EventConflictException("Изменить можно только отмененные события или события в состоянии ожидания модерации");
        }

        updateEventFields(event, eventUpdateDto);

        event = eventRepository.save(event);
        UserShortDto initiator = getUserShortDto(event.getInitiatorId());
        EventDto result = EventMapper.toEventDto(event,initiator);
        enrichEventDtoWithAdditionalData(result, eventId);

        return result;
    }

    private void updateEventFields(Event event, EventUpdateDto eventUpdateDto) {
        if (eventUpdateDto.getEventDate() != null) {
            log.info("Валидация новых даты и времени события");
            validateEventDate(eventUpdateDto.getEventDate(), event.getState());
            log.info("Обновление даты и времени события");
            event.setEventDate(eventUpdateDto.getEventDate());
        }

        if (eventUpdateDto.getTitle() != null) {
            log.info("Обновление заголовка события");
            event.setTitle(eventUpdateDto.getTitle());
        }

        if (eventUpdateDto.getAnnotation() != null) {
            log.info("Обновление аннотации события");
            event.setAnnotation(eventUpdateDto.getAnnotation());
        }

        if (eventUpdateDto.getDescription() != null) {
            log.info("Обновление описания события");
            event.setDescription(eventUpdateDto.getDescription());
        }

        if (eventUpdateDto.getPaid() != null) {
            log.info("Обновление флага платности события");
            event.setPaid(eventUpdateDto.getPaid());
        }

        if (eventUpdateDto.getRequestModeration() != null) {
            log.info("Обновление признака премодерации заявок на участие");
            event.setRequestModeration(eventUpdateDto.getRequestModeration());
        }

        if (eventUpdateDto.getParticipantLimit() != null) {
            log.info("Обновление лимита пользователей собятия");
            event.setParticipantLimit(eventUpdateDto.getParticipantLimit());
        }

        if (eventUpdateDto.getCategory() != null) {
            log.info("Обновление категории события");
            Category category = categoryRepository.findById(eventUpdateDto.getCategory())
                    .orElseThrow(() -> new NotFoundException("Категория с данным id не найдена"));
            event.setCategory(category);
        }

        if (eventUpdateDto.getLocation() != null) {
            log.info("Обновление локации события");
            Location location = getOrSaveLocation(eventUpdateDto.getLocation());
            event.setLocation(location);
        }

        if (eventUpdateDto.getStateAction() != null) {
            switch (eventUpdateDto.getStateAction()) {
                case SEND_TO_REVIEW:
                    event.setState(EventState.PENDING);
                    break;
                case CANCEL_REVIEW:
                    event.setState(EventState.CANCELED);
                    break;
            }
        }
    }

    private void validateEventDate(LocalDateTime eventDate, EventState currentState) {
        LocalDateTime now = LocalDateTime.now();

        int minHours;
        if (currentState == EventState.PUBLISHED) {
            minHours = 1;
        } else {
            minHours = 2;
        }

        LocalDateTime minValidDate = now.plusHours(minHours);
        if (eventDate.isBefore(minValidDate)) {
            String message;
            if (minHours == 1) {
                message = "Дата начала изменяемого события должна быть не ранее чем за час от даты публикации";
            } else {
                message = "Дата и время на которые намечено событие не может быть раньше, чем через два часа от текущего момента";
            }
            throw new ValidationException(message);
        }
    }

    private Location getOrSaveLocation(LocationDto dto) {
        Location location = LocationMapper.toLocation(dto);
        return locationRepository.findFirstByLatAndLon(location.getLat(), location.getLon()).orElseGet(
                () -> locationRepository.save(location)
        );
    }

    private void enrichEventDtoWithAdditionalData(EventDto eventDto, Long eventId) {
        try {
            int confirmedRequestsCount = requestFeignClient.getRequestsCountByEventIdAndStatus(
                    eventId, RequestStatus.CONFIRMED);
            eventDto.setConfirmedRequests((long) confirmedRequestsCount);

            List<CommentDto> comments = commentFeignClient.getCommentsByEventIdAndStatus(
                    eventId, CommentStatus.PUBLISHED);
            eventDto.setComments(comments);

        } catch (Exception e) {
            log.error("Error fetching additional data for event: {}", eventId, e);
            eventDto.setConfirmedRequests(0L);
            eventDto.setComments(List.of());
        }
    }

    @Override
    public List<EventShortDto> allEvents(EntityParam params, String ip) {

        if (params.getRangeStart() != null && params.getRangeEnd() != null
                && params.getRangeStart().isAfter(params.getRangeEnd())) {
            throw new ValidationException("Дата начала не может быть позже даты окончания");
        }

        BooleanExpression expression = prepareAndBuildQuery(params);
        Pageable pageable = PageRequest.of(params.getFrom() / params.getSize(), params.getSize());
        List<Event> events = eventRepository.findAll(expression, pageable).getContent();

        if (params.getOnlyAvailable() != null && params.getOnlyAvailable()) {
            events = filterAvailableEvents(events);
        }

        List<EventShortDto> shortDtos = buildEvents(events);

        EndpointHitDto hitDto = EndpointHitDto.builder()
                .app("main-service")
                .ip(ip)
                .uri("/events")
                .timestamp(LocalDateTime.now())
                .build();
        client.hit(hitDto);

        if (params.getSort() == EventSort.EVENT_DATE) {
            return shortDtos.stream().sorted(Comparator.comparing(EventShortDto::getEventDate)).toList().reversed();
        }

        return shortDtos.stream().sorted(Comparator.comparing(EventShortDto::getViews)).toList().reversed();
    }

    private List<Event> filterAvailableEvents(List<Event> events) {
        return events.stream()
                .filter(event -> {
                    int confirmedRequests = requestFeignClient.getRequestsCountByEventIdAndStatus(
                            event.getId(), RequestStatus.CONFIRMED);
                    return event.getParticipantLimit() == 0 ||
                            confirmedRequests < event.getParticipantLimit();
                })
                .collect(Collectors.toList());
    }

    @Override
    public EventDto eventById(Long eventId, String ip) {
        Event event = checkEvent(eventId);
        Integer countOfConfirmedInt = requestFeignClient.findAllByStatusAndEvent_Id(RequestStatus.CONFIRMED, eventId).size();
        Long countOfConfirmed = countOfConfirmedInt.longValue();
        Long countOfViews = getViews(eventId);

        List<CommentDto> comments = commentFeignClient.getCommentsByEventIdAndStatus(
                eventId, CommentStatus.PUBLISHED);

        UserShortDto initiator = getUserShortDto(event.getInitiatorId());
        EventDto eventDto = EventMapper.mapToDto(event, countOfConfirmed, countOfViews,initiator);
        eventDto.setComments(comments);

        EndpointHitDto hitDto = EndpointHitDto.builder()
                .app("main-service")
                .ip(ip)
                .uri("/events/" + eventId)
                .timestamp(LocalDateTime.now())
                .build();
        client.hit(hitDto);

        return eventDto;
    }

    private Event checkEvent(Long eventI) {
        Event event = eventRepository.findById(eventI)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + eventI + " не найдено"));

        if (event.getState() != EventState.PUBLISHED) {
            throw new NotFoundException("Событие с id=" + eventI + " не опубликовано");
        }

        return event;
    }

    private UserShortDto getUserShortDto(Long userId) {
        try {
            return userFeignClient.getUserByIdShort(userId);
        } catch (Exception e) {
            log.error("Ошибка при получении пользователя с id {}: {}", userId, e.getMessage());
            // Возвращаем заглушку или null, в зависимости от требований
            return null;
        }
    }

    private Long getViews(Long eventId) {
        LocalDateTime start = LocalDateTime.now().minusYears(2);
        LocalDateTime end = LocalDateTime.now();
        List<String> uri = List.of("/events/" + eventId);

        List<ViewStats> statResponse = client.getStats(start, end, uri, true).getBody();
        if (statResponse != null && !statResponse.isEmpty()) {
            return statResponse.getFirst().getHits();
        }

        return 0L;
    }

    private BooleanExpression prepareAndBuildQuery(EntityParam param) {
        QEvent event = QEvent.event;
        BooleanExpression predicate = event.state.eq(EventState.PUBLISHED);

        if (param.getText() != null && !param.getText().isEmpty()) {
            predicate = predicate.and(event.annotation.likeIgnoreCase("%" + param.getText() + "%")
                    .or(event.description.likeIgnoreCase("%" + param.getText() + "%")));
        }

        if (param.getCategories() != null && !param.getCategories().isEmpty()) {
            predicate = predicate.and(event.category.id.in(param.getCategories()));
        }

        if (param.getPaid() != null) {
            predicate = predicate.and(event.paid.eq(param.getPaid()));
        }

        if (param.getRangeStart() != null && param.getRangeEnd() != null) {
            predicate = predicate.and(event.eventDate.between(param.getRangeStart(), param.getRangeEnd()));
        } else if (param.getRangeStart() != null) {
            predicate = predicate.and(event.eventDate.goe(param.getRangeStart()));
        } else if (param.getRangeEnd() != null) {
            predicate = predicate.and(event.eventDate.loe(param.getRangeEnd()));
        } else {
            predicate = predicate.and(event.eventDate.after(LocalDateTime.now()));
        }

        return predicate;
    }

    private List<EventShortDto> buildEvents(List<Event> events) {
        List<Long> eventIds = events.stream().map(Event::getId).toList();

        Map<Long, Long> confirmedRequests = getConfirmedRequestsForEvents(eventIds);
        Map<Long, Long> views = getViewsForEvents(eventIds);
        Map<Long, List<CommentDto>> comments = getCommentsForEvents(eventIds);


        return events.stream().map(event -> {
            Long confirmedR = confirmedRequests.getOrDefault(event.getId(), 0L);
            Long view = views.getOrDefault(event.getId(), 0L);
            int commentCount = comments.getOrDefault(event.getId(), Collections.emptyList()).size();
            UserShortDto initiator = getUserShortDto(event.getInitiatorId());
            return EventMapper.mapToShortDto(event, confirmedR, view, commentCount, initiator);
        }).toList();
    }

    private Map<Long, Long> getConfirmedRequestsForEvents(List<Long> eventIds) {
        if (eventIds.isEmpty()) {
            return Map.of();
        }
        try {
            Map<Long, Long> result = new HashMap<>();
            for (Long eventId : eventIds) {
                int count = requestFeignClient.getRequestsCountByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);
                result.put(eventId, (long) count);
            }
            return result;
        } catch (Exception e) {
            log.error("Error fetching confirmed requests for events", e);
            return Map.of();
        }
    }

    private Map<Long, Long> getViewsForEvents(List<Long> eventIds) {
        if (eventIds.isEmpty()) {
            return Map.of();
        }

        LocalDateTime start = LocalDateTime.now().minusYears(2);
        LocalDateTime end = LocalDateTime.now();

        List<String> uris = eventIds.stream()
                .map(id -> "/events/" + id)
                .toList();

        List<ViewStats> statResponse = client.getStats(start, end, uris, true).getBody();
        Map<Long, Long> viewsMap = new HashMap<>();

        if (statResponse != null) {
            for (ViewStats stats : statResponse) {
                String uri = stats.getUri();
                Long eventId = Long.parseLong(uri.substring(uri.lastIndexOf('/') + 1));
                viewsMap.put(eventId, stats.getHits());
            }
        }

        return viewsMap;
    }

    @Override
    public List<EventDto> getEvents(List<Long> users, List<String> states, List<Long> categories,
                                    LocalDateTime rangeStart, LocalDateTime rangeEnd, Integer from, Integer size) {
        log.info("Поиск событий с параметрами: users={}, states={}, categories={}, rangeStart={}, rangeEnd={}, from={}, size={}",
                users, states, categories, rangeStart, rangeEnd, from, size);

        validateSearchParameters(users, states, rangeStart, rangeEnd);

        List<EventState> eventStates = parseEventStates(states);

        List<Long> usersParam = (users != null && users.isEmpty()) ? null : users;
        List<EventState> statesParam = (eventStates != null && eventStates.isEmpty()) ? null : eventStates;
        List<Long> categoriesParam = (categories != null && categories.isEmpty()) ? null : categories;

        int page = from / size;
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").ascending());

        List<Event> events = eventRepository.findEventsByAdminFilters(
                usersParam, statesParam, categoriesParam, rangeStart, rangeEnd, pageable);

        log.info("Найдено {} событий", events.size());

        List<Long> eventIds = events.stream().map(Event::getId).toList();

        Map<Long, Long> confirmedRequests = getConfirmedRequestsForEvents(eventIds);

        Map<Long, Long> views = getViewsForEvents(eventIds);

        Map<Long, List<CommentDto>> commentsMap = getCommentsForEvents(eventIds);

        return events.stream().map(event -> {
            Long confirmedR = confirmedRequests.getOrDefault(event.getId(), 0L);
            Long view = views.getOrDefault(event.getId(), 0L);
            UserShortDto initiator = getUserShortDto(event.getInitiatorId());
            EventDto dto = EventMapper.mapToDto(event, confirmedR, view,initiator);

            List<CommentDto> comments = commentsMap.get(event.getId());
            dto.setComments(comments != null ? comments : Collections.emptyList());

            return dto;
        }).collect(Collectors.toList());
    }

    private Map<Long, List<CommentDto>> getCommentsForEvents(List<Long> eventIds) {
        if (eventIds.isEmpty()) {
            return Map.of();
        }

        try {
            Map<Long, List<CommentDto>> commentsMap = new HashMap<>();
            for (Long eventId : eventIds) {
                List<CommentDto> comments = commentFeignClient.getCommentsByEventIdAndStatus(
                        eventId, CommentStatus.PUBLISHED);
                commentsMap.put(eventId, comments);
            }
            return commentsMap;
        } catch (Exception e) {
            log.error("Error fetching comments for events", e);
            return Map.of();
        }
    }

    @Override
    @Transactional
    public EventDto updateEvent(Long eventId, EventAdminUpdateDto updateRequest) {
        log.info("Обновление события с id = {} администратором: {}", eventId, updateRequest);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id = " + eventId + " не найдено"));

        validateEventForAdminUpdate(event, updateRequest);

        updateEventFields(event, updateRequest);

        if (updateRequest.getStateAction() != null) {
            processAdminStateAction(event, updateRequest.getStateAction());
        }

        Event updatedEvent = eventRepository.save(event);
        log.info("Событие с id = {} успешно обновлено администратором", eventId);
        UserShortDto initiator = getUserShortDto(event.getInitiatorId());
        EventDto result = EventMapper.toEventDto(updatedEvent,initiator);

        int confirmedRequestsCount = requestFeignClient.getRequestsCountByEventIdAndStatus(
                eventId, RequestStatus.CONFIRMED);
        result.setConfirmedRequests((long) confirmedRequestsCount);

        Long views = getViews(eventId);
        result.setViews(views);

        try {
            List<CommentDto> comments = commentFeignClient.getCommentsByEventIdAndStatus(
                    eventId, CommentStatus.PUBLISHED);
            result.setComments(comments);
        } catch (Exception e) {
            log.error("Error fetching comments for event: {}", eventId, e);
            result.setComments(List.of());
        }

        return result;

    }

    @Override
    public EventDto getEventByIdAnyState(Long eventId) {
        Event event= eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие не найдено"));
        UserShortDto initiator = getUserShortDto(event.getInitiatorId());
        return EventMapper.toEventDto(event,initiator);
    }

    private void validateSearchParameters(List<Long> users, List<String> states,
                                          LocalDateTime rangeStart, LocalDateTime rangeEnd) {
        if (rangeStart != null && rangeEnd != null && rangeStart.isAfter(rangeEnd)) {
            throw new ValidationException("Дата начала не может быть позже даты окончания");
        }

        if (users != null && !users.isEmpty()) {
            List<Long> existingUsers = userFeignClient.findExistingUserIds(users);
            if (existingUsers.size() != users.size()) {
                throw new NotFoundException("Некоторые пользователи не найдены");
            }
        }
    }

    private List<EventState> parseEventStates(List<String> states) {
        if (states == null || states.isEmpty()) {
            return null;
        }

        return states.stream()
                .map(stateStr -> {
                    try {
                        return EventState.valueOf(stateStr.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        throw new ValidationException("Некорректное состояние события: " + stateStr);
                    }
                })
                .collect(Collectors.toList());
    }

    private LocalDateTime parseDateTime(String dateTimeStr) {
        if (dateTimeStr == null) {
            return null;
        }
        try {
            return LocalDateTime.parse(dateTimeStr,
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } catch (DateTimeParseException e) {
            throw new ValidationException("Некорректный формат даты: " + dateTimeStr +
                    ". Ожидается формат: yyyy-MM-dd HH:mm:ss");
        }
    }

    private void validateEventForAdminUpdate(Event event, EventAdminUpdateDto updateRequest) {
        LocalDateTime now = LocalDateTime.now();

        LocalDateTime eventDateToCheck = updateRequest.getEventDate() != null
                ? updateRequest.getEventDate()
                : event.getEventDate();

        if (updateRequest.getStateAction() == AdminUpdateStateAction.PUBLISH_EVENT) {
            if (event.getState() != EventState.PENDING) {
                throw new ConflictException(
                        "Событие можно публиковать только, если оно в состоянии ожидания публикации. " +
                                "Текущее состояние: " + event.getState()
                );
            }

            if (eventDateToCheck.isBefore(now.plusHours(1))) {
                throw new ValidationException("Дата начала события должна быть не ранее чем за час от даты публикации. " +
                        "Дата события: " + eventDateToCheck + ", Текущее время: " + now
                );
            }
        }

        if (updateRequest.getEventDate() != null) {
            if (event.getState() == EventState.PUBLISHED) {
                if (updateRequest.getEventDate().isBefore(now.plusHours(1))) {
                    throw new ValidationException("Дата начала изменяемого события должна быть не ранее чем за час от даты публикации"
                    );
                }
            } else {
                if (updateRequest.getEventDate().isBefore(now.plusHours(2))) {
                    throw new ValidationException("Дата и время события должны быть не ранее чем через 2 часа от текущего момента"
                    );
                }
            }
        }

        if (updateRequest.getStateAction() == AdminUpdateStateAction.REJECT_EVENT) {
            if (event.getState() == EventState.PUBLISHED) {
                throw new ConflictException("Событие можно отклонить только, если оно еще не опубликовано");
            }
        }
    }


    private void updateEventFields(Event event, EventAdminUpdateDto updateRequest) {
        if (updateRequest.getAnnotation() != null && !updateRequest.getAnnotation().isBlank()) {
            event.setAnnotation(updateRequest.getAnnotation());
        }

        if (updateRequest.getCategory() != null) {
            Category category = categoryRepository.findById(updateRequest.getCategory())
                    .orElseThrow(() -> new NotFoundException("Категория с id = " + updateRequest.getCategory() + " не найдена"));
            event.setCategory(category);
        }

        if (updateRequest.getDescription() != null && !updateRequest.getDescription().isBlank()) {
            event.setDescription(updateRequest.getDescription());
        }

        if (updateRequest.getEventDate() != null) {
            event.setEventDate(updateRequest.getEventDate());
        }

        if (updateRequest.getLocation() != null) {
            Location location = new Location();
            location.setLat(updateRequest.getLocation().getLat());
            location.setLon(updateRequest.getLocation().getLon());
            event.setLocation(location);
        }

        if (updateRequest.getPaid() != null) {
            event.setPaid(updateRequest.getPaid());
        }

        if (updateRequest.getParticipantLimit() != null) {
            event.setParticipantLimit(updateRequest.getParticipantLimit());
        }

        if (updateRequest.getRequestModeration() != null) {
            event.setRequestModeration(updateRequest.getRequestModeration());
        }

        if (updateRequest.getTitle() != null && !updateRequest.getTitle().isBlank()) {
            event.setTitle(updateRequest.getTitle());
        }
    }

    private void processAdminStateAction(Event event, AdminUpdateStateAction stateAction) {
        switch (stateAction) {
            case PUBLISH_EVENT:
                event.setState(EventState.PUBLISHED);
                event.setPublishedOn(LocalDateTime.now());
                log.info("Событие с id = {} опубликовано", event.getId());
                break;

            case REJECT_EVENT:
                event.setState(EventState.CANCELED);
                log.info("Событие с id = {} отклонено администратором", event.getId());
                break;

            default:
                throw new ValidationException("Некорректное действие: " + stateAction);
        }
    }

    @Override
    public List<ParticipationRequestDto> getUserEventRequests(Long userId, Long eventId) {
        if (eventId == null || userId == null) {
            throw new ValidationException("Id должен быть указан");
        }
        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId).orElseThrow(
                () -> new NotFoundException("Событие или пользователь с данным id не найдены, или событие недоступно к просмотру данным пользователем")
        );
        return requestFeignClient.getRequestsByEventId(event.getId());
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

    private boolean isPreModerationOff(boolean moderationStatus, int limit) {
        return !moderationStatus || limit == 0;
    }
}
