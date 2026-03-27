package ru.practicum.dto.events;

import lombok.*;
import ru.practicum.enums.event.EventSort;

import java.time.LocalDateTime;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
@ToString
public class EntityParam {
    private String text;
    private EventSort sort;
    private Integer from;
    private Integer size;
    private List<Long> categories;
    private LocalDateTime rangeStart;
    private LocalDateTime rangeEnd;
    private Boolean paid;
    private Boolean onlyAvailable;

    public boolean hasText() {
        return text != null;
    }

    public boolean hasCategories() {
        return categories != null;
    }

    public boolean hasPaid() {
        return paid != null;
    }

    public boolean hasRangeStart() {
        return rangeStart != null;
    }

    public boolean hasRangeEnd() {
        return rangeEnd != null;
    }
}
