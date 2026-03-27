package ru.practicum.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class ConfirmedRequests {
    private long count;
    private Long event;
}
