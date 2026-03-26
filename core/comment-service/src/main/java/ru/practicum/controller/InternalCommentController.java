package ru.practicum.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.comment.CommentDto;
import ru.practicum.enums.comment.CommentStatus;
import ru.practicum.feign.api.CommentInternalApi;
import ru.practicum.mapper.CommentMapper;
import ru.practicum.repository.CommentRepository;
import ru.practicum.service.CommentService;

import java.util.List;
import java.util.Set;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/comments")
public class InternalCommentController implements CommentInternalApi {

    private final CommentService commentService;
    private final CommentRepository commentRepository;

    @GetMapping("/event/last")
    public List<CommentDto> getLastCommentsForEvents(@RequestParam(name = "ids") Set<Long> eventsId) {
        log.info("Пришел GET запрос на /comments/events/last с телом: {}", eventsId);
        List<CommentDto> comments = commentService.getCommentsByEventsIds(eventsId);
        log.info("Для мероприятий с id ({}) найдены следующие комментарии: {}", eventsId, comments);
        return comments;
    }

    @GetMapping("/event/{eventId}")
    public List<CommentDto> getCommentsByEventId(@PathVariable Long eventId) {
        log.info("Пришел GET запрос на /comments/event/{}", eventId);
        List<CommentDto> comments = commentService.getEventComments(eventId, 0, Integer.MAX_VALUE);
        log.info("Для мероприятия с id {} нашлись комментарии: {}", eventId, comments);
        return comments;
    }

    @GetMapping("/event/{eventId}")
    public List<CommentDto> getCommentsByEventIdAndStatus(
            @PathVariable("eventId") Long eventId,
            @RequestParam("status") CommentStatus status) {

        return commentRepository.findByEventIdAndStatus(eventId, status)
                .stream()
                .map(CommentMapper::toDto)
                .toList();
    }

    @GetMapping("/event/{eventId}/count")
    public long getCommentsCountByEventIdAndStatus(
            @PathVariable("eventId") Long eventId,
            @RequestParam("status") CommentStatus status) {

        return commentRepository.countByEventIdAndStatus(eventId, status);
    }
}
