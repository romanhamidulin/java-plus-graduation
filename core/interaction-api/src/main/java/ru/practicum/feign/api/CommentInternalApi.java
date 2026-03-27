package ru.practicum.feign.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.dto.comment.CommentDto;

import java.util.List;
import java.util.Set;

public interface CommentInternalApi {
    @GetMapping("/event/last")
    List<CommentDto> getLastCommentsForEvents(@RequestParam(name = "ids") Set<Long> eventsId);

    @GetMapping("/event/{eventId}")
    List<CommentDto> getCommentsByEventId(@PathVariable Long eventId);
}
