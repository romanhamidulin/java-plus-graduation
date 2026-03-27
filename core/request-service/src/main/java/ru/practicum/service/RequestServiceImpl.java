package ru.practicum.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.dto.events.EventFullDto;
import ru.practicum.dto.request.ConfirmedRequestsDto;
import ru.practicum.dto.user.UserDto;
import ru.practicum.enums.event.EventState;
import ru.practicum.exception.*;
import ru.practicum.dto.request.ParticipationRequestDto;
import ru.practicum.feign.client.EventFeignClient;
import ru.practicum.feign.client.UserFeignClient;
import ru.practicum.mapper.RequestMapper;
import ru.practicum.model.ConfirmedRequests;
import ru.practicum.model.Request;
import ru.practicum.enums.request.RequestStatus;
import ru.practicum.repository.RequestRepository;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class RequestServiceImpl implements RequestService {
    private final RequestRepository requestRepository;
    private final EventFeignClient eventFeignClient;
    private final UserFeignClient userFeignClient;
    private final RequestMapper requestMapper;

    @Override
    public List<ParticipationRequestDto> getRequestsByRequester(Long userId) {
        List<Request> requests = requestRepository.findAllByRequesterId(userId);
        return requests.stream().map(requestMapper::toParticipationRequestDto).toList();
    }

    @Override
    @Transactional
    public ParticipationRequestDto addRequest(Long userId, Long eventId) {

        EventFullDto event = eventFeignClient.getEventById(eventId).orElseThrow(() -> new NotFoundException(String.format("Событие с ID %s не найдено", eventId))).getBody();

        UserDto user = userFeignClient.getUserById(userId).orElseThrow(() -> new NotFoundException(String.format("Пользователь с ID %s не найден", userId)));

        if (requestRepository.existsByEventIdAndRequesterId(eventId, userId)) {
            throw new DuplicateDataException("Запрос на такое событие уже есть");
        }

        if (!event.getState().equals(EventState.PUBLISHED)) {
            throw new ConflictException("Невозможно создать запрос на неопубликованное событие");
        }


        if (event.getParticipantLimit() != 0 && requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED)
                >= event.getParticipantLimit()) {
            throw new ConflictException("Достигнут лимит запросов на событие");
        }

        if (event.getInitiator().getId().equals(userId)) {
            throw new ConflictException("Невозможно создать запрос будучи инициатором события");
        }

        boolean isPreModerationOn = isPreModerationOn(event.getRequestModeration(), event.getParticipantLimit());
        Request request = new Request(
                null,
                userId,
                eventId,
                isPreModerationOn ? RequestStatus.PENDING : RequestStatus.CONFIRMED,
                LocalDateTime.now()
        );

        request = requestRepository.save(request);

        return requestMapper.toParticipationRequestDto(request);
    }

    @Override
    @Transactional
    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {
        Request request = requestRepository.findById(requestId).orElseThrow(() ->
                new NotFoundException("Запрос не найден"));

        userFeignClient.getUserById(userId).orElseThrow(() -> new NotFoundException(String.format("Пользователь с ID %s не найден", userId)));

        if (!request.getStatus().equals(RequestStatus.PENDING)) {
            throw new ConflictException("Нельзя отменить заявку, т.к. ее статус не PENDING");
        }

        request.setStatus(RequestStatus.CANCELED);

        request = requestRepository.save(request);

        return requestMapper.toParticipationRequestDto(request);
    }

    @Override
    public List<ParticipationRequestDto> getRequestsByEventId(Long eventId) {
        List<Request> requests = requestRepository.findAllByEventId(eventId);
        return requests.stream().map(requestMapper::toParticipationRequestDto).toList();
    }

    @Override
    public int getRequestsCountByEventIdAndStatus(Long eventId, RequestStatus status) {
        return requestRepository.countByEventIdAndStatus(eventId, status);
    }

    @Override
    public List<ConfirmedRequestsDto> getConfirmedRequestsByEventId(Collection<Long> eventsId) {
        List<ConfirmedRequests> participationRequests =
                requestRepository.getConfirmedRequests(eventsId, RequestStatus.CONFIRMED);

        return participationRequests.stream()
                .map(requestMapper::toConfirmedRequestsDto)
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
        return requestMapper.toParticipationRequestDto(requestRepository.save(request));
    }

    private boolean isPreModerationOn(boolean moderationStatus, int limit) {
        return moderationStatus && limit != 0;
    }
}
