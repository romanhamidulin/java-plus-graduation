package ru.practicum.compilation.mapper;

import org.mapstruct.*;
import ru.practicum.dto.compilation.CompilationDto;
import ru.practicum.dto.compilation.NewCompilationDto;
import ru.practicum.compilation.model.Compilation;

@Mapper(componentModel = "spring")
public interface CompilationMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "events", ignore = true)
    Compilation toEntity(NewCompilationDto newCompilationDto);

    CompilationDto toDto(Compilation compilation);

}