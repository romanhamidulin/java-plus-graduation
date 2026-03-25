package ru.practicum.dto.request;

import lombok.*;
import ru.practicum.enums.request.RequestStatus;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
@ToString
public class EventRequestStatusUpdateRequest {
    private List<Long> requestIds;

    private RequestStatus status;

    public enum Status {
        CONFIRMED, REJECTED
    }
}
