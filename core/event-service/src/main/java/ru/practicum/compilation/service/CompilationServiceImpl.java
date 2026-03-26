package ru.practicum.compilation.service;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.compilation.CompilationDto;
import ru.practicum.dto.compilation.NewCompilationDto;
import ru.practicum.dto.compilation.UpdateCompilationRequest;
import ru.practicum.dto.events.EventShortDto;
import ru.practicum.dto.user.UserShortDto;
import ru.practicum.compilation.mapper.CompilationMapper;
import ru.practicum.compilation.model.Compilation;
import ru.practicum.compilation.repository.CompilationRepository;
import ru.practicum.events.mapper.EventMapper;
import ru.practicum.events.model.Event;
import ru.practicum.events.repository.EventRepository;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.feign.client.UserFeignClient;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompilationServiceImpl implements CompilationService {

    private final CompilationRepository compilationRepository;
    private final EventRepository eventRepository;
    private final UserFeignClient userFeignClient;  // Добавляем UserFeignClient

    @Override
    public List<CompilationDto> getCompilations(Boolean pinned, Integer from, Integer size) {
        int page = from / size;
        Pageable pageable = PageRequest.of(page, size);

        List<Compilation> compilations;
        if (pinned != null) {
            compilations = compilationRepository.findByPinned(pinned, pageable);
        } else {
            compilations = compilationRepository.findAll(pageable).getContent();
        }

        return compilations.stream()
                .map(this::toDtoWithEvents)
                .collect(Collectors.toList());
    }

    @Override
    public CompilationDto getCompilationById(Long compId) {
        Compilation compilation = compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException("Подборка с id = " + compId + " не найдена"));

        return toDtoWithEvents(compilation);
    }

    @Override
    @Transactional
    public CompilationDto createCompilation(NewCompilationDto newCompilationDto) {
        if (compilationRepository.existsByTitle(newCompilationDto.getTitle())) {
            throw new ConflictException("Подборка с наименованием " + newCompilationDto.getTitle() + " уже существует");
        }
        Compilation compilation = CompilationMapper.toEntity(newCompilationDto);

        if (newCompilationDto.getEvents() != null && !newCompilationDto.getEvents().isEmpty()) {
            Set<Event> events = new HashSet<>(eventRepository.findAllById(newCompilationDto.getEvents()));
            compilation.setEvents(events);
        }

        Compilation savedCompilation = compilationRepository.save(compilation);
        return toDtoWithEvents(savedCompilation);
    }

    @Override
    @Transactional
    public void deleteCompilation(Long compId) {
        try {
            compilationRepository.deleteById(compId);
        } catch (EmptyResultDataAccessException e) {
            throw new NotFoundException("Подборка с id = " + compId + " не найдена");
        }
    }

    @Override
    @Transactional
    public CompilationDto updateCompilation(Long compId, UpdateCompilationRequest updateCompilationRequest) {
        Compilation compilation = compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException("Подборка с id = " + compId + " не найдена"));

        if (updateCompilationRequest.getTitle() != null && !updateCompilationRequest.getTitle().isBlank()) {
            if (!compilation.getTitle().equals(updateCompilationRequest.getTitle()) &&
                    compilationRepository.existsByTitle(updateCompilationRequest.getTitle())) {
                throw new ConflictException("Подборка с наименованием " + updateCompilationRequest.getTitle() + " уже существует");
            }
            compilation.setTitle(updateCompilationRequest.getTitle());
        }

        if (updateCompilationRequest.getPinned() != null) {
            compilation.setPinned(updateCompilationRequest.getPinned());
        }

        if (updateCompilationRequest.getEvents() != null) {
            if (updateCompilationRequest.getEvents().isEmpty()) {
                compilation.getEvents().clear();
            } else {
                List<Event> foundEvents = eventRepository.findAllById(updateCompilationRequest.getEvents());

                if (foundEvents.size() != updateCompilationRequest.getEvents().size()) {
                    throw new NotFoundException("Некоторые события не найдены");
                }
                compilation.setEvents(new HashSet<>(foundEvents));
            }
        }

        return toDtoWithEvents(compilation);
    }

    private CompilationDto toDtoWithEvents(Compilation compilation) {
        List<EventShortDto> eventShortDtos = new ArrayList<>();

        if (compilation.getEvents() != null && !compilation.getEvents().isEmpty()) {
            eventShortDtos = compilation.getEvents().stream()
                    .map(this::mapEventToShortDto)
                    .collect(Collectors.toList());
        }

        return CompilationMapper.toDto(compilation, eventShortDtos);
    }

    private EventShortDto mapEventToShortDto(Event event) {
        UserShortDto initiator = getUserShortDto(event.getInitiatorId());
        return EventMapper.toEventShortDto(event, initiator);
    }

    private UserShortDto getUserShortDto(Long userId) {
        try {
            return userFeignClient.getUserByIdShort(userId);
        } catch (Exception e) {
            return null;
        }
    }
}