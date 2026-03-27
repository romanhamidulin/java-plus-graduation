package ru.practicum.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import ru.practicum.dto.comment.CommentDto;
import ru.practicum.dto.comment.NewCommentDto;
import ru.practicum.model.Comment;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface CommentMapper {
    Comment toComment(NewCommentDto commentDto);

    @Mapping(target = "eventId", expression = "java(comment.getEventId())")
    @Mapping(target = "status", expression = "java(comment.getStatus().name())")
    CommentDto toCommentDto(Comment comment);

}
