package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.comment.AdminUpdateCommentStatusDto;
import ru.practicum.dto.comment.CommentDto;
import ru.practicum.dto.comment.NewCommentDto;
import ru.practicum.dto.events.EventDto;
import ru.practicum.dto.request.ParticipationRequestDto;
import ru.practicum.dto.user.UserDto;
import ru.practicum.enums.event.EventState;
import ru.practicum.enums.request.RequestStatus;
import ru.practicum.feign.client.EventFeignClient;
import ru.practicum.feign.client.RequestFeignClient;
import ru.practicum.feign.client.UserFeignClient;
import ru.practicum.mapper.CommentMapper;
import ru.practicum.enums.comment.AdminUpdateCommentStatusAction;
import ru.practicum.model.Comment;
import ru.practicum.enums.comment.CommentStatus;
import ru.practicum.repository.CommentRepository;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentServiceImpl implements CommentService {
    private final CommentRepository commentRepository;
    private final UserFeignClient userFeignClient;
    private final EventFeignClient eventFeignClient;
    private final RequestFeignClient requestFeignClient;

    @Transactional
    @Override
    public CommentDto createComment(long authorId, long eventId, NewCommentDto newCommentDto) {
        UserDto author = userFeignClient.getUserById(authorId)
                .orElseThrow(() -> new NotFoundException(String.format("Пользователь с ID %s не найден", authorId)));
        EventDto event = eventFeignClient.getEventById(eventId)
                .orElseThrow(() -> new NotFoundException(String.format("Событие с ID %s не найдено", eventId)));
        if (authorId == event.getInitiator().getId()) {
            throw new ConflictException("Инициатор мероприятия не может оставлять комментарии к нему");
        }
        if (!event.getState().equals(EventState.PUBLISHED)) {
            throw new ConflictException("Мероприятие должно быть опубликовано");
        }
        Optional<ParticipationRequestDto> existingRequest = requestFeignClient
                .findByRequesterIdAndEventIdAndStatus(authorId, eventId, RequestStatus.CONFIRMED);

        if (existingRequest.isEmpty()) {
            throw new ConflictException("Комментарии может оставлять только подтвержденный участник мероприятия");
        }
        Comment comment = CommentMapper.toComment(newCommentDto, author.getId(), event.getId());
        commentRepository.save(comment);
        return CommentMapper.toDto(comment);
    }

    @Transactional
    @Override
    public CommentDto updateComment(long authorId, long commentId, NewCommentDto updateCommentDto) {
        Comment commentToUpdate = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException(String.format("Комментарий ID %s не найден", commentId)));
        if (authorId != commentToUpdate.getAuthor()) {
            throw new ConflictException("Изменить комментарий может только его автор");
        }
        commentToUpdate.setText(updateCommentDto.getText());
        commentToUpdate.setStatus(CommentStatus.PENDING);

        commentRepository.save(commentToUpdate);
        return CommentMapper.toDto(commentToUpdate);
    }

    @Transactional
    @Override
    public void deleteComment(long authorId, long commentId) {
        Comment commentToDelete = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException(String.format("Комментарий ID %s не найден", commentId)));
        if (authorId != commentToDelete.getAuthor()) {
            throw new ConflictException("Удалить комментарий может только его автор");
        }
        commentRepository.delete(commentToDelete);
    }

    @Transactional
    @Override
    public void adminDeleteComment(long commentId) {
        Comment commentToDelete = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException(String.format("Комментарий ID %s не найден", commentId)));
        commentRepository.delete(commentToDelete);
    }

    @Transactional
    @Override
    public CommentDto adminUpdateCommentStatus(Long commentId, AdminUpdateCommentStatusDto updateCommentStatusDto) {
        Comment commentToUpdateStatus = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException(String.format("Комментарий ID %s не найден", commentId)));
        if (!commentToUpdateStatus.getStatus().equals(CommentStatus.PENDING)) {
            throw new ConflictException("Невозможно отклонить комментарий");
        }
        if (updateCommentStatusDto.getAction().equals(AdminUpdateCommentStatusAction.PUBLISH_COMMENT)) {
            commentToUpdateStatus.setStatus(CommentStatus.PUBLISHED);
        }
        if (updateCommentStatusDto.getAction().equals(AdminUpdateCommentStatusAction.REJECT_COMMENT)) {
            commentToUpdateStatus.setStatus(CommentStatus.REJECTED);
        }
        commentRepository.save(commentToUpdateStatus);
        return CommentMapper.toDto(commentToUpdateStatus);
    }

    @Override
    public List<CommentDto> adminPendigCommentList(List<Long> usersId) {
        if (usersId != null && !usersId.isEmpty()) {
            return commentRepository.findByAuthor_IdInAndStatus(usersId, CommentStatus.PENDING)
                    .stream()
                    .map(CommentMapper::toDto)
                    .toList();
        }
        return commentRepository.findAllByStatus(CommentStatus.PENDING)
                .stream()
                .map(CommentMapper::toDto)
                .toList();
    }

    @Override
    public List<CommentDto> getCommentsByEventsIds(Set<Long> eventsId) {
        return commentRepository.findLastCommentsForManyEvents(eventsId).stream()
                .map(CommentMapper::toDto)
                .toList();
    }

    @Override
    public List<CommentDto> getEventComments(Long eventId, Integer from, Integer size) {
        Sort sort = Sort.by("created").descending();
        Pageable pageable = PageRequest.of(from, size, sort);
        List<Comment> comments = commentRepository.findByEventId(eventId, pageable);
        return comments.stream().map(CommentMapper::toDto).toList();
    }
}
