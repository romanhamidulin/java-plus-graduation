package ru.practicum.feign.fallback;

import org.springframework.stereotype.Component;
import ru.practicum.dto.comment.CommentDto;
import ru.practicum.enums.comment.CommentStatus;
import ru.practicum.exception.ServiceUnavailableException;
import ru.practicum.feign.api.CommentInternalApi;

import java.util.List;
import java.util.Set;

@Component
public class CommentFallbackClient implements CommentInternalApi {

    private static final String SERVICE_NAME = "comment-service";

    @Override
    public List<CommentDto> getLastCommentsForEvents(Set<Long> eventsId) {
        throw new ServiceUnavailableException(SERVICE_NAME);
    }

    @Override
    public List<CommentDto> getCommentsByEventId(Long eventId) {
        throw new ServiceUnavailableException(SERVICE_NAME);
    }

    @Override
    public List<CommentDto> getCommentsByEventIdAndStatus(Long eventId, CommentStatus status) {
        throw new ServiceUnavailableException(SERVICE_NAME);
    }

    @Override
    public long getCommentsCountByEventIdAndStatus(Long eventId, CommentStatus status) {
        throw new ServiceUnavailableException(SERVICE_NAME);
    }
}
