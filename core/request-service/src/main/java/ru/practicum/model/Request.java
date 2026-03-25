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

    @JoinColumn(name = "event_id")
    private Long eventId;

    @JoinColumn(name = "requester_id")
    private Long requester;

    @Column(name = "created_at")
    private LocalDateTime createdOn;

    @Enumerated(EnumType.STRING)
    private RequestStatus status;
}
