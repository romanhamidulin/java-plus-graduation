package ru.practicum.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.query.Param;
import ru.practicum.model.ConfirmedRequests;
import ru.practicum.model.Request;
import ru.practicum.enums.request.RequestStatus;

import java.util.Collection;
import java.util.List;

public interface RequestRepository extends JpaRepository<Request, Long>, QuerydslPredicateExecutor<Request> {
    List<Request> findAllByRequesterId(Long requesterId);

    boolean existsByEventIdAndRequesterId(Long eventId, Long requesterId);

    List<Request> findAllByEventId(Long eventId);

    int countByEventIdAndStatus(Long eventId, RequestStatus requestStatus);

    @Query("""
            SELECT new ru.practicum.model.ConfirmedRequests(r.eventId, CAST(COUNT(r.requesterId) AS INTEGER))
            FROM Request r
            WHERE r.eventId IN :eventIds AND r.status = :status
            GROUP BY r.eventId
            """)
    List<ConfirmedRequests> getConfirmedRequests(@Param("eventIds") Collection<Long> eventIds,
                                                 @Param("status") RequestStatus status);
}
