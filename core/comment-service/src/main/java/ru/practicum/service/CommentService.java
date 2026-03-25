package ru.practicum.service;

import ru.practicum.dto.comment.AdminUpdateCommentStatusDto;
import ru.practicum.dto.comment.CommentDto;
import ru.practicum.dto.comment.NewCommentDto;

import java.util.List;
import java.util.Set;

public interface CommentService {
    CommentDto createComment(long userId, long eventId, NewCommentDto newCommentDto);

    CommentDto updateComment(long userId, long commentId, NewCommentDto updateCommentDto);

    void deleteComment(long userId, long commentId);

    void adminDeleteComment(long commentId);

    CommentDto adminUpdateCommentStatus(Long commentId, AdminUpdateCommentStatusDto dto);

    List<CommentDto> adminPendigCommentList(List<Long> usersId);

    List<CommentDto> getCommentsByEventsIds(Set<Long> eventsId);

    List<CommentDto> getEventComments(Long eventId, Integer from, Integer size);

}
