package ru.practicum.compilation.service;

import com.querydsl.core.types.dsl.BooleanExpression;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import ru.practicum.compilation.model.GetCompilationsParam;
import ru.practicum.compilation.model.QCompilation;
import ru.practicum.events.model.QEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.compilation.CompilationDto;
import ru.practicum.dto.compilation.NewCompilationDto;
import ru.practicum.dto.compilation.UpdateCompilationRequest;
import ru.practicum.compilation.mapper.CompilationMapper;
import ru.practicum.compilation.model.Compilation;
import ru.practicum.compilation.repository.CompilationRepository;
import ru.practicum.events.model.Event;
import ru.practicum.events.repository.EventRepository;
import ru.practicum.exception.NotFoundException;

import java.util.*;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompilationServiceImpl implements CompilationService {

    private final CompilationRepository compilationRepository;
    private final EventRepository eventRepository;
    private final CompilationMapper compilationMapper;

    @Override
    public List<CompilationDto> getCompilations(GetCompilationsParam param) {
        QCompilation qCompilation = QCompilation.compilation;
        BooleanExpression pinnedExpression = qCompilation.pinned.eq(param.getPinned());

        List<Compilation> compilations =
                compilationRepository.findAll(pinnedExpression, param.getPageable()).getContent();
        return compilations.stream()
                .map(compilationMapper::toCompilationDto)
                .toList();
    }

    @Override
    public CompilationDto getCompilationById(Long compId) {
        Compilation compilation = compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException("Подборка с id = " + compId + " не найдена"));

        return compilationMapper.toCompilationDto(compilation);
    }

    @Override
    @Transactional
    public CompilationDto createCompilation(NewCompilationDto newCompilationDto) {
        Compilation compilation = compilationMapper.toCompilation(newCompilationDto);
        Set<Event> events = eventRepository.findByIdIn(newCompilationDto.getEvents());
        compilation.setEvents(events);
        compilationRepository.save(compilation);
        return compilationMapper.toCompilationDto(compilation);
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
                .orElseThrow(() -> new NotFoundException("Подборка с eventId = " + compId + " не найдена"));

        if (updateCompilationRequest.hasEvents()) {
            QEvent qEvent = QEvent.event;
            BooleanExpression idsExpression = qEvent.id.in(updateCompilationRequest.getEvents());
            Iterable<Event> eventsInDb = eventRepository.findAll(idsExpression);

            long sizeEventsInDb = Stream.of(eventsInDb).count();

            if (updateCompilationRequest.getEvents().size() != sizeEventsInDb) {
                throw new NotFoundException("Одно или более событий включенных в подборку не существует");
            }

            compilation.getEvents().clear();
            eventsInDb.forEach(compilation.getEvents()::add);
        }

        if (updateCompilationRequest.hasTitle()
                && !compilation.getTitle().equals(updateCompilationRequest.getTitle())) {
            compilation.setTitle(updateCompilationRequest.getTitle());
        }

        if (updateCompilationRequest.hasPinned()
                && !compilation.getPinned().equals(updateCompilationRequest.getPinned())) {
            compilation.setPinned(updateCompilationRequest.getPinned());
        }
        return compilationMapper.toCompilationDto(compilation);
    }
}