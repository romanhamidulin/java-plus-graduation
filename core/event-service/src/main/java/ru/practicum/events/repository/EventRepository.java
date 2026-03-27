package ru.practicum.events.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import ru.practicum.events.model.Event;
import ru.practicum.enums.event.EventState;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface EventRepository extends JpaRepository<Event, Long>, QuerydslPredicateExecutor<Event> {
    List<Event> findByInitiatorId(Long userId, Pageable pageable);

    Optional<Event> findByIdAndState(Long eventId, EventState state);

    Set<Event> findByIdIn(Set<Long> events);

    Optional<Event> findByIdAndInitiatorId(Long eventId, Long userId);

    boolean existsByCategory_Id(Long catId);
}


