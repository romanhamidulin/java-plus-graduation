package ru.practicum.dto.compilation;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import ru.practicum.dto.events.EventShortDto;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
@ToString
public class CompilationDto {
    @Min(1)
    @NotNull
    private Long id;
    private List<EventShortDto> events;
    private Boolean pinned = false;
    private String title;
}
