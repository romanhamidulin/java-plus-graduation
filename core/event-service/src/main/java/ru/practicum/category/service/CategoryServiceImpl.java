package ru.practicum.category.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.category.CategoryDto;
import ru.practicum.dto.category.NewCategoryDto;
import ru.practicum.category.mapper.CategoryMapper;
import ru.practicum.category.model.Category;
import ru.practicum.category.repository.CategoryRepository;
import ru.practicum.events.repository.EventRepository;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository repository;
    private final EventRepository eventRepository;
    private final CategoryMapper categoryMapper;

    @Override
    public List<CategoryDto> findAll(Integer from, Integer size) {
        Pageable pageable = PageRequest.of(from / size, size);
        return categoryMapper.toCategoryDtoList(repository.findAll(pageable).toList());
    }

    @Override
    public CategoryDto findById(Long catId) {
        Category category = checkCategory(catId);
        return categoryMapper.toCategoryDto(category);
    }

    @Override
    @Transactional
    public CategoryDto createById(NewCategoryDto dto) {
        repository.findByNameContainsIgnoreCase(dto.getName()).ifPresent(category -> {
            throw new ConflictException("Категория с таким именем уже существует");
        });
        Category category = categoryMapper.toCategory(dto);
        return categoryMapper.toCategoryDto(repository.save(category));
    }

    @Override
    @Transactional
    public CategoryDto updateById(Long catId, CategoryDto dto) {
        Category category = repository.findById(catId)
                .orElseThrow(() -> new NotFoundException("Категория с id = " + catId + " не найдена"));

        if (dto.getName() != null && !dto.getName().isBlank()) {
            String newName = dto.getName().trim();

            if (category.getName().equals(newName)) {
                return categoryMapper.toCategoryDto(category);
            }

            if (repository.existsByNameAndIdNot(newName, catId)) {
                throw new ConflictException("Категория с именем " + newName + " уже существует");
            }

            category.setName(newName);
        }

        Category updatedCategory = repository.save(category);
        return categoryMapper.toCategoryDto(updatedCategory);
    }

    @Override
    @Transactional
    public void deleteById(Long catId) {
        if (eventRepository.existsByCategory_Id(catId)) {
            throw new ConflictException("Категория относиться к событию");
        }
        repository.deleteById(catId);
    }

    private Category checkCategory(Long catId) {
        return repository.findById(catId).orElseThrow(() -> new NotFoundException("Категория с id=" + catId + " не найдена"));
    }
}