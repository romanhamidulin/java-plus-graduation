package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.events.EventDto;
import ru.practicum.dto.request.ConfirmedRequests;
import ru.practicum.dto.user.UserDto;
import ru.practicum.enums.event.EventState;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.exception.RequestConflictException;
import ru.practicum.exception.ValidationException;
import ru.practicum.dto.request.EventRequestStatusUpdateRequest;
import ru.practicum.dto.request.EventRequestStatusUpdateResult;
import ru.practicum.dto.request.ParticipationRequestDto;
import ru.practicum.feign.client.EventFeignClient;
import ru.practicum.feign.client.UserFeignClient;
import ru.practicum.mapper.RequestMapper;
import ru.practicum.model.Request;
import ru.practicum.enums.request.RequestStatus;
import ru.practicum.repository.RequestRepository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RequestServiceImpl implements RequestService {
    private final RequestRepository requestRepository;
    private final EventFeignClient eventFeignClient;
    private final UserFeignClient userFeignClient;

    @Override
    public List<ParticipationRequestDto> getUserEventRequests(Long userId, Long eventId) {
        if (eventId == null || userId == null) {
            throw new ValidationException("Id должен быть указан");
        }
        EventDto event = eventFeignClient.findByIdAndInitiatorId(eventId, userId).orElseThrow(
                () -> new NotFoundException("Событие или пользователь с данным id не найдены, или событие недоступно к просмотру данным пользователем")
        );
        List<Request> requests = requestRepository.findAllByEventId(eventId);
        return requests.stream()
                .map(RequestMapper::toParticipationRequestDto)
                .toList();
    }

    @Override
    @Transactional
    public EventRequestStatusUpdateResult updateUserEventRequests(Long userId, Long eventId, EventRequestStatusUpdateRequest dto) {
        if (eventId == null || userId == null) {
            throw new ValidationException("Id должен быть указан");
        }

        EventDto event = eventFeignClient.findByIdAndInitiatorId(eventId, userId).orElseThrow(
                () -> new NotFoundException("Событие или пользователь с данным id не найдены, или событие недоступно к просмотру данным пользователем")
        );

        if (event.getParticipantLimit() == 0 || !event.getRequestModeration()) {
            log.info("Если для события лимит заявок равен 0 или отключена пре-модерация заявок, то подтверждение заявок не требуется");
            return new EventRequestStatusUpdateResult(List.of(), List.of());
        }

        Long confirmedRequests = requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);

        if (event.getParticipantLimit() > 0 && confirmedRequests >= event.getParticipantLimit()) {
            throw new RequestConflictException("Уже достигнут лимит по заявкам на данное событие");
        }

        List<Request> requests = requestRepository.findAllByIdIn(dto.getRequestIds());

        if (!requests.stream()
                .map(Request::getStatus)
                .allMatch(RequestStatus.PENDING::equals)) {
            throw new RequestConflictException("Статус можно изменить только у заявок, находящихся в состоянии ожидания");
        }

        List<ParticipationRequestDto> confirmed = new ArrayList<>();
        List<ParticipationRequestDto> rejected = new ArrayList<>();

        if (dto.getStatus().equals(RequestStatus.REJECTED)) {
            requests.forEach(request -> request.setStatus(RequestStatus.REJECTED));
            List<Request> updatedRequests = requestRepository.saveAll(requests);
            rejected = updatedRequests.stream()
                    .map(RequestMapper::toParticipationRequestDto)
                    .toList();
            return new EventRequestStatusUpdateResult(confirmed, rejected);
        }

        int limit = event.getParticipantLimit() == 0 ? Integer.MAX_VALUE : event.getParticipantLimit();
        int availableSlots = limit - confirmedRequests.intValue();

        for (int i = 0; i < requests.size(); i++) {
            Request request = requests.get(i);
            if (i < availableSlots) {
                request.setStatus(RequestStatus.CONFIRMED);
                confirmed.add(RequestMapper.toParticipationRequestDto(request));
            } else {
                request.setStatus(RequestStatus.REJECTED);
                rejected.add(RequestMapper.toParticipationRequestDto(request));
            }
        }

        List<Request> updatedRequests = requestRepository.saveAll(requests);

        confirmed.clear();
        rejected.clear();
        for (Request request : updatedRequests) {
            ParticipationRequestDto dtoResult = RequestMapper.toParticipationRequestDto(request);
            if (request.getStatus() == RequestStatus.CONFIRMED) {
                confirmed.add(dtoResult);
            } else if (request.getStatus() == RequestStatus.REJECTED) {
                rejected.add(dtoResult);
            }
        }

        return new EventRequestStatusUpdateResult(confirmed, rejected);
    }

    @Override
    public List<ParticipationRequestDto> getRequestsByRequester(Long userId) {
        UserDto user = userFeignClient.getUserById(userId).orElseThrow(
                () -> new NotFoundException("Пользователь с данным id не найден")
        );
        List<Request> requests = requestRepository.findAllByRequesterId(userId);
        return requests.stream()
                .map(RequestMapper::toParticipationRequestDto)
                .toList();
    }

    @Override
    @Transactional
    public ParticipationRequestDto addRequest(Long userId, Long eventId) {
        EventDto event = eventFeignClient.getEventById(eventId).orElseThrow(
                () -> new NotFoundException("Событие с данным id не найдено")
        );
        UserDto user = userFeignClient.getUserById(userId).orElseThrow(
                () -> new NotFoundException("Пользователь с данным id не найден")
        );

        if (requestRepository.existsByRequesterIdAndEventId(userId, eventId)) {
            throw new RequestConflictException("Запрос на участие в данном событии уже создан");
        }

        if (userId.equals(event.getInitiator().getId())) {
            throw new RequestConflictException("Инициатор события не может добавить запрос на участие в своём событии");
        }

        if (!event.getState().equals(EventState.PUBLISHED)) {
            throw new RequestConflictException("Нельзя участвовать в неопубликованном событии");
        }

        if (event.getParticipantLimit() > 0) {
            Long confirmedRequests = requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);

            if (confirmedRequests >= event.getParticipantLimit()) {
                throw new RequestConflictException("Уже достигнут лимит по заявкам на данное событие");
            }
        }

        Request request = new Request();
        request.setEventId(event.getId());
        request.setRequesterId(user.getId());
        request.setCreatedOn(LocalDateTime.now());

        if (!event.getRequestModeration() || event.getParticipantLimit() == 0) {
            request.setStatus(RequestStatus.CONFIRMED);
        } else {
            request.setStatus(RequestStatus.PENDING);
        }

        request = requestRepository.save(request);
        requestRepository.flush();
        return RequestMapper.toParticipationRequestDto(request);
    }

    @Override
    @Transactional
    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {
        Request request = requestRepository.findByIdAndRequesterId(requestId, userId)
                .orElseThrow(() -> new NotFoundException("Запрос не найден или недоступен данному пользователю"));
        request.setStatus(RequestStatus.CANCELED);
        request = requestRepository.save(request);
        requestRepository.flush();
        return RequestMapper.toParticipationRequestDto(request);
    }

    @Override
    public List<ParticipationRequestDto> getRequestsByEventId(Long eventId) {
        List<Request> requests = requestRepository.findAllByEventId(eventId);
        return requests.stream().map(RequestMapper::toParticipationRequestDto).toList();
    }

    @Override
    public int getRequestsCountByEventIdAndStatus(Long eventId, RequestStatus status) {
        return requestRepository.findAllByStatusAndEventId(status, eventId).size();
    }

    @Override
    public List<ConfirmedRequests> getConfirmedRequestsByEventId(List<Long> eventsId) {
        List<ConfirmedRequests> participationRequests =
                requestRepository.countConfirmedRequestsByEvents(RequestStatus.CONFIRMED, eventsId);

        return participationRequests.stream()
                .map(RequestMapper::toConfirmedRequestsDto)
                .toList();
    }

    @Override
    public ParticipationRequestDto changeRequestStatus(Long requestId, RequestStatus status) {
        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Запрос не найден"));
        if (request.getStatus() != RequestStatus.PENDING) {
            throw new ConflictException("Чтобы поменять статус заявки, она должна быть в статусе PENDING");
        }
        request.setStatus(status);
        return RequestMapper.toParticipationRequestDto(requestRepository.save(request));
    }

    @Override
    public Optional<ParticipationRequestDto> findByRequesterIdAndEventIdAndStatus(
            long requesterId, long eventId, RequestStatus status) {

        log.debug("Finding request by requesterId: {}, eventId: {}, status: {}", requesterId, eventId, status);

        return requestRepository.findByRequesterIdAndEventIdAndStatus(requesterId, eventId, status)
                .map(RequestMapper::toParticipationRequestDto);
    }

    @Override
    public List<ParticipationRequestDto> findAllByStatusAndEventId(RequestStatus status, Long eventId) {
        log.debug("Finding all requests by status: {} and eventId: {}", status, eventId);

        return requestRepository.findAllByStatusAndEventId(status, eventId)
                .stream()
                .map(RequestMapper::toParticipationRequestDto)
                .collect(Collectors.toList());
    }

    @Override
    public Map<Long, Long> getConfirmedRequestsCountsForEvents(List<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return Map.of();
        }

        return requestRepository.findConfirmedRequestsCountsByEventIds(eventIds)
                .stream()
                .collect(Collectors.toMap(
                        ConfirmedRequests::getEvent,
                        ConfirmedRequests::getCount
                ));
    }
}
