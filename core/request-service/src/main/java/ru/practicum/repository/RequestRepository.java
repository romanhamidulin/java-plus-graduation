package ru.practicum.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.query.Param;
import ru.practicum.dto.request.ConfirmedRequests;
import ru.practicum.model.Request;
import ru.practicum.enums.request.RequestStatus;

import java.util.List;
import java.util.Optional;

public interface RequestRepository extends JpaRepository<Request, Long>, QuerydslPredicateExecutor<Request> {
    @Query("""
        SELECT new ru.practicum.dto.request.ConfirmedRequests(COUNT(DISTINCT r.id), r.eventId)
        FROM Request AS r
        WHERE r.eventId IN (:ids)
        AND r.status = :status
        GROUP BY r.eventId
        """)
    List<ConfirmedRequests> findAllByEventIdInAndStatus(@Param("ids") List<Long> ids,
                                                        @Param("status") RequestStatus status);

    List<Request> findAllByStatusAndEvent_Id(RequestStatus status, Long eventId);

    Long countByEventIdAndStatus(Long eventId, RequestStatus status);

    @Query("SELECT new ru.practicum.dto.request.ConfirmedRequests(COUNT(r), r.eventId) " +
            "FROM Request r " +
            "WHERE r.status = :status AND r.eventId IN :eventIds " +
            "GROUP BY r.eventId")
    List<ConfirmedRequests> countConfirmedRequestsByEvents(@Param("status") RequestStatus status,
                                                           @Param("eventIds") List<Long> eventIds);



    List<Request> findAllByEventId(Long eventId);

    List<Request> findAllByIdIn(List<Long> ids);

    List<Request> findAllByRequesterId(Long requesterId);

    Optional<Request> findByIdAndRequesterId(Long requestId, Long requesterId);

    Boolean existsByRequesterIdAndEventId(Long userId, Long eventId);

    Optional<Request> findByRequesterIdAndEventIdAndStatus(
            Long requesterId, Long eventId, RequestStatus status);
}
