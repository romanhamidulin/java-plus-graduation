package ru.practicum.compilation.controller;

import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.compilation.model.GetCompilationsParam;
import ru.practicum.dto.compilation.CompilationDto;
import ru.practicum.compilation.service.CompilationService;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/compilations")
@Validated
public class CompilationPublicController {

    private final CompilationService compilationService;

    @GetMapping
    public List<CompilationDto> getCompilations(@RequestParam(defaultValue = "false") Boolean pinned,
                                                @Min(0) @RequestParam(defaultValue = "0") Integer from,
                                                @Min(1) @RequestParam(defaultValue = "10") Integer size) {
        log.info("GET /compilations - получение подборки");
        Pageable page = PageRequest.of(from, size);
        GetCompilationsParam param = GetCompilationsParam.builder()
                .pinned(pinned)
                .pageable(page)
                .build();

        List<CompilationDto> compilations = compilationService.getCompilations(param);
        return compilations;
    }

    @GetMapping("/{compId}")
    public CompilationDto getCompilationById(@Min(1) @PathVariable Long compId) {
        return compilationService.getCompilationById(compId);
    }
}
