package ru.practicum.compilation.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import ru.practicum.dto.compilation.CompilationDto;
import ru.practicum.dto.compilation.NewCompilationDto;
import ru.practicum.compilation.model.Compilation;
import ru.practicum.dto.compilation.UpdateCompilationRequest;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface CompilationMapper {

    @Mapping(source = "events", target = "events", ignore = true)
    Compilation toCompilation(NewCompilationDto newCompilationDto);

    @Mapping(source = "events", target = "events", ignore = true)
    Compilation toCompilation(UpdateCompilationRequest updateCompilationRequest);

    CompilationDto toCompilationDto(Compilation compilation);
}