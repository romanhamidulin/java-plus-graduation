package ru.practicum.dto.events;

import ru.practicum.dto.comment.CommentDto;

import java.util.List;

public interface ResponseEvent {
    void setConfirmedRequests(int confirmedRequests);

    int getConfirmedRequests();

    void setViews(long views);

    long getViews();

    List<CommentDto> getComments();

    void setComments(List<CommentDto> comments);
}
