package ru.practicum.dto.events;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;
import ru.practicum.enums.event.EventState;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
@ToString
public class EventCreateDto {
    public static final String DATE_PATTERN = "yyyy-MM-dd HH:mm:ss";
    @NotBlank
    @Size(min = 3, max = 120)
    private String title;
    @NotBlank
    @Size(min = 20, max = 2000)
    private String annotation;
    @NotBlank
    @Size(min = 20, max = 7000)
    private String description;
    @NotNull
    private Long category;
    @NotNull
    @DateTimeFormat(pattern = DATE_PATTERN)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime eventDate;
    @NotNull
    private LocationDto location;

    private Boolean paid;
    @PositiveOrZero
    private Integer participantLimit = 0;
    private Boolean requestModeration = true;

    @JsonIgnore
    EventState state = EventState.PENDING;
}
