package ru.practicum.model;

import jakarta.persistence.*;
import lombok.*;
import ru.practicum.enums.request.RequestStatus;

import java.time.LocalDateTime;

@RequiredArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
@Entity
@Table(name = "requests")
public class Request {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JoinColumn(name = "requester_id")
    private Long requesterId;

    @JoinColumn(name = "event_id")
    private Long eventId;

    @Column(nullable = false, length = 120)
    @Enumerated(EnumType.ORDINAL)
    private RequestStatus status;

    @Column(name = "created_at")
    private LocalDateTime createdOn;
}
