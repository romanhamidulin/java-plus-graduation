package ru.practicum.mapper;

import lombok.experimental.UtilityClass;
import ru.practicum.dto.comment.CommentDto;
import ru.practicum.dto.comment.NewCommentDto;
import ru.practicum.model.Comment;
import ru.practicum.enums.comment.CommentStatus;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@UtilityClass
public class CommentMapper {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public Comment toComment(NewCommentDto newCommentDto, Long author, Long event) {
        return Comment.builder()
                .text(newCommentDto.getText())
                .author(author)
                .eventId(event)
                .created(LocalDateTime.now())
                .status(CommentStatus.PENDING)
                .build();
    }

    public CommentDto toDto(Comment comment) {
        return CommentDto.builder()
                .id(comment.getId())
                .text(comment.getText())
                .eventId(comment.getEventId() != null ? comment.getEventId() : null)
                .authorId(comment.getAuthor() != null ? comment.getAuthor() : null)
                .created(comment.getCreated().format(FORMATTER))
                .status(comment.getStatus().name())
                .build();
    }
}
