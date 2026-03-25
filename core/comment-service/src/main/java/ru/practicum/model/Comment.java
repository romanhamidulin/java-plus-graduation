package ru.practicum.model;

import jakarta.persistence.*;
import lombok.*;
import ru.practicum.enums.comment.CommentStatus;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "comments")
public class Comment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(length = 1000)
    private String text;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @JoinColumn(name = "author_id")
    private Long author;

    private LocalDateTime created = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    private CommentStatus status = CommentStatus.PENDING;
}
