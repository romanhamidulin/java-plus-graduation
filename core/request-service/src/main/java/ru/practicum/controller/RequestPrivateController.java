package ru.practicum.controller;

import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.request.ParticipationRequestDto;
import ru.practicum.service.RequestService;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/users/{userId}/requests")
public class RequestPrivateController {
    private final RequestService requestService;

    @GetMapping
    public List<ParticipationRequestDto> getRequestsByRequester(@Min(1) @PathVariable Long userId) {
        return requestService.getRequestsByRequester(userId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ParticipationRequestDto addRequest(@Min(1) @PathVariable Long userId,
                                              @Min(1) @RequestParam Long eventId) {
        ParticipationRequestDto participationRequest = requestService.addRequest(userId, eventId);
        log.info("Успешно создана заявка пользователем = {} на участие в мероприятити = {}", participationRequest.getRequester(), participationRequest.getId());
        return participationRequest;
    }

    @PatchMapping("/{requestId}/cancel")
    public ParticipationRequestDto updateRequest(@Min(1) @PathVariable Long userId,
                                                 @Min(1) @PathVariable Long requestId) {
        return requestService.cancelRequest(userId, requestId);
    }
}
