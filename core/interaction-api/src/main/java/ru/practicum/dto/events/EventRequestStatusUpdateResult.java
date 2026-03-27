package ru.practicum.dto.events;

import lombok.*;
import ru.practicum.dto.request.ParticipationRequestDto;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
@ToString
public class EventRequestStatusUpdateResult {
    private List<ParticipationRequestDto> confirmedRequests = new ArrayList<>();

    private List<ParticipationRequestDto> rejectedRequests = new ArrayList<>();
}
